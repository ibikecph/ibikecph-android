package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.location.DetectedActivity;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.LOG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

        // If the track is too short, just disregard it. We have nothing more to do, so just return.
        if (curLocationList.size() < 3) {
            Log.d("MF", "track too short");
            Log.d("MF", "##############################################");
            realm.cancelTransaction();
            return;
        }

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
        uploadTracksToServer();
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

                try {
                    // If ID values are >0, an ID from the server has already been set, meaning that the track has already been uploaded.
                    tracksToUpload = realm.where(Track.class).equalTo("ID", 0).findAll();
                    Log.d("DV", "tracksToUploadSize = " + tracksToUpload.size());
                } catch (Exception e) {
                    Log.d("DV", "uploadTracksToServer-exception: " + e.getMessage());
                }

                if (tracksToUpload.size() > 0) {

                    JSONObject postObject = null;
                    if (IbikeApplication.isUserLogedIn()) {
                        String authToken = IbikeApplication.getAuthToken();
                        try {
                            // Loop and pack JSON for each track we want to upload!
                            for (int i = 0; i < tracksToUpload.size(); i++) {
                                postObject = new JSONObject();
                                JSONObject trackData = new JSONObject();

                                JSONArray jsonArray = new JSONArray();

                                Date start = tracksToUpload.get(i).getLocations().first().getTimestamp();

                                trackData.put("timestamp", start.getTime() / 1000); //Seconds
                                trackData.put("from_name", tracksToUpload.get(i).getStart());
                                trackData.put("to_name", tracksToUpload.get(i).getEnd());
                                Log.d("DV", "timestamp = " + start.toString() + " / seconds : " + start.getTime() / 1000);
                                Log.d("DV", "from_name = " + tracksToUpload.get(i).getStart());
                                Log.d("DV", "to_name = " + tracksToUpload.get(i).getEnd());

                                final RealmList<TrackLocation> tl = tracksToUpload.get(i).getLocations();
                                for (int j = 0; j < tl.size(); j++) {
                                    JSONObject locationsObject = new JSONObject();
                                    locationsObject.put("seconds_passed", ((tl.get(j).getTimestamp().getTime() / 1000) - (start.getTime() / 1000))); //Seconds
                                    locationsObject.put("latitude", tl.get(j).getLatitude());
                                    locationsObject.put("longitude", tl.get(j).getLongitude());
                                    jsonArray.put(locationsObject);
                                    Log.d("DV", "seconds_past = " + tl.get(j).getTimestamp() + " / seconds : " + ((tl.get(j).getTimestamp().getTime() / 1000) - (start.getTime() / 1000)) + " (lat,lon): " + tl.get(j).getLatitude() + " , " + tl.get(j).getLongitude());
                                }

                                trackData.put("coordinates_attributes", jsonArray);
                                postObject.put("auth_token", authToken);
                                postObject.put("track", trackData);
                                Log.d("DV", "postObject = " + postObject.toString());

                                Log.d("DV", "Server request: " + Config.API_UPLOAD_TRACKS);
                                JsonNode responseNode = HttpUtils.postToServer(Config.API_UPLOAD_TRACKS, postObject);
                                if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
                                    int id = responseNode.get("data").get("id").asInt();
                                    Log.d("DV", "ID modtaget = " + id);
                                    Log.d("DV", "Count = " + responseNode.get("data").get("count").asInt());
                                    // Set the new ID received from the server
                                    Log.d("DV", "Id before set = " + tracksToUpload.get(i).getID());
                                    tracksToUpload.get(i).setID(id);
                                    Log.d("DV", "Id after set = " + tracksToUpload.get(i).getID());
                                }
                            }
                            Log.d("DV", "Saving changes to DB!");
                            realm.commitTransaction();
                            realm.close();
                        } catch (JSONException e) {
                            LOG.e(e.getLocalizedMessage());
                        }
                    }
                }
            }
        }).start();
    }

    public static void deleteTrack(final int id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (IbikeApplication.isUserLogedIn()) {
                        String authToken = IbikeApplication.getAuthToken();
                        JSONObject postObject = new JSONObject();
                        postObject.put("auth_token", authToken);
                        Log.d("DV", "Track ID to delete = " + id);
                        HttpUtils.deleteFromServer(Config.API_UPLOAD_TRACKS + "/" + id, postObject);
                    }
                } catch (JSONException e) {
                    LOG.e(e.getLocalizedMessage());
                }
            }
        }).start();
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
        track.setTimestamp(stamp);
        track.setDuration(5234 / 1000);
        track.setLength(250);
        track.setID(0);

        realm.commitTransaction();
        uploadeFakeTrack();
    }

    //Test method
    public static void uploadeFakeTrack() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Realm realm = Realm.getInstance(IbikeApplication.getContext());
                realm.beginTransaction();
                RealmResults<Track> tracksToUpload = null;

                try {
                    // If ID values are >0, an ID from the server has already been set, meaning that the track has already been uploaded.
                    tracksToUpload = realm.where(Track.class).equalTo("ID", 0).findAll();
                    Log.d("DV", "tracksToUploadSize = " + tracksToUpload.size());
                } catch (Exception e) {
                    Log.d("DV", "uploadTracksToServer-exception: " + e.getMessage());
                }

                if (tracksToUpload.size() > 0) {

                    JSONObject postObject = null;
                    if (IbikeApplication.isUserLogedIn()) {
                        String authToken = IbikeApplication.getAuthToken();
                        try {
                            // Loop and pack JSON for each track we want to upload!
                            for (int i = 0; i < tracksToUpload.size(); i++) {
                                postObject = new JSONObject();
                                JSONObject trackData = new JSONObject();
                                JSONArray jsonArray = new JSONArray();

                                trackData.put("timestamp", "133713371"); //Seconds
                                trackData.put("from_name", "Borgergade 24");
                                trackData.put("to_name", "Vestergade 20C");

                                for (int j = 0; j < 3; j++) {
                                    JSONObject locationsObject = new JSONObject();
                                    locationsObject.put("seconds_passed", 20); //Seconds
                                    locationsObject.put("latitude", 55.1337);
                                    locationsObject.put("longitude", 12.1337);
                                    jsonArray.put(locationsObject);
                                }

                                trackData.put("coordinates_attributes", jsonArray);
                                postObject.put("auth_token", authToken);
                                postObject.put("track", trackData);
                                Log.d("DV", "postObject = " + postObject.toString());

                                Log.d("DV", "Server request: " + Config.API_UPLOAD_TRACKS);
                                JsonNode responseNode = HttpUtils.postToServer(Config.API_UPLOAD_TRACKS, postObject);
                                if (responseNode != null && responseNode.has("data") && responseNode.get("data").has("id")) {
                                    int id = responseNode.get("data").get("id").asInt();
                                    Log.d("DV", "ID modtaget = " + id);
                                    Log.d("DV", "Count = " + responseNode.get("data").get("count").asInt());
                                    // Set the new ID received from the server
                                    Log.d("DV", "Id before set = " + tracksToUpload.get(i).getID());
                                    tracksToUpload.get(i).setID(id);
                                    //Log.d("DV", "Id after set = " + tracksToUpload.get(i).getID());
                                }
                            }
                            Log.d("DV", "Saving changes to DB!");
                            realm.commitTransaction();
                            realm.close();
                        } catch (JSONException e) {
                            LOG.e(e.getLocalizedMessage());
                        }
                    }
                }
            }
        }).start();
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
