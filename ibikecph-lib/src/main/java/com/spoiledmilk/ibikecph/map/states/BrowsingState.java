package com.spoiledmilk.ibikecph.map.states;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.map.MapActivity;

/**
 * The initial state, which basically displays the users current location and allows for the user
 * to browse the map.
 * Created by kraen on 02-05-16.
 */
public class BrowsingState extends MapState {

    public BrowsingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        if (from == null) {
            activity.getMapView().setUserLocationEnabled(true);
            activity.getMapView().getUserLocationOverlay().enableFollowLocation();
            activity.getMapView().setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
            activity.updateCompassIcon();
        } else {
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
    public void transitionAway(MapState to) {
        activity.getMapView().setUserLocationEnabled(false);
        // throw new UnsupportedOperationException("Not yet implemented");
    }
}
