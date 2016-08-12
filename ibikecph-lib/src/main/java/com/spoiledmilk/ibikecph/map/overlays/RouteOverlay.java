package com.spoiledmilk.ibikecph.map.overlays;

import android.location.Location;

import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;

/**
 * An overlay that shows an Route on a MapView.
 * Created by kraen on 03-07-16.
 */
public class RouteOverlay extends RoutePathOverlay implements SMRouteListener {

    protected SMRoute route;
    protected MapView mapView;

    public RouteOverlay(MapView mapView, SMRoute route) {
        super(mapView.getContext(), route.transportType);
        this.mapView = mapView;
        this.route = route;
        route.addListener(this);
        updateRoute();
    }

    @Override
    public void onDetach(MapView mapView) {
        super.onDetach(mapView);
        route.removeListener(this);
    }

    @Override
    public void reachedDestination() {

    }

    @Override
    public void updateRoute() {
        clearPath();
        for(Location location: route.getPoints()) {
            addPoint(location.getLatitude(), location.getLongitude());
        }
        mapView.invalidate();
    }

    @Override
    public void startRoute() {

    }

    @Override
    public void routeNotFound() {

    }

    @Override
    public void routeRecalculationStarted() {

    }

    @Override
    public void routeRecalculationDone() {

    }

    @Override
    public void serverError() {

    }
}
