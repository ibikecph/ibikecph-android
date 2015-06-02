package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.navigation.NavigationOverviewInfoPane;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;

import java.util.ArrayList;

/**
 * Created by jens on 5/30/15.
 */
public class NavigationMapHandler extends IBCMapHandler implements SMRouteListener {

    private SMRoute route;

    public NavigationMapHandler(IBCMapView mapView) {
        super(mapView);
    }

    @Override
    public void onShowMarker(MapView mapView, Marker marker) {


    }

    @Override
    public void onHideMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {

    }

    @Override
    public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

    }



    //// SMRouteListener methods
    @Override
    public void updateTurn(boolean firstElementRemoved) {

    }

    @Override
    public void reachedDestination() {

    }

    @Override
    public void updateRoute() {

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

    @Override
    public void destructor() {

    }

    public void startRouting(SMRoute route) {
        this.route = route;

        route.setListener(this);

        // TODO: Fix confusion between Location and LatLng objects
        PathOverlay path = new PathOverlay(Color.RED, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();
        for (Location loc : route.waypoints) {
            path.addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        // Get rid of old overlays
        this.mapView.getOverlays().clear();

        // Show the whole route, zooming to make it fit
        this.mapView.getOverlays().add(path);
        this.mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(waypoints), true, true, true, true);

        // Add info to the infoPane
        InfoPaneFragment ifp = new NavigationOverviewInfoPane();
        Bundle b = new Bundle();
        b.putString("endStationName", route.endStationName);
        ifp.setArguments(b);


        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.infoPaneContainer, ifp);

        // Only push the route to the back stack if it's the first one. Pushing back on subsequent ones should result in
        // the state changing back to DEFAULT.
        //if (firstTrack) {
        ft.addToBackStack(null);
        //}

        ft.commit();
    }
}
