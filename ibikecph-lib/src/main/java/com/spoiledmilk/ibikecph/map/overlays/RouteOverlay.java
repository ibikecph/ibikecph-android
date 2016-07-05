package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;

import java.util.ArrayList;
import java.util.List;

/**
 * An overlay that shows an Route on a MapView.
 * Created by kraen on 03-07-16.
 */
public class RouteOverlay extends RoutePathOverlay implements SMRouteListener {

    protected SMRoute route;

    public RouteOverlay(Context context, SMRoute route) {
        super(context, route.transportType);
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
        for(Location location: route.getWaypoints()) {
            addPoint(location.getLatitude(), location.getLongitude());
        }
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
