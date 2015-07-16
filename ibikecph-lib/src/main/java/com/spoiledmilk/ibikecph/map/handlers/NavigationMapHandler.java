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
import com.mapbox.mapboxsdk.overlay.*;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.NavigationOverviewInfoPane;
import com.spoiledmilk.ibikecph.navigation.RouteETAFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by jens on 5/30/15.
 */
public class NavigationMapHandler extends IBCMapHandler implements SMRouteListener, Serializable {
    private UserLocationOverlay userLocationOverlay;
    private static SMRoute route; // TODO: Static is bad, but we'll never have two NavigationMapHandlers anyway.
    private boolean cleanedUp = true;
    private TurnByTurnInstructionFragment turnByTurnFragment;
    private RouteETAFragment routeETAFragment;
    private Marker beginMarker;
    private Marker endMarker;

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
        this.turnByTurnFragment.updateTurn(firstElementRemoved);
        Log.d("JC", "NavigationMapHandler updateTurn");
    }

    @Override
    public void reachedDestination() {
        Log.d("JC", "NavigationMapHandler reachedDestination");
        this.turnByTurnFragment.reachedDestination();
    }

    @Override
    public void updateRoute() {
        Log.d("JC", "NavigationMapHandler updateRoute");
        if (this.turnByTurnFragment != null) {
            this.turnByTurnFragment.render();
        }

        if (this.routeETAFragment != null) {
            this.routeETAFragment.render();
        }
    }

    @Override
    public void startRoute() {
        Log.d("JC", "NavigationMapHandler startRoute");

    }

    @Override
    public void routeNotFound() {
        Log.d("JC", "NavigationMapHandler routeNotFound");

    }

    @Override
    public void routeRecalculationStarted() {
        Log.d("JC", "NavigationMapHandler routeRecalculationStarted");

    }

    @Override
    public void routeRecalculationDone() {
        Log.d("JC", "NavigationMapHandler routeRecalculationDone");

    }

    @Override
    public void serverError() {
        Log.d("JC", "NavigationMapHandler serverError");

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
    public void showRouteOverview(SMRoute route) {
        this.route = route;

        route.setListener(this);

        // TODO: Fix confusion between Location and LatLng objects
        PathOverlay path = new PathOverlay(Color.RED, 10);
        PathOverlay walkingPath = new PathOverlay(Color.GRAY, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();

        // Add a waypoint at the user's current position
        walkingPath.addPoint(new LatLng(IbikeApplication.getService().getLastValidLocation()));
        walkingPath.addPoint(route.waypoints.get(0).getLatitude(), route.waypoints.get(0).getLongitude());

        for (Location loc : route.waypoints) {
            path.addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        // Show the whole route, zooming to make it fit
        this.mapView.getOverlays().add(walkingPath);
        this.mapView.getOverlays().add(path);
        this.mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(waypoints), true, true, true, true);

        // Put markers at the beginning and end of the route.
        beginMarker = new Marker("", "", new LatLng(IbikeApplication.getService().getLastValidLocation()));
        endMarker = new Marker("", "", new LatLng(route.waypoints.get(route.waypoints.size()-1)));

        beginMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
        endMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));

        this.mapView.addMarker(beginMarker);
        this.mapView.addMarker(endMarker);

        // Set up the infoPane
        initInfopane();

        cleanedUp = false;
    }

    public void goButtonClicked() {
        Log.d("JC", "Go button clicked");

        // Zoom to the first waypoint
        Location start = route.getWaypoints().get(0);
        mapView.setCenter(new LatLng(start), true);
        mapView.setZoom(17f);

        mapView.addGPSOverlay();
        mapView.getGPSOverlay().setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        initInstructions();
    }

    /**
     * Sets up a NavigationOverviewInfoPane that shows the destination of the route and allows the user to press "go"
     */
    public void initInfopane() {
        NavigationOverviewInfoPane ifp;

        // Add info to the infoPane
        ifp = new NavigationOverviewInfoPane();

        ifp.setParent(this);

        Bundle b = new Bundle();
        b.putSerializable("NavigationMapHandler", this);
        ifp.setArguments(b);

        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, ifp, "NavigationOverviewInfoPane");
        ft.commit();
    }

    public void initInstructions() {
        Log.d("JC", "initInstructions");
        TurnByTurnInstructionFragment tbtf = new TurnByTurnInstructionFragment();
        RouteETAFragment ref = new RouteETAFragment();

        Bundle b = new Bundle();
        b.putSerializable("NavigationMapHandler", this);
        tbtf.setArguments(b);
        ref.setArguments(b);

        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.turnByTurnContainer, tbtf, "TurnByTurnPane");
        ft.replace(R.id.infoPaneContainer, ref, "RouteETAFragment");
        ft.commit();
    }

    /**
     * Tells the MapView to stop routing, i.e. instantiate an OverviewMapHandler, calling the destructor of this one.
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

        // remove any path overlays. Mapbox bug: Why do we need this spin lock?
        boolean foundPathOverlay = true;
        while(foundPathOverlay) {
            foundPathOverlay = false;
            for (Overlay overlay: this.mapView.getOverlays()) {
                if (overlay instanceof com.mapbox.mapboxsdk.overlay.PathOverlay) {
                    this.mapView.removeOverlay(overlay);
                    foundPathOverlay = true;
                }
            }
        }

        this.mapView.removeMarker(beginMarker);
        this.mapView.removeMarker(endMarker);

        this.mapView.invalidate();

        // And remove the fragment(s)
        FragmentTransaction transaction = mapView.getParentActivity().getFragmentManager().beginTransaction();
        if (getInfoPane() != null) {
            transaction.remove(getInfoPane());
        }
        if (getTurnByTurnFragment() != null) {
            transaction.remove(getTurnByTurnFragment());
        }
        transaction.commit();

        if (this.route != null) {
            route.cleanUp();
            route = null;
        }

        cleanedUp = true;
    }

    public NavigationOverviewInfoPane getInfoPane() {
        return (NavigationOverviewInfoPane) mapView.getParentActivity().getFragmentManager().findFragmentByTag("NavigationOverviewInfoPane");
    }

    public SMRoute getRoute() {
        return route;
    }


    public void setTurnByTurnFragment(TurnByTurnInstructionFragment turnByTurnFragment) {
        this.turnByTurnFragment = turnByTurnFragment;
    }

    public TurnByTurnInstructionFragment getTurnByTurnFragment() {
        return turnByTurnFragment;
    }

    public RouteETAFragment getRouteETAFragment() {
        return routeETAFragment;
    }

    public void setRouteETAFragment(RouteETAFragment routeETAFragment) {
        this.routeETAFragment = routeETAFragment;
    }
}
