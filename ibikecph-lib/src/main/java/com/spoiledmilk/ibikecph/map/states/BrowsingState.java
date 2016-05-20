package com.spoiledmilk.ibikecph.map.states;

import android.util.Log;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.handlers.IBCMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;

/**
 * The initial state, which basically displays the users current location and allows for the user
 * to browse the map.
 * Created by kraen on 02-05-16.
 */
public class BrowsingState extends MapState {

    protected IBCMapHandler mapViewHandler;

    public BrowsingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        activity.getMapView().setUserLocationEnabled(true);
        activity.getMapView().getUserLocationOverlay().setDrawAccuracyEnabled(true);
        activity.getMapView().getUserLocationOverlay().enableFollowLocation();
        activity.getMapView().setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        activity.updateCompassIcon();

        mapViewHandler = new OverviewMapHandler(activity.getMapView());
        activity.getMapView().setMapViewListener(mapViewHandler);
    }

    @Override
    public void transitionAway(MapState to) {
        activity.getMapView().setUserLocationEnabled(false);
        // TODO: Consider if we even need to destruct the map view at all.
        mapViewHandler.destructor();
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        return BackPressBehaviour.PROPAGATE;
    }
}
