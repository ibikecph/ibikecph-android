package com.spoiledmilk.ibikecph.tracking;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.RealmList;

/**
 * Created by jens on 2/25/15.
 */
public class TrackingManager implements LocationListener {
    private static TrackingManager instance = null;
    private BikeLocationService bikeLocationService;
    private boolean isTracking;

    private RealmList<TrackLocation> curLocationList;

    public TrackingManager() {
        bikeLocationService = IbikeApplication.getService();
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
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (isTracking) {
            curLocationList.add(TrackLocation.fromLocation(location));
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
