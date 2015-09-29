package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.location.DetectedActivity;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.channels.spi.AbstractSelectionKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by jens on 2/25/15.
 */
public class TrackingManager implements LocationListener {
    private static final boolean DEBUG = false;
    private static final int MAX_INACCURACY = 20;
    private static final int TRACK_PAUSE_THRESHOLD = 120000; // 2 minutes in milliseconds

    private static TrackingManager instance = null;
    private boolean isTracking = false;

    private List<Location> curLocationList;
    private Realm realm;
    private static int attemptsToSend = 0;
    public static int statusCode = 0;

    // Sometimes we want to start tracking, overriding the ActivityRecognition
    private boolean manualOverride = false;

    public TrackingManager() {
        Log.d("JC", "TrackingManager instantiated");
    }

    public static TrackingManager getInstance() {
        if (TrackingManager.instance == null)
            TrackingManager.instance = new TrackingManager();

        return instance;
    }

    public void startTracking(boolean override) {
        startTracking();
        this.manualOverride = override;
    }

    /**
     * Resets the list of location points and registers itself to receive GPS updates.
     */
    public void startTracking() {
        if (!this.isTracking) {
            Log.d("JC", "TrackingManager: Starting to track");
            BikeLocationService.getInstance().addGPSListener(this);
            this.curLocationList = new ArrayList<Location>();
            this.isTracking = true;
        }
    }

    /**
     * Deregisters for GPS updates and calls `makeAndSaveTrack` to create the track in the DB.
     *
     * @param override
     */
    public void stopTracking(boolean override) {
        // We stop the tracking, either if we've not manually overridden, or if we
        // locally overrode the override. This nomenclature sucks.
        if (this.isTracking && (!manualOverride || override)) {
            Log.d("JC", "TrackingManager: Stopping track");
            BikeLocationService.getInstance().removeGPSListener(this);
            this.isTracking = false;

            makeAndSaveTrack();

            // If we just stopped, manualOverride should be false, regardless whether
            // we came from an overridden state or not.
            this.manualOverride = false;

            if (IbikeApplication.getSettings().getNotifyMilestone()) {
                Log.d("JC", "Sending notification!");
                // Check if we should notify the user of milestones reached
                MilestoneManager.checkForMilestones();
            }
        }
    }

    /**
     * Creates a track from the currently saved locations and saves it to the database.
     */
    private void makeAndSaveTrack() {
        // Save the track to the DB
        realm = Realm.getInstance(IbikeApplication.getContext());
        realm.beginTransaction();

        Log.d("MF", "############## makeAndSaveTrack ##############");
        Log.d("MF", "threshold: " + TRACK_PAUSE_THRESHOLD);

        /*
        // If the track is too short, just disregard it. We have nothing more to do, so just return.
        if (curLocationList.size() < 3) {
            Log.d("MF", "track too short");
            Log.d("MF", "##############################################");
            realm.cancelTransaction();
            return;
        }
        */

        Track track;
        // last track
        try {
            Track lastTrack = realm.where(Track.class)
                    .findAllSorted("timestamp", RealmResults.SORT_ORDER_DESCENDING)
                    .first();

            Log.d("MF", "last track time: " + lastTrack.getLocations().last().getTimestamp().getTime());

            // use previous track if still fresh, or create new

            long lastTrackDiff = curLocationList.get(0).getTime() - lastTrack.getLocations().last().getTimestamp().getTime();

            Log.d("MF", "time diff: " + lastTrackDiff);

            if (lastTrackDiff < TRACK_PAUSE_THRESHOLD) {
                Log.d("MF", "using last!");
                track = lastTrack;
            } else {
                Log.d("MF", "creating new");
                uploadTracksToServer();
                track = realm.createObject(Track.class);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // There were no tracks in the first place!
            track = realm.createObject(Track.class);
        }

        // Set a timestamp for the Track.
        Date stamp = new Date();
        Log.d("MF", "new time: " + stamp.getTime());
        track.setTimestamp(stamp);

        RealmList<TrackLocation> trackLocations = track.getLocations();

        // We have a list of Location objects that represent our route. Convert these to TrackLocation objects
        // and add them to the track we're working on.

        Location lastLocation = null;
        double dist = track.getLength();

        for (Location l : curLocationList) {
            TrackLocation trackLocation = realm.createObject(TrackLocation.class);

            // Set all the relevant fields
            trackLocation.setLatitude(l.getLatitude());
            trackLocation.setLongitude(l.getLongitude());
            trackLocation.setTimestamp(new Date(l.getTime()));
            trackLocation.setAltitude(l.getAltitude());

            // This is potentially bad. We don't have a measure of the horizontal and vertical accuracies, but we do have
            // one for the accuracy all in all. We just set that for both fields.
            trackLocation.setHorizontalAccuracy(l.getAccuracy());
            trackLocation.setVerticalAccuracy(l.getAccuracy());

            // Add it to the track
            trackLocations.add(trackLocation);

            // Update the distance counter
            if (lastLocation != null) {
                dist += lastLocation.distanceTo(l);
            }
            lastLocation = l;
        }

        // If the track is too short, just disregard it. We have nothing more to do, so just return.
        //Possible logic to check if a track is < 50 meters
        //float distance = curLocationList.get(0).distanceTo(curLocationList.get(curLocationList.size() - 1));
        if (dist < 50) {
            Log.d("DV", "track too short");
            Log.d("DV", "##############################################");
            realm.cancelTransaction();
            return;
        }

        // Set the duration. We say it's the duration of time from the first to the last timestamp. We're dividing by
        // 1000 because the timestamps are in milliseconds, and we want seconds.
        track.setDuration((trackLocations.last().getTimestamp().getTime() - trackLocations.first().getTimestamp().getTime()) / 1000);

        // Set the distance
        track.setLength(dist);

        // Set ID = 0 meaning that the track hasn't been uploaded yet
        track.setID(0);

        Log.d("MF", "last time: " + trackLocations.last().getTimestamp().getTime());
        Log.d("MF", "distance: " + dist);

        Log.d("MF", "##############################################");

        // We're done so far.
        realm.commitTransaction();

        // Geocode the track. The TrackHelper will open a new Realm transaction.
        TrackHelper helper = new TrackHelper(track);
        helper.geocodeTrack();
    }

    /**
     * Called when makeAndSaveTrack() is called to upload tracks that haven't been uploaded yet.
     */
    public static void uploadTracksToServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Realm realm = Realm.getInstance(IbikeApplication.getContext());
                realm.beginTransaction();
                RealmResults<Track> tracksToUpload = null;

                Date stamp = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(stamp);
                cal.add(Calendar.MINUTE, -15);
                Date newDate = cal.getTime();

                try {
                    /*
                    If ID values are >0, an ID from the server has already been set, meaning that the track has already been uploaded.
                    Also checks if the track is > 15 minutes old - used to avoid uploading a track in progress in the makeAndSaveTrack-method.
                     */
                    tracksToUpload = realm.where(Track.class).equalTo("ID", 0).lessThanOrEqualTo("timestamp", newDate).findAll();
                    Log.d("DV", "tracksToUploadSize = " + tracksToUpload.size());
                } catch (Exception e) {
                    Log.d("DV", "uploadTracksToServer-exception: " + e.getMessage());
                }

                if (tracksToUpload.size() > 0) {

                    JSONObject postObject = null;
                    String signature = IbikeApplication.getSignature();
                    if (IbikeApplication.isUserLogedIn() && !signature.equals("")) {
                        String authToken = IbikeApplication.getAuthToken();
                        try {
                            // Loop and pack JSON for each track we want to upload!
                            for (int i = 0; i < tracksToUpload.size(); i++) {
                                postObject = new JSONObject();
                                JSONObject trackData = new JSONObject();
                                JSONArray jsonArray = new JSONArray();
                                int amountToSend = 0;
                                int count = 0;
                                Log.d("DV", "Track ID to be uploaded = " + tracksToUpload.get(i).getID());

                                Date start = tracksToUpload.get(i).getLocations().first().getTimestamp();

                                trackData.put("timestamp", start.getTime() / 1000); //Seconds
                                trackData.put("from_name", tracksToUpload.get(i).getStart());
                                trackData.put("to_name", tracksToUpload.get(i).getEnd());
                                trackData.put("coord_count", amountToSend);
                                Log.d("DV", "timestamp = " + start.toString() + " / seconds : " + start.getTime() / 1000);
                                Log.d("DV", "from_name = " + tracksToUpload.get(i).getStart());
                                Log.d("DV", "to_name = " + tracksToUpload.get(i).getEnd());
                                Log.d("DV", "count(amountToSend) = " + amountToSend);

                                final RealmList<TrackLocation> tl = tracksToUpload.get(i).getLocations();
                                for (int j = 0; j < tl.size(); j++) {
                                    JSONObject locationsObject = new JSONObject();
                                    locationsObject.put("seconds_passed", ((tl.get(j).getTimestamp().getTime() / 1000) - (start.getTime() / 1000))); //Seconds
                                    locationsObject.put("latitude", tl.get(j).getLatitude());
                                    locationsObject.put("longitude", tl.get(j).getLongitude());
                                    jsonArray.put(locationsObject);
                                    amountToSend++;
                                    Log.d("DV", "seconds_past = " + tl.get(j).getTimestamp() + " / seconds : " + ((tl.get(j).getTimestamp().getTime() / 1000) - (start.getTime() / 1000)) + " (lat,lon): " + tl.get(j).getLatitude() + " , " + tl.get(j).getLongitude());
                                }

                                trackData.put("coordinates", jsonArray);
                                trackData.put("signature", signature);
                                postObject.put("auth_token", authToken);
                                postObject.put("track", trackData);
                                Log.d("DV", "postObject = " + postObject.toString());
                                Log.d("DV", "Amount of sent coordinates = " + amountToSend);

                                Log.d("DV", "Server request: " + Config.API_UPLOAD_TRACKS);
                                JsonNode responseNode = HttpUtils.postToServer(Config.API_UPLOAD_TRACKS, postObject);
                                if (responseNode != null && responseNode.has("invalid_token")) {
                                    if (responseNode.get("invalid_token").asBoolean()) {
                                        Log.d("DV", "invalid token - logout the user!");
                                        IbikeApplication.logout();
                                    }
                                } else {
                                    if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
                                        int id = responseNode.get("data").get("id").asInt();
                                        Log.d("DV", "ID modtaget = " + id);
                                        count = responseNode.get("data").get("count").asInt();
                                        Log.d("DV", "Count = " + count);
                                        Log.d("DV", "Id before set = " + tracksToUpload.get(i).getID());
                                        // Set the new ID received from the server if the server received all data
                                        if (count == amountToSend) {
                                            Log.d("DV", "Id before set = " + tracksToUpload.get(i).getID());
                                            tracksToUpload.get(i).setID(id);
                                            //Log.d("DV", "Id after set = " + tracksToUpload.get(i).getID());
                                            attemptsToSend = 0;
                                            i--;
                                        }
                                    } else {
                                        // Try to resend maximum 3 times.
                                        attemptsToSend++;
                                        if (attemptsToSend < 4) {
                                            Log.d("DV", "Resending track.. attempt " + attemptsToSend);
                                            i--;
                                            //uploadeFakeTrack();
                                        } else {
                                            attemptsToSend = 0;
                                            //Delete track?
                                            //tracksToUpload.get(i).removeFromRealm();
                                            //realm.commitTransaction();
                                            //realm.close();
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            LOG.e(e.getLocalizedMessage());
                        }
                    }
                }
                Log.d("DV", "Saving changes to DB!");
                realm.commitTransaction();
                realm.close();
            }
        }).start();
    }

    public static void deleteTrack(final int id) {

        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        realm.beginTransaction();
        RealmResults<Track> trackToDelete = null;

        try {
            trackToDelete = realm.where(Track.class).equalTo("ID", id).findAll();
        } catch (Exception e) {
            Log.d("DV", "deleteTrack-exception: " + e.getMessage());
        }

        try {
            if (IbikeApplication.isUserLogedIn()) {
                String authToken = IbikeApplication.getAuthToken();
                String signature = IbikeApplication.getSignature();
                JSONObject postObject = new JSONObject();
                postObject.put("auth_token", authToken);
                postObject.put("signature", signature);
                Log.d("DV", "Track ID to delete = " + id);
                JsonNode responseNode = HttpUtils.deleteFromServer(Config.API_UPLOAD_TRACKS + "/" + id, postObject);
                if (responseNode != null && responseNode.has("invalid_token")) {
                    if (responseNode.get("invalid_token").asBoolean()) {
                        Log.d("DV", "invalid token - logout the user!");
                        IbikeApplication.logout();
                    }
                } else {
                    if (responseNode != null) {
                        Log.d("DV", "responseNode = " + responseNode.toString());
                        if (responseNode.get("success").asBoolean() || statusCode == 404) {
                            Log.d("DV", "Track deleted from the server!");
                            trackToDelete.get(0).removeFromRealm();
                            realm.commitTransaction();
                            realm.close();
                            Log.d("DV", "Track deleted from the APP!");
                        } else if (responseNode.has("invalid_token")) {
                            if (responseNode.get("invalid_token").asBoolean()) {
                                Log.d("DV", "invalid token - logout the user!");
                                IbikeApplication.logout();
                            }
                        } else {
                            //??
                        }
                    }
                }
            }
        } catch (JSONException e) {
            LOG.e(e.getLocalizedMessage());
        }

    }

    //Test method
    public static void createFakeTrack() {
        Log.d("DV", "Creating fake track!");
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        realm.beginTransaction();

        Track track;
        track = realm.createObject(Track.class);

        Date stamp = new Date();
        Log.d("DV", "new time: " + stamp.getTime());
        Log.d("DV", "new time: " + stamp);

        Calendar cal = Calendar.getInstance();
        cal.setTime(stamp);
        cal.add(Calendar.MINUTE, -14);
        Date newDate = cal.getTime();
        Log.d("DV", "new time = " + newDate);

        track.setTimestamp(newDate);
        track.setDuration(5234 / 1000);
        double length = Math.random() * 1000;
        track.setLength(length);
        track.setID(0);

        realm.commitTransaction();
        realm.close();
    }


    //Test method
    public static void uploadeFakeTrack() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Realm realm = Realm.getInstance(IbikeApplication.getContext());
                realm.beginTransaction();
                RealmResults<Track> tracksToUpload = null;

                Date stamp = new Date();
                Calendar cal = Calendar.getInstance();
                cal.setTime(stamp);
                cal.add(Calendar.MINUTE, -15);
                Date newDate = cal.getTime();

                try {
                     /*
                    If ID values are >0, an ID from the server has already been set, meaning that the track has already been uploaded.
                    Also checks if the track is > 15 minutes old - used to avoid uploading a track in progress in the makeAndSaveTrack-method.
                     */
                    //tracksToUpload = realm.where(Track.class).equalTo("ID", 0).lessThanOrEqualTo("timestamp", newDate).findAll();
                    tracksToUpload = realm.where(Track.class).equalTo("ID", 0).findAll();//lessThanOrEqualTo("timestamp", newDate).findAll();
                    Log.d("DV", "tracksToUploadSize = " + tracksToUpload.size());
                } catch (Exception e) {
                    Log.d("DV", "uploadTracksToServer-exception: " + e.getMessage());
                }

                if (tracksToUpload.size() > 0) {

                    JSONObject postObject = null;
                    String signature = IbikeApplication.getSignature();
                    if (IbikeApplication.isUserLogedIn() && !signature.equals("")) {
                        String authToken = IbikeApplication.getAuthToken();
                        try {

                            // Loop and pack JSON for each track we want to upload!
                            for (int i = 0; i < tracksToUpload.size(); i++) {
                                postObject = new JSONObject();
                                JSONObject trackData = new JSONObject();
                                JSONArray jsonArray = new JSONArray();
                                int amountToSend = 0;
                                int count = 0;
                                Log.d("DV", "Track ID to be uploaded = " + tracksToUpload.get(i).getID());

                                trackData.put("coord_count", 3);
                                trackData.put("timestamp", "133713371"); //Seconds
                                trackData.put("from_name", "Borgergade 24");
                                trackData.put("to_name", "Vestergade 20C");

                                for (int j = 0; j < 3; j++) {
                                    JSONObject locationsObject = new JSONObject();
                                    locationsObject.put("seconds_passed", 20); //Seconds
                                    locationsObject.put("latitude", 55.1337);
                                    locationsObject.put("longitude", 12.1337);
                                    amountToSend++;
                                    jsonArray.put(locationsObject);
                                }

                                trackData.put("coordinates", jsonArray);
                                trackData.put("signature", signature);
                                postObject.put("auth_token", authToken);
                                postObject.put("track", trackData);
                                Log.d("DV", "postObject = " + postObject.toString());

                                Log.d("DV", "Server request: " + Config.API_UPLOAD_TRACKS);
                                JsonNode responseNode = HttpUtils.postToServer(Config.API_UPLOAD_TRACKS, postObject);
                                if (responseNode != null && responseNode.has("invalid_token")) {
                                    if (responseNode.get("invalid_token").asBoolean()) {
                                        Log.d("DV", "invalid token - logout the user!");
                                        IbikeApplication.logout();
                                    }
                                } else {
                                    if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
                                        int id = responseNode.get("data").get("id").asInt();
                                        Log.d("DV", "ID modtaget = " + id);
                                        count = responseNode.get("data").get("count").asInt();
                                        Log.d("DV", "Count = " + count);
                                        // Set the new ID received from the server if the server received all data
                                        if (count == amountToSend) {
                                            Log.d("DV", "Id before set = " + tracksToUpload.get(i).getID());
                                            tracksToUpload.get(i).setID(id);
                                            //Log.d("DV", "Id after set = " + tracksToUpload.get(i).getID());
                                            attemptsToSend = 0;
                                            i--;
                                        }
                                    } else {
                                        // Try to resend maximum 3 times.
                                        attemptsToSend++;
                                        if (attemptsToSend < 4) {
                                            Log.d("DV", "Resending track.. attempt " + attemptsToSend);
                                            i--;
                                            //uploadeFakeTrack();
                                        } else {
                                            attemptsToSend = 0;
                                            //Delete track?
                                            //tracksToUpload.get(i).removeFromRealm();
                                            //realm.commitTransaction();
                                            //realm.close();
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            LOG.e(e.getLocalizedMessage());
                        }
                    }
                }
                Log.d("DV", "Saving changes to DB!");
                realm.commitTransaction();
                realm.close();
            }
        }).start();
    }

    public static void printAllTracks() {
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        realm.beginTransaction();
        RealmResults<Track> tracksToUpload = null;
        try {
            // If ID values are >0, an ID from the server has already been set, meaning that the track has already been uploaded.
            tracksToUpload = realm.where(Track.class).greaterThanOrEqualTo("ID", 1).findAll();
        } catch (Exception ex) {

            for (int i = 0; i < tracksToUpload.size(); i++) {
                Log.d("DV", "Track ID = " + tracksToUpload.get(i).getID());
            }
        }
    }

    public void stopTracking() {
        stopTracking(false);
    }


    public boolean isTracking() {
        return this.isTracking;
    }

    /***
     * Called when the GPS service has a new location ready. Adds the given location to the current track if we're
     * tracking locations.
     *
     * @param givenLocation
     */
    @Override
    public void onLocationChanged(Location givenLocation) {
        // TODO: The `realm` field would be nice to have on the class instead of potentially constructing it on each GPS update
        realm = Realm.getInstance(IbikeApplication.getContext());

        if (isTracking && givenLocation.getAccuracy() <= MAX_INACCURACY) {
            Log.d("JC", "Got new GPS coord");
            curLocationList.add(givenLocation);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        this.stopTracking();
    }

    public void onActivityChanged(int activityType, int confidence) {
        //Log.d("JC", "TrackingManager new activity");
        if (
                IbikeApplication.getSettings().getTrackingEnabled() &&
                        ((!this.isTracking && activityType == DetectedActivity.ON_BICYCLE) ||
                                (!this.isTracking() && DEBUG && activityType == DetectedActivity.TILTING))
                ) {
            Log.i("JC", "Activity changed to bicycle, starting track.");
            startTracking();
        } else if (activityType != DetectedActivity.ON_BICYCLE && activityType != DetectedActivity.UNKNOWN && this.isTracking) {
            Log.i("JC", "Activity changed away from bicycle, stopping track.");
            stopTracking();
        }

    }

}
