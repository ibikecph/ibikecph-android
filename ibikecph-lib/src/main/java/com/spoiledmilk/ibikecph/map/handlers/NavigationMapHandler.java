package com.spoiledmilk.ibikecph.map.handlers;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.IBCMarker;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.MarkerType;
import com.spoiledmilk.ibikecph.map.ObservablePageInteger;
import com.spoiledmilk.ibikecph.map.OnIntegerChangeListener;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.navigation.NavigationOverviewInfoPane;
import com.spoiledmilk.ibikecph.navigation.RouteETAFragment;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.tracking.TrackingInfoPaneFragment;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.bearing.BearingToNorthProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by jens on 5/30/15.
 */
public class NavigationMapHandler extends IBCMapHandler implements SMRouteListener, Serializable, LocationListener {
    private UserLocationOverlay userLocationOverlay;
    private static SMRoute route; // TODO: Static is bad, but we'll never have two NavigationMapHandlers anyway.
    private static SMRoute[] breakRoute;
    private boolean cleanedUp = true;
    private transient TurnByTurnInstructionFragment turnByTurnFragment;
    private transient RouteETAFragment routeETAFragment;
    private transient IBCMarker beginMarker, endMarker;
    private transient ArrayList<IBCMarker> mMarker, sMarker, busMarker, boatMarker, trainMarker, walkMarker, icMarker, lynMarker, regMarker, exbMarker, nbMarker, tbMarker, fMarker;
    public static boolean isRouting;
    private transient PathOverlay[] path;
    private transient PathOverlay beginWalkingPath = null;
    private transient PathOverlay endWalkingPath = null;
    private int amount = 0;
    public static int routePos = 0; // Keep track of which routePiece we are currently tracking
    ArrayList<LatLng> waypoints;
    public static ObservablePageInteger obsInt;
    public static boolean displayExtraField = false;
    public static boolean displayGetOffAt = false;
    public static boolean isPublic = false;
    public static String getOffAt = "";
    public static String lastType = "";
    public static float dist = 0;
    private Address tryAgainSrc, tryAgainDst;


    public NavigationMapHandler(IBCMapView mapView) {
        super(mapView);
        startListener();
    }

    // Listener to draw the selected route from the breakRouteFragment
    private void startListener() {
        obsInt = new ObservablePageInteger();
        obsInt.setOnIntegerChangeListener(new OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                mapView.removeAllMarkers();
                showRouteOverviewPieces(newValue);
            }
        });
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
        Log.d("DV", "NavigationMapHandler reachedDestination");
        if (Geocoder.arrayLists != null && MapActivity.isBreakChosen) {
            Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).setListener(null);
            Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).reachedDestination = true;
            Log.d("DV", "NavigationMapHandler reachedDestination, removed with index = " + routePos + " og pageValue = " + obsInt.getPageValue());
            if ((routePos + 1) < Geocoder.arrayLists.get(obsInt.getPageValue()).size()) {
                displayGetOffAt = false;
                routePos = routePos + 1;
                Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).setListener(this);
                IbikeApplication.getService().addLocationListener(Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos));
                Log.d("DV", "NavigationMapHandler reachedDestination, ny listener er sat med index = " + routePos);
                if (Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).isPublic(Geocoder.arrayLists.get(NavigationMapHandler.obsInt.getPageValue()).get(NavigationMapHandler.routePos).transportType)) {
                    isPublic = true;
                    Log.d("DV", "NavigationMapHandler reachedDestination, public set to true");
                } else {
                    isPublic = false;
                    Log.d("DV", "NavigationMapHandler reachedDestination, public set to false");
                }
            } else {
                displayGetOffAt = false;
                if (this.turnByTurnFragment != null) {
                    this.turnByTurnFragment.reachedDestination();
                }
            }
        } else {
            if (this.turnByTurnFragment != null) {
                this.turnByTurnFragment.reachedDestination();
            }
        }


    }

    @Override
    public void updateRoute() {
        Log.d("JC", "NavigationMapHandler updateRoute");
        if (this.turnByTurnFragment != null) {
            if (MapActivity.isBreakChosen) {
                this.turnByTurnFragment.renderForBreakRoute(Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos));
            } else {
                this.turnByTurnFragment.render();
            }
        }

        if (this.routeETAFragment != null) {
            if (MapActivity.isBreakChosen) {
                this.routeETAFragment.renderForBreakRoute(Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos));
            } else {
                this.routeETAFragment.render(this);
            }
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
        removeAnyPathOverlays();
        PathOverlay path = null;
        if (IbikeApplication.getAppName().equals("CykelPlanen")) {
            path = new PathOverlay(Color.parseColor("#FF6600"), 10);
        } else {
            path = new PathOverlay(Color.RED, 10);
        }

        if (this.route != null) {
            for (Location loc : this.route.waypoints) {
                path.addPoint(loc.getLatitude(), loc.getLongitude());
            }

            this.mapView.getOverlays().add(path);
        }
    }

    @Override
    public void routeRecalculationDone(String type) {
        Log.d("JC", "NavigationMapHandler routeRecalculationDone");
        removeAnyPathOverlays();
        PathOverlay[] path = new PathOverlay[Geocoder.arrayLists.get(obsInt.getPageValue()).size()];

        // Redraw the whole route
        for (int i = 0; i < Geocoder.arrayLists.get(obsInt.getPageValue()).size(); i++) {
            //Log.d("DV", "REDRAWING WITH TYPE = " + Geocoder.arrayLists.get(obsInt.getPageValue()).get(i).transportType);
            if (Geocoder.arrayLists.get(obsInt.getPageValue()).get(i).transportType.equals("BIKE")) {
                path[i] = new PathOverlay(Color.parseColor("#FF6600"), 10);
            } else {
                path[i] = new PathOverlay(Color.GRAY, 10);
            }
            for (Location loc : Geocoder.arrayLists.get(obsInt.getPageValue()).get(i).waypoints) {
                path[i].addPoint(loc.getLatitude(), loc.getLongitude());
            }
        }

        for (int i = 0; i < path.length; i++) {
            this.mapView.getOverlays().add(path[i]);
        }

    }

    @Override
    public void serverError() {
        Log.d("JC", "NavigationMapHandler serverError");

    }

    @Override
    public void destructor() {
        Log.d("JC", "Destructing NavigationMapHandler");

        if (this.route != null) {
            route.cleanUp();
            route.setListener(null);
            IbikeApplication.getService().removeLocationListener(route);
            //route = null;
        }

        cleanUp();
    }

    /*
     pos = the position chosen in the
     breakRouteFragment in order to draw the correct route
     */
    public void showRouteOverviewPieces(int pos) {

        ArrayList<ArrayList<SMRoute>> routes = Geocoder.arrayLists;

        amount = routes.get(pos).size();
        path = new PathOverlay[amount];
        mMarker = new ArrayList<IBCMarker>();
        boatMarker = new ArrayList<IBCMarker>();
        walkMarker = new ArrayList<IBCMarker>();
        sMarker = new ArrayList<IBCMarker>();
        busMarker = new ArrayList<IBCMarker>();
        trainMarker = new ArrayList<IBCMarker>();
        icMarker = new ArrayList<IBCMarker>();
        lynMarker = new ArrayList<IBCMarker>();
        regMarker = new ArrayList<IBCMarker>();
        exbMarker = new ArrayList<IBCMarker>();
        nbMarker = new ArrayList<IBCMarker>();
        tbMarker = new ArrayList<IBCMarker>();
        fMarker = new ArrayList<IBCMarker>();

        breakRoute = new SMRoute[amount];

        for (int j = 0; j < routes.get(pos).size(); j++) {
            showBreakRouteOverview(routes.get(pos).get(j), j);
        }

    }


    /**
     * Brings up the whole route for the user, shows the address in the info pane.
     * The idea is that the user should start the route from this view.
     *
     * @param route
     */
    public void showRouteOverview(SMRoute route) {

        if (Geocoder.arrayLists != null) {
            for (int i = 0; i < Geocoder.arrayLists.size(); i++) {
                for (int j = 0; j < Geocoder.arrayLists.get(i).size(); j++) {
                    Geocoder.arrayLists.get(i).get(j).setListener(null);
                    IbikeApplication.getService().removeLocationListener(Geocoder.arrayLists.get(i).get(j));
                }
            }
        }

        this.route = route;
        IbikeApplication.getService().addLocationListener(route);
        this.mapView.removeAllMarkers();
        this.cleanUp();

        Log.d("DV_break", "showRouteOverview");

        route.setListener(this);

        // Set up the infoPane
        initInfopane();

        // TODO: Fix confusion between Location and LatLng objects
        PathOverlay path = null;
        if (IbikeApplication.getAppName().equals("CykelPlanen")) {
            path = new PathOverlay(Color.parseColor("#FF6600"), 10);
        } else {
            path = new PathOverlay(Color.RED, 10);
        }
        PathOverlay beginWalkingPath = new PathOverlay(Color.GRAY, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();

        // Add a waypoint at the user's current position
        if (route.startAddress != null && route.startAddress.isCurrentLocation() && !route.waypoints.isEmpty()) {
            beginWalkingPath.addPoint(new LatLng(IbikeApplication.getService().getLastValidLocation()));
            beginWalkingPath.addPoint(route.waypoints.get(0).getLatitude(), route.waypoints.get(0).getLongitude());
        }

        for (Location loc : route.waypoints) {
            path.addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        // Start the zoom to the bounding box around the waypoints of the route.
        BoundingBox boundingBox = BoundingBox.fromLatLngs(waypoints);

        double north = boundingBox.getLatNorth();
        double east = boundingBox.getLonEast();
        double west = boundingBox.getLonWest();
        double south = boundingBox.getLatSouth();

        double latitudeDiff = Math.abs(north - south) * 0.2;

        double longitudeDiff = Math.abs(east - west) * 0.2;

        // Add 20% padding
        ArrayList<LatLng> paddedWaypoints = new ArrayList<LatLng>();
        LatLng ne = new LatLng(north + latitudeDiff, east + longitudeDiff);
        LatLng sw = new LatLng(south - latitudeDiff, west - longitudeDiff);
        paddedWaypoints.add(ne);
        paddedWaypoints.add(sw);

        this.mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(paddedWaypoints), true, true, false, true);


        // We also want a grey line if the user is expected to walk somewhere we cannot route directly to.
        PathOverlay endWalkingPath = new PathOverlay(Color.GRAY, 10);
        LatLng lastPoint = new LatLng(route.waypoints.get(route.waypoints.size() - 1));
        LatLng realLastPoint = route.getRealEndLocation();
        endWalkingPath.addPoint(lastPoint);
        endWalkingPath.addPoint(realLastPoint);

        Log.d("JC", "distance: " + lastPoint.distanceTo(realLastPoint));

        // Show the whole route, zooming to make it fit
        this.mapView.addOverlay(beginWalkingPath);
        this.mapView.addOverlay(endWalkingPath);
        this.mapView.addOverlay(path);

        // Put markers at the beginning and end of the route. We use the "real" end location, which means the place that
        // the user tapped.

        beginMarker = new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT);
        endMarker = new IBCMarker("", "", new LatLng(route.getRealEndLocation()), MarkerType.PATH_ENDPOINT);

        beginMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
        endMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));

        this.mapView.addMarker(beginMarker);
        this.mapView.addMarker(endMarker);


        Log.d("NavitationMapHandler", "Printing the " + this.mapView.getOverlays().size() + " overlays:");
        for(Overlay overlay: this.mapView.getOverlays()) {
            Log.d("NavitationMapHandler", "\tOverlay: " + overlay + " isEnabled=" + overlay.isEnabled());
        }

        cleanedUp = false;
    }

    /*
    Used only for breakRoute. Looks a lot like the other showBreakRouteOverview,
    but due to too many if conditions etc, it's easier to make a separate method
     */
    public void showBreakRouteOverview(SMRoute route, int position) {
        //this.route = route;
        //breakRoute[position] = route;
        this.cleanUp();

        Log.d("DV_break", "showBreakRouteOverview");

        this.route.setListener(null);

        if (position == 0) {

            if (Geocoder.arrayLists != null) {
                for (int i = 0; i < Geocoder.arrayLists.size(); i++) {
                    for (int j = 0; j < Geocoder.arrayLists.get(i).size(); j++) {
                        Geocoder.arrayLists.get(i).get(j).setListener(null);
                        IbikeApplication.getService().removeLocationListener(Geocoder.arrayLists.get(i).get(j));
                    }
                }
            }

            Log.d("DV", "Setting listener from showBreakRouteOverview");
            route.setListener(this);
            IbikeApplication.getService().addLocationListener(route);
        }

        removeAnyPathOverlays();

        // TODO: Fix confusion between Location and LatLng objects
        if (IbikeApplication.getAppName().equals("CykelPlanen")) {
            if (route.transportType != null && route.transportType.equals("BIKE")) {
                // Only draw an orange line for bike route pieces
                path[position] = new PathOverlay(Color.parseColor("#FF6600"), 10);
            } else {
                path[position] = new PathOverlay(Color.GRAY, 10);
            }
        } else {
            path[position] = new PathOverlay(Color.RED, 10);
        }

        if (position == 0) {
            // Set up the infoPane
            initInfopane(); //Should only be run once.
            waypoints = new ArrayList<LatLng>();
            beginWalkingPath = new PathOverlay(Color.GRAY, 10);
            // Add a waypoint at the user's current position
            if (route.startAddress != null) {
                beginWalkingPath.addPoint(new LatLng(IbikeApplication.getService().getLastValidLocation()));
                beginWalkingPath.addPoint(route.waypoints.get(0).getLatitude(), route.waypoints.get(0).getLongitude());
            }
        }

        for (Location loc : route.waypoints) {
            path[position].addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        if (amount - 1 == position) {
            endWalkingPath = new PathOverlay(Color.GRAY, 10);
            // We also want a grey line if the user is expected to walk somewhere we cannot route directly to.
            LatLng lastPoint = new LatLng(route.waypoints.get(route.waypoints.size() - 1));
            LatLng realLastPoint = route.getRealEndLocation();
            endWalkingPath.addPoint(lastPoint);
            endWalkingPath.addPoint(realLastPoint);

            Log.d("JC", "distance: " + lastPoint.distanceTo(realLastPoint));
        }

        // Show the whole route, zooming to make it fit
        if (amount - 1 == position) {
            this.mapView.getOverlays().add(beginWalkingPath);
            this.mapView.getOverlays().add(endWalkingPath);
        }

        // Loop and add lines to be drawn
        if (amount - 1 == position) {
            Log.d("DV", "Sidste position!");
            for (int i = 0; i < path.length; i++) {
                this.mapView.getOverlays().add(path[i]);
            }
        }

        // Put markers at the beginning and end of the route. We use the "real" end location, which means the place that
        // the user tapped.
        if (position == 0) {
            beginMarker = new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT);
            beginMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
        }
        if (amount - 1 == position) {
            endMarker = new IBCMarker("", "", new LatLng(route.getRealEndLocation()), MarkerType.PATH_ENDPOINT);
            endMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));
        }

        // Set the different marker types
        if (route.transportType != null && route.transportType.equals("M")) {
            mMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_metro))));
        }
        if (route.transportType != null && route.transportType.equals("S")) {
            sMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_s))));
        }
        if (route.transportType != null && route.transportType.equals("WALK")) {
            walkMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_walk))));
        }
        if (route.transportType != null && route.transportType.equals("TOG")) {
            trainMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_train))));
        }
        if (route.transportType != null && route.transportType.equals("BUS")) {
            busMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_bus))));
        }
        if (route.transportType != null && route.transportType.equals("IC")) {
            icMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_train))));
        } //fix
        if (route.transportType != null && route.transportType.equals("LYN")) {
            lynMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_train))));
        } //fix
        if (route.transportType != null && route.transportType.equals("REG")) {
            regMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_train))));
        } //fix
        if (route.transportType != null && route.transportType.equals("EXB")) {
            exbMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_bus))));
        } //fix
        if (route.transportType != null && route.transportType.equals("NB")) {
            nbMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_bus))));
        } //fix
        if (route.transportType != null && route.transportType.equals("TB")) {
            tbMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_bus))));
        } // fix
        if (route.transportType != null && route.transportType.equals("F")) {
            fMarker.add(new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT).setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_boat))));
        }

        // Loop and set markers
        if (amount - 1 == position)

        {
            this.mapView.addMarker(beginMarker);
            this.mapView.addMarker(endMarker);

            for (int i = 0; i < mMarker.size(); i++) {
                this.mapView.addMarker(mMarker.get(i));
            }
            for (int i = 0; i < sMarker.size(); i++) {
                this.mapView.addMarker(sMarker.get(i));
            }
            for (int i = 0; i < busMarker.size(); i++) {
                this.mapView.addMarker(busMarker.get(i));
            }
            for (int i = 0; i < trainMarker.size(); i++) {
                this.mapView.addMarker(trainMarker.get(i));
            }
            for (int i = 0; i < walkMarker.size(); i++) {
                this.mapView.addMarker(walkMarker.get(i));
            }
            for (int i = 0; i < icMarker.size(); i++) {
                this.mapView.addMarker(icMarker.get(i));
            }
            for (int i = 0; i < lynMarker.size(); i++) {
                this.mapView.addMarker(lynMarker.get(i));
            }
            for (int i = 0; i < regMarker.size(); i++) {
                this.mapView.addMarker(regMarker.get(i));
            }
            for (int i = 0; i < exbMarker.size(); i++) {
                this.mapView.addMarker(exbMarker.get(i));
            }
            for (int i = 0; i < nbMarker.size(); i++) {
                this.mapView.addMarker(nbMarker.get(i));
            }
            for (int i = 0; i < tbMarker.size(); i++) {
                this.mapView.addMarker(tbMarker.get(i));
            }
            for (int i = 0; i < fMarker.size(); i++) {
                this.mapView.addMarker(fMarker.get(i));
            }

        }

        if (amount - 1 == position) {

            BoundingBox boundingBox = BoundingBox.fromLatLngs(waypoints);

            double north = boundingBox.getLatNorth();
            double east = boundingBox.getLonEast();
            double west = boundingBox.getLonWest();
            double south = boundingBox.getLatSouth();

            double latitudeDiff = Math.abs(north - south) * 0.25;

            double longitudeDiff = Math.abs(east - west) * 0.25;

            //Add 20% padding
            ArrayList<LatLng> paddedWaypoints = new ArrayList<LatLng>();
            LatLng ne = new LatLng(north + latitudeDiff, east + longitudeDiff);
            LatLng sw = new LatLng(south - latitudeDiff, west - longitudeDiff);
            paddedWaypoints.add(ne);
            paddedWaypoints.add(sw);

            this.mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(paddedWaypoints), true, true, false, true);
        }

        breakRoute[position] = route;
        cleanedUp = false;
    }

    public void goButtonClicked() {
        Log.d("JC", "Go button clicked");

        // Zoom to the first waypoint
        Location start = route.getWaypoints().get(0);
        mapView.setCenter(new LatLng(start), true);
        mapView.setZoom(17f);

        mapView.addUserLocationOverlay();
        mapView.getUserLocationOverlay().setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        //registerBearingRotation();

        IbikeApplication.getService().addLocationListener(this);

        isRouting = true;

        showProblemButton();

        initInstructions();
    }

    private void registerBearingRotation() {
        BearingToNorthProvider bearingToNorthProvider = new BearingToNorthProvider(this.mapView.getContext()/*, 1, 0.1, 10*/);
        final IBCMapView finalMapView = this.mapView;

        bearingToNorthProvider.setChangeEventListener(new BearingToNorthProvider.ChangeEventListener() {
            @Override
            public void onBearingChanged(double bearing) {
                finalMapView.setMapOrientation(360 - ((float) bearing) - 90);
            }
        });

        bearingToNorthProvider.start();
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
        MapActivity.breakFrag.setVisibility(View.GONE);
        MapActivity.progressBarHolder.setVisibility(View.GONE);
    }

    // TODO: Move this the the MapState classes
    /*
     * Tells the MapView to stop routing, i.e. instantiate an OverviewMapHandler, calling the destructor of this one.
     *
     * @return false because we need the user to
     * /
    public boolean onBackPressed() {
        if (!cleanedUp) {
            IbikePreferences settings;
            settings = IbikeApplication.getSettings();
            // Navigation happens in two steps. First is showing the route, second is actually following it turn-by-turn
            // If we're in turn-by-turn mode, go back to showing the route. If we're seeing the route, go back to
            // showing the overview.
            if (isRouting) {
                this.showRouteOverview(this.route);
                isRouting = false;
            } else {

                if (settings.getTrackingEnabled()) {
                    FragmentManager fm = mapView.getParentActivity().getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.infoPaneContainer, new TrackingInfoPaneFragment());
                    ft.commit();
                    MapActivity.frag.setVisibility(View.VISIBLE);
                } else {
                    MapActivity.frag.setVisibility(View.GONE);
                }
                this.mapView.changeState(IBCMapView.MapViewState.DEFAULT);
            }
            return false;
        }


        return true;
    }
    */

    private void removeAnyPathOverlays() {
        // It's suspected that a bug in Mapbox gives trouble when iterating getOverlays and removing
        // from it at the same time. Therefore we use an iterator to remove overlays.
        //Log.d("NavigationMapHandler", "Before removal " + this.mapView.getOverlays().size() + " overlays exists.");
        Iterator<Overlay> overlays = this.mapView.getOverlays().iterator();
        while(overlays.hasNext()) {
            Overlay overlay = overlays.next();
            if (overlay instanceof PathOverlay) {
                //Log.d("NavigationMapHandler", "\tRemoving " + overlay);
                overlays.remove();
            }
        }
        /*
        Log.d("NavigationMapHandler", "After removal " + this.mapView.getOverlays().size() + " overlays exists.");
        for(Overlay o: this.mapView.getOverlays()) {
            Log.d("NavigationMapHandler", "\tOverlay: " + o);
        }
        */
    }

    public void cleanUp() {
        if (cleanedUp) return;

        removeAnyPathOverlays();
        // this.mapView.removeAllMarkers();

        if (beginMarker != null) {
            this.mapView.removeMarker(beginMarker);
        }

        if (endMarker != null) {
            this.mapView.removeMarker(endMarker);
        }

        // Return the orientation to normal.
        this.mapView.setMapOrientation(0);
        IbikeApplication.getService().removeLocationListener(this);

        // TODO: Consider if this is needed - removing markers invalidates the MapView internally.
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

        // Remove the problem button
        hideProblemButton();

        cleanedUp = true;
    }

    public void hideProblemButton() {
        this.mapView.getParentActivity().hideProblemButton();
    }

    public void showProblemButton() {
        this.mapView.getParentActivity().showProblemButton();
    }

    public NavigationOverviewInfoPane getInfoPane() {
        return (NavigationOverviewInfoPane) mapView.getParentActivity().getFragmentManager().findFragmentByTag("NavigationOverviewInfoPane");
    }

    public static SMRoute getRoute() {
        return route;
    }

    public static SMRoute getBreakRoute(int pos) {
        return breakRoute[pos];
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

    /**
     * Calculates a new route from either a new source point, a new destination point,  or both. If null supplied, then
     * we assume the user to leave the other field unchanged.
     *
     * @param givenSrc
     * @param givenDst
     */
    private void changeAddress(Address givenSrc, Address givenDst, RouteType routeType) {
        Address newSrc, newDst;
        RouteType newRouteType;

        if (givenSrc == null) {
            newSrc = this.getRoute().startAddress;
        } else {
            newSrc = givenSrc;
        }

        if (givenDst == null) {
            newDst = this.getRoute().endAddress;
        } else {
            newDst = givenDst;
        }

        if (routeType == null) {
            newRouteType = this.getRoute().getType();
        } else {
            newRouteType = routeType;
        }

        final Address finalSource = newSrc;
        final Address finalDestination = newDst;
        tryAgainSrc = finalSource;
        tryAgainDst = finalDestination;


        Log.d("DV_break", "NavigationMaphandler: Geocoder.getroute!");
        Geocoder.getRoute(finalSource.getLocation(), finalDestination.getLocation(), new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                route.startStationName = finalSource.getStreetAddress();
                route.endStationName = finalDestination.getStreetAddress();
                route.startAddress = finalSource;
                route.endAddress = finalDestination;

                Log.d("DV_break", "NavigationMaphandler: Calling showRoute!");
                mapView.showRoute(route);
            }

            @Override
            public void onSuccess(boolean isBreak) {
                Log.d("DV", "isRouting = " + isRouting);
                if (MapActivity.isBreakChosen && !isRouting) {
                    //Måske fjern route helt herfra og sæt de her ting i Geocoder?
                /*route.startStationName = finalSource.getStreetAddress();
                route.endStationName = finalDestination.getStreetAddress();
                route.startAddress = finalSource;
                route.endAddress = finalDestination;*/

                    Log.d("DV_break", "NavigationMaphandler: Calling showRoute with breakRoute!");
                    mapView.showMultipleRoutes();
                } else {
                    MapActivity.breakFrag.setVisibility(View.GONE);
                }

            }

            @Override
            public void onFailure() {
                Log.d("DV_break", "IBCMapView, onFailure!");
                displayTryAgain();
            }

        }, null, newRouteType);

    }

    public void displayTryAgain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.mapActivityContext);
        String[] options = {IbikeApplication.getString("Cancel"), IbikeApplication.getString("Try_again")};
        builder.setTitle(IbikeApplication.getString("error_route_not_found"))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (which == 0) {
                            MapActivity.breakFrag.setVisibility(View.GONE);
                            MapActivity.progressBarHolder.setVisibility(View.GONE);
                            MapActivity.frag.setVisibility(View.GONE);
                            mapView.removeAllMarkers();
                            removeAnyPathOverlays();
                        } else {
                            changeAddress(tryAgainSrc, tryAgainDst, RouteType.BREAK);
                        }

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                MapActivity.breakFrag.setVisibility(View.GONE);
                MapActivity.progressBarHolder.setVisibility(View.GONE);
                MapActivity.frag.setVisibility(View.GONE);
                mapView.removeAllMarkers();
                removeAnyPathOverlays();
            }
        });
        dialog.show();
    }

    public void changeDestinationAddress(Address a) {
        changeAddress(null, a, null);
    }

    public void changeSourceAddress(Address a) {
        changeAddress(a, null, null);
    }

    public void changeRouteType(RouteType routeType) {
        changeAddress(null, null, routeType);
    }

    public void flipRoute() {
        Log.d("JC", "Flipping route");
        changeAddress(this.getRoute().endAddress, this.getRoute().startAddress, null);
    }

    /**
     * This was copied from the Spoiled Milk code. It would of course make more sense to just pass the SMRoute instead
     * of this. TODO i guess.
     */
    public void problemButtonPressed() {
        Log.d("JC", "Problem button pressed");

        Intent i = new Intent(mapView.getParentActivity(), IssuesActivity.class);

        ArrayList<String> turnsArray = new ArrayList<String>();
        for (SMTurnInstruction instruction : NavigationMapHandler.getRoute().getTurnInstructions()) {
            turnsArray.add(instruction.generateFullDescriptionString());
        }

        i.putStringArrayListExtra("turns", turnsArray);
        i.putExtra("startLoc", NavigationMapHandler.getRoute().getStartLocation().toString());
        i.putExtra("endLoc", NavigationMapHandler.getRoute().getEndLocation().toString());
        i.putExtra("startName", NavigationMapHandler.getRoute().startStationName);
        i.putExtra("endName", NavigationMapHandler.getRoute().endStationName);

        mapView.getParentActivity().startActivity(i);

    }

    @Override
    public void onLocationChanged(Location location) {
        if (this.isRouting) {
            mapView.setMapOrientation(-1 * location.getBearing());
        }
    }
}
