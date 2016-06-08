package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.IBCMarker;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.MarkerType;
import com.spoiledmilk.ibikecph.map.ObservablePageInteger;
import com.spoiledmilk.ibikecph.map.OnIntegerChangeListener;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.fragments.RouteSelectionFragment;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by jens on 5/30/15.
 */
public class NavigationMapHandler extends IBCMapHandler implements SMRouteListener, Serializable, LocationListener {
    private UserLocationOverlay userLocationOverlay;
    private static SMRoute route; // TODO: Static is bad, but we'll never have two NavigationMapHandlers anyway.
    private static SMRoute[] breakRoute;
    private boolean cleanedUp = true;
    private transient TurnByTurnInstructionFragment turnByTurnFragment;
    private transient NavigationETAFragment navigationETAFragment;
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
            Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).removeListener(this);
            Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).reachedDestination = true;
            Log.d("DV", "NavigationMapHandler reachedDestination, removed with index = " + routePos + " og pageValue = " + obsInt.getPageValue());
            if ((routePos + 1) < Geocoder.arrayLists.get(obsInt.getPageValue()).size()) {
                displayGetOffAt = false;
                routePos = routePos + 1;
                Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos).addListener(this);
                IBikeApplication.getService().addLocationListener(Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos));
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

        if (this.navigationETAFragment != null) {
            if (MapActivity.isBreakChosen) {
                this.navigationETAFragment.renderForBreakRoute(Geocoder.arrayLists.get(obsInt.getPageValue()).get(routePos));
            } else {
                this.navigationETAFragment.render(this);
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
        mapView.removeAllRouteOverlays();

        PathOverlay path = null;
        if (IBikeApplication.getAppName().equals("CykelPlanen")) {
            path = new PathOverlay(Color.parseColor("#FF6600"), 10);
        } else {
            path = new PathOverlay(Color.RED, 10);
        }

        if (this.route != null) {
            for (Location loc : this.route.waypoints) {
                path.addPoint(loc.getLatitude(), loc.getLongitude());
            }
            mapView.addRouteOverlay(path);
        }
    }

    @Override
    public void routeRecalculationDone(String type) {
        Log.d("JC", "NavigationMapHandler routeRecalculationDone");
        mapView.removeAllRouteOverlays();

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
            mapView.addRouteOverlay(path[i]);
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
            route.removeListeners();
            IBikeApplication.getService().removeLocationListener(route);
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
        this.route = route;
        IBikeApplication.getService().addLocationListener(route);
        this.mapView.removeAllMarkers();
        this.cleanUp();

        Log.d("DV_break", "showRouteOverview");

        route.addListener(this);

        // Set up the infoPane
        // initRouteSelectionFragment();
        // FIXME: initRouteSelectionFragment used to be called here ...

        // TODO: Fix confusion between Location and LatLng objects
        PathOverlay path = null;
        if (IBikeApplication.getAppName().equals("CykelPlanen")) {
            path = new PathOverlay(Color.parseColor("#FF6600"), 10);
        } else {
            path = new PathOverlay(Color.RED, 10);
        }
        PathOverlay beginWalkingPath = new PathOverlay(Color.GRAY, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();

        // Add a waypoint at the user's current position
        if (route.startAddress != null && route.startAddress.isCurrentLocation() && !route.waypoints.isEmpty()) {
            beginWalkingPath.addPoint(new LatLng(IBikeApplication.getService().getLastValidLocation()));
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
        mapView.addRouteOverlay(beginWalkingPath);
        mapView.addRouteOverlay(endWalkingPath);
        mapView.addRouteOverlay(path);

        // Put markers at the beginning and end of the route. We use the "real" end location, which means the place that
        // the user tapped.

        beginMarker = new IBCMarker("", "", new LatLng(route.getStartLocation()), MarkerType.PATH_ENDPOINT);
        endMarker = new IBCMarker("", "", new LatLng(route.getRealEndLocation()), MarkerType.PATH_ENDPOINT);

        beginMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_start)));
        endMarker.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));

        this.mapView.addMarker(beginMarker);
        this.mapView.addMarker(endMarker);

        /*
        Log.d("NavitationMapHandler", "Printing the " + this.mapView.getOverlays().size() + " overlays:");
        for(Overlay overlay: this.mapView.getOverlays()) {
            Log.d("NavitationMapHandler", "\tOverlay: " + overlay + " isEnabled=" + overlay.isEnabled());
        }
        */

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

        this.route.removeListeners();

        if (position == 0) {

            if (Geocoder.arrayLists != null) {
                for (int i = 0; i < Geocoder.arrayLists.size(); i++) {
                    for (int j = 0; j < Geocoder.arrayLists.get(i).size(); j++) {
                        Geocoder.arrayLists.get(i).get(j).removeListeners();
                        IBikeApplication.getService().removeLocationListener(Geocoder.arrayLists.get(i).get(j));
                    }
                }
            }

            Log.d("DV", "Setting listener from showBreakRouteOverview");
            route.addListener(this);
            IBikeApplication.getService().addLocationListener(route);
        }

        mapView.removeAllRouteOverlays();

        // TODO: Fix confusion between Location and LatLng objects
        if (IBikeApplication.getAppName().equals("CykelPlanen")) {
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
            // initRouteSelectionFragment(); //Should only be run once.
            // FIXME: initRouteSelectionFragment used to be called here!
            waypoints = new ArrayList<LatLng>();
            beginWalkingPath = new PathOverlay(Color.GRAY, 10);
            // Add a waypoint at the user's current position
            if (route.startAddress != null) {
                beginWalkingPath.addPoint(new LatLng(IBikeApplication.getService().getLastValidLocation()));
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
            mapView.addRouteOverlay(beginWalkingPath);
            mapView.addRouteOverlay(endWalkingPath);
        }

        // Loop and add lines to be drawn
        if (amount - 1 == position) {
            Log.d("DV", "Sidste position!");
            for (int i = 0; i < path.length; i++) {
                mapView.addRouteOverlay(path[i]);
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

    public void cleanUp() {
        if (cleanedUp) {
            Log.d("NavigationMapHandler", "Skipping clean up - as we're already clean.");
            return;
        }

        mapView.removeAllRouteOverlays();

        if (beginMarker != null) {
            this.mapView.removeMarker(beginMarker);
        }

        if (endMarker != null) {
            this.mapView.removeMarker(endMarker);
        }

        // Return the orientation to normal.
        this.mapView.setMapOrientation(0);
        IBikeApplication.getService().removeLocationListener(this);

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

        cleanedUp = true;
    }

    public RouteSelectionFragment getInfoPane() {
        return (RouteSelectionFragment) mapView.getParentActivity().getFragmentManager().findFragmentByTag("RouteSelectionFragment");
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

    public NavigationETAFragment getNavigationETAFragment() {
        return navigationETAFragment;
    }

    public void setNavigationETAFragment(NavigationETAFragment navigationETAFragment) {
        this.navigationETAFragment = navigationETAFragment;
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
