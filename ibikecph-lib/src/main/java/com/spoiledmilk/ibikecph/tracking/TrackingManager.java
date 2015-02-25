package com.spoiledmilk.ibikecph.tracking;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmList;

import java.util.Date;

/**
 * Created by jens on 2/25/15.
 */
public class TrackingManager implements LocationListener {
    private static TrackingManager instance = null;
    private BikeLocationService bikeLocationService;
    private boolean isTracking;

    private RealmList<TrackLocation> curLocationList;
    private Realm realm;

    public TrackingManager() {
        bikeLocationService = IbikeApplication.getService();
        realm = Realm.getInstance(IbikeApplication.getContext());
    }

    public static TrackingManager getInstance() {
        if (TrackingManager.instance == null)
            TrackingManager.instance = new TrackingManager();

        return instance;
    }

    public void startTracking() {
        Log.d("JC", "TrackingManager: Starting to track");
        bikeLocationService.addGPSListener(this);
        this.curLocationList = new RealmList<TrackLocation>();
        this.isTracking = true;
    }

    public void stopTracking() {
        bikeLocationService.removeGPSListener(this);
        this.isTracking = false;

        Track track = getLocationsAsTrack();
    }

    public Track getLocationsAsTrack() {
        Track t = new Track();
        t.setLocations(curLocationList);

        // TODO: Match against favorites

        return t;
    }

    /***
     * Called when the GPS service has a new location ready. Adds the given location to the current track if we're
     * tracking locations.
     * @param givenLocation
     */
    @Override
    public void onLocationChanged(Location givenLocation) {
        if (isTracking) {
            realm.beginTransaction();
            // Instantiate the object the right way
            TrackLocation realmLocation = realm.createObject(TrackLocation.class);

            // Set all the relevant fields
            realmLocation.setLatitude(givenLocation.getLatitude());
            realmLocation.setLongitude(givenLocation.getLongitude());
            realmLocation.setTimestamp(new Date(givenLocation.getTime()));
            realmLocation.setAltitude(givenLocation.getAltitude());

            // This is potentially bad. We don't have a measure of the horizontal and vertical accuracies, but we do have
            // one for the accuracy all in all. We just set that for both fields.
            realmLocation.setHorizontalAccuracy(givenLocation.getAccuracy());
            realmLocation.setVerticalAccuracy(givenLocation.getAccuracy());

            curLocationList.add(realmLocation);
            realm.commitTransaction();
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
}
