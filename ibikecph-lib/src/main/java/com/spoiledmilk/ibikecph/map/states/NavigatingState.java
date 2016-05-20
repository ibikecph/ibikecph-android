package com.spoiledmilk.ibikecph.map.states;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState {

    public NavigatingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        activity.getMapView().setUserLocationEnabled(true);
        UserLocationOverlay userLocationOverlay = activity.getMapView().getUserLocationOverlay();
        userLocationOverlay.enableFollowLocation();
        userLocationOverlay.setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        activity.getMapView().setMapViewListener(NavigationMapHandler.class);
    }

    @Override
    public void transitionAway(MapState to) {
        activity.getMapView().setUserLocationEnabled(false);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onBackPressed() {
        throw new UnsupportedOperationException("Back press has not been implemented yet.");
    }
}
