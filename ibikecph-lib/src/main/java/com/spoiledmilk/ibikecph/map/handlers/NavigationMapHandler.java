package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.navigation.NavigationOverviewInfoPane;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;

import java.util.ArrayList;

/**
 * Created by jens on 5/30/15.
 */
public class NavigationMapHandler extends IBCMapHandler implements SMRouteListener {

    private SMRoute route;
    private PathOverlay path;
    private NavigationOverviewInfoPane ifp;
    private boolean cleanedUp = true;

    public NavigationMapHandler(IBCMapView mapView) {
        super(mapView);
        Log.d("JC", "Instantiating NavigationMapHandler");

        mapView.setMapViewListener(this);
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
        Log.d("JC", "NavigationMapHandler.onTapMap");

    }

    @Override
    public void onLongPressMap(MapView mapView, final ILatLng iLatLng) {
        Log.d("JC", "NavigationMapHandler.onLongPressMap");
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
        Log.d("JC", "Destructing NavigationMapHandler");
        cleanUp();
    }

    /**
     * Brings up the whole route for the user, shows the address in the info pane. The idea is that the user should
     * start the route from this view.
     * @param route
     */
    public void commenceRouting(SMRoute route) {
        this.route = route;

        route.setListener(this);

        // TODO: Fix confusion between Location and LatLng objects
        path = new PathOverlay(Color.RED, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();
        for (Location loc : route.waypoints) {
            path.addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        // Show the whole route, zooming to make it fit
        this.mapView.getOverlays().add(path);
        this.mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(waypoints), true, true, true, true);

        // Set up the infoPane
        initInfopane();

        cleanedUp = false;
    }

    public void goButtonClicked() {
        Log.d("JC", "Go button clicked");

        // Zoom to the first waypoint
        Location start = route.getWaypoints().get(0);
        mapView.setCenter(new LatLng(start), true);
        mapView.setZoom(mapView.getMaxZoomLevel());

    }

    /**
     * Sets up a NavigationOverviewInfoPane that shows the destination of the route and allows the user to press "go"
     */
    public void initInfopane() {
        // Add info to the infoPane
        ifp = new NavigationOverviewInfoPane();
        ifp.setParent(this);

        Bundle b = new Bundle();
        b.putString("endStationName", route.endStationName);
        ifp.setArguments(b);

        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, ifp);
        ft.commit();
    }

    /**
     * Remove the path before returning.
     * @return false because we need the user to
     */
    public boolean onBackPressed() {
        if (!cleanedUp) {
            this.mapView.stopRouting();
            return false;
        }

        return true;
    }

    public void cleanUp() {
        if (cleanedUp) return;

        this.mapView.getOverlays().remove(path);
        this.mapView.invalidate();

        // And remove the fragment
        mapView.getParentActivity().getFragmentManager().beginTransaction().remove(ifp).commit();

        cleanedUp = true;
    }
}
