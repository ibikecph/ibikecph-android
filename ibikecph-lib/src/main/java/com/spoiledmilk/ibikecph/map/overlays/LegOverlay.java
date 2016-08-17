package com.spoiledmilk.ibikecph.map.overlays;

import android.location.Location;

import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Leg;

/**
 * An overlay that shows an Route on a MapView.
 * Created by kraen on 03-07-16.
 */
public class LegOverlay extends RoutePathOverlay {

    protected Leg leg;
    protected MapView mapView;

    public LegOverlay(MapView mapView, Leg leg) {
        super(mapView.getContext(), leg.getTransportType());
        this.mapView = mapView;
        this.leg = leg;
        routeChanged();
    }

    public void routeChanged() {
        clearPath();
        for(Location location: leg.getPoints()) {
            addPoint(location.getLatitude(), location.getLongitude());
        }
    }
}
