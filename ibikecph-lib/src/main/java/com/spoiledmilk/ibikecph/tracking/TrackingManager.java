package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.location.DetectedActivity;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.map.SMHttpRequestListener;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jens on 2/25/15.
 */
public class TrackingManager implements LocationListener, SMHttpRequestListener {
    private static final boolean DEBUG = false;
    private static final int MAX_INACCURACY = 25;

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

        // TODO: Determine if we'd rather add the locations to the former track.

        Track track;

        if (previousTrackTooNew()) {
            // TODO
            track = null;
        } else {
            track = realm.createObject(Track.class);
        }

        // If the track is too short, just disregard it. We have nothing more to do, so just return.
        if (curLocationList.size() < 3) {
            realm.cancelTransaction();
            return;
        }

        RealmList<TrackLocation> trackLocations = track.getLocations();

        // We have a list of Location objects that represent our route. Convert these to TrackLocation objects
        // and add them to the track we're working on.

        Location lastLocation = null;
        double dist = 0;
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

        // Get a geolocation for the start and end points.
        new SMHttpRequest().findPlacesForLocation(curLocationList.get(0), this);
        new SMHttpRequest().findPlacesForLocation(curLocationList.get(curLocationList.size()-1), this);

        realm.commitTransaction();
    }

    private boolean previousTrackTooNew() {
        return false;
    }

    public void stopTracking()   {
        stopTracking(false);
    }


    public boolean isTracking() {
        return this.isTracking;
    }

    /***
     * Called when the GPS service has a new location ready. Adds the given location to the current track if we're
     * tracking locations.
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
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {
        this.stopTracking();
    }

    public void onActivityChanged(int activityType, int confidence) {
        Log.d("JC", "TrackingManager new activity");
        if (
                IbikeApplication.getSettings().getTrackingEnabled() &&
                (!this.isTracking && activityType == DetectedActivity.ON_BICYCLE || (DEBUG && activityType == DetectedActivity.TILTING))
           ) {
            Log.i("JC", "Activity changed to bicycle, starting track.");
            startTracking();
        } else if(activityType != DetectedActivity.ON_BICYCLE && this.isTracking) {
            Log.i("JC", "Activity changed away from bicycle, stopping track.");
            stopTracking();
        }

    }

    @Override
    public void onResponseReceived(int requestType, Object response) {

        // We got an answer from the geocoder, put it on the most recent route
        if (requestType == SMHttpRequest.REQUEST_FIND_PLACES_FOR_LOC) {
            SMHttpRequest.Address address = (SMHttpRequest.Address) response;

            /**
             * OK, we don't know if this answer relates to the start or the end of the route, so we have to cross-check.
             * We take the lat/lon from the Address object and check whether it's closest to the start or the end. This
             * creates some problems for tracks that begins and ends on the same spot, so we make an additional check to
             * see if the route is circular first. If it is, we set the start and end geotag at the same time.
             */

            // Get the most recent track
            realm = Realm.getInstance(IbikeApplication.getContext());
            Track mostRecentTrack = realm.allObjects(Track.class).last();

            TrackLocation start = mostRecentTrack.getLocations().first();
            TrackLocation end = mostRecentTrack.getLocations().last();

            // First convert tne starts and ends to Location objects
            Location startLocation = new Location("TrackingManager");
            startLocation.setLatitude(start.getLatitude());
            startLocation.setLongitude(start.getLongitude());

            Location endLocation = new Location("TrackingManager");
            endLocation.setLatitude(end.getLatitude());
            endLocation.setLongitude(end.getLongitude());

            boolean routeIsCircular = startLocation.distanceTo(endLocation) < 20;

            Location geocodedLocation = new Location("TrackingManager");
            geocodedLocation.setLatitude(address.lat);
            geocodedLocation.setLongitude(address.lon);

            // Figure out whether the geocoded position is closest to start or end, and set the appropriate field on
            // the Track object.
            double distanceToStart = startLocation.distanceTo(geocodedLocation);
            double distanceToEnd = endLocation.distanceTo(geocodedLocation);

            realm.beginTransaction();
            if (routeIsCircular || distanceToStart < distanceToEnd) {
                mostRecentTrack.setStart(address.street);
            }

            if (routeIsCircular || distanceToEnd < distanceToStart) {
                mostRecentTrack.setEnd(address.street);
            }
            realm.commitTransaction();
        }
    }
}
