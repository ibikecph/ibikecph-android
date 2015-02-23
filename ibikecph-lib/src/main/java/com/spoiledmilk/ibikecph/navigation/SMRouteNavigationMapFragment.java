// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapFragmentBase;
import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationActivity.InstrcutionViewState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.SensorData;
import com.spoiledmilk.ibikecph.util.Util;

public class SMRouteNavigationMapFragment extends MapFragmentBase implements SMRouteListener, MapListener, SensorEventListener {

    protected static final double COORDINATE_PADDING = 0.002;

    protected PathOverlay pathOverlay, pathOverlay2;
    protected int currentPathAlpha = IbikePreferences.ROUTE_ALPHA;
    public SMRoute route;
    private int routeColor = IbikePreferences.ROUTE_COLOR;
    protected ItemizedIconOverlay<OverlayItem> markerOverlay;
    protected ItemizedIconOverlay<OverlayItem> markerOverlayB;
    protected ItemizedIconOverlay<OverlayItem> markerStationOverlay;
    protected ItemizedIconOverlay<OverlayItem> markerStationOverlayB;
    public Location startLocation;
    public Location endLocation;
    public JsonNode jsonRoot;
    public String source;
    public String destination;
    protected String startName = "";
    protected String endName = "";
    public boolean currentlyRouting = false;
    protected boolean routingStarted = false;
    float lastBearing = 0;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    // private Sensor mOrientSensor;
    // private float[] mLastAccelerometer = new float[3];
    // private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    float compassOrientation = 0;
    protected TextView textA, textB;
    GpsMyLocationProvider locationProvider;

    SensorData accValues = new SensorData(1, 3), magValues = new SensorData(1, 3), orientValues = new SensorData(1, 3);

    boolean isTileSourceChecked = false;
    boolean isRecalculation = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        currentlyRouting = false;

        pathOverlay = new PathOverlay(getRouteColor(), getActivity());

        if (getActivity().getIntent().getExtras() != null) {
            startLocation = Util.locationFromCoordinates(getActivity().getIntent().getExtras().getDouble("start_lat"), getActivity().getIntent()
                    .getExtras().getDouble("start_lng"));
            endLocation = Util.locationFromCoordinates(getActivity().getIntent().getExtras().getDouble("end_lat"), getActivity().getIntent()
                    .getExtras().getDouble("end_lng"));
            if (getActivity().getIntent().getExtras().containsKey("json_root"))
                jsonRoot = Util.stringToJsonNode(getActivity().getIntent().getExtras().getString("json_root"));
            source = getActivity().getIntent().getExtras().getString("source");
            destination = getActivity().getIntent().getExtras().getString("destination");
        }

        getMapActivity().stopTrackingUser();
        if (getActivity().getIntent().getExtras().containsKey("start_name"))
            this.startName = getActivity().getIntent().getExtras().getString("start_name");
        if (getActivity().getIntent().getExtras().containsKey("end_name"))
            this.endName = getActivity().getIntent().getExtras().getString("end_name");
        start(startLocation, endLocation, jsonRoot, startName, endName);
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        textA = new TextView(getActivity());
        textA.setBackgroundResource(R.drawable.rounded_rectangle_22);
        textA.setTypeface(IbikeApplication.getNormalFont());
        textA.setTextColor(Color.BLACK);
        textA.setGravity(Gravity.CENTER);
        textA.setPadding(Util.dp2px(5), Util.dp2px(5), Util.dp2px(5), Util.dp2px(5));
        textA.setVisibility(View.GONE);
        textA.setTextSize(16);
        textA.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textA.setVisibility(View.GONE);
            }
        });
        mapView.addView(textA);
        textB = new TextView(getActivity());
        textB.setBackgroundResource(R.drawable.rounded_rectangle_22);
        textB.setTypeface(IbikeApplication.getNormalFont());
        textB.setTextColor(Color.BLACK);
        textB.setGravity(Gravity.CENTER);
        textB.setPadding(Util.dp2px(5), Util.dp2px(5), Util.dp2px(5), Util.dp2px(5));
        textB.setVisibility(View.GONE);
        textB.setTextSize(16);
        textB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                textB.setVisibility(View.GONE);
            }
        });
        mapView.addView(textB);

        locationOverlay.setMapFragment(this);

        mapView.directionShown = true;
        mapView.isNavigation = true;
    }

    protected int getRouteColor() {
        if (route != null && route.recalculationInProgress) {
            return routeColor;
        } else {
            return IbikePreferences.ROUTE_COLOR;
        }
    }

    String formatArrivalTime(int seconds) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, seconds);
        return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    @Override
    public void onResume() {
    	Log.d("JC", "SMRouteNavigationMapFragment onResume");
    	
        super.onResume();
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
        
        // make sure we've updated the route
        onLocationChanged(SMLocationManager.getInstance().getPrevLastValidLocation());
        onLocationChanged(SMLocationManager.getInstance().getLastValidLocation());
    }

    @Override
    public void onPause() {
        super.onPause();
        // mSensorManager.unregisterListener(this);
    }

    public boolean getTrackingMode() {
        return mapView.tracking;
    }

    public void setTrackingMode(boolean tracking) {
        mapView.tracking = tracking;
        if (tracking && !routingStarted) {
            locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
        } else if (!tracking || !mapView.directionShown) {
            locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
        } else {
            locationOverlay.setCustomIconCentered(R.drawable.direction_arrow, true);
        }
        rotateMap();
        if (mapView.tracking && lastAnimLoc != null) {
            animateMap(lastAnimLoc);
        }
    }

    protected void start(Location from, Location to, JsonNode jsonRoot, String startName, String endName) {
        mapView.setMapListener(this);
        this.startName = startName;
        this.endName = endName;
        route = new SMRoute();
        route.init(from, to, this, jsonRoot);
        if (locationOverlay == null) {
            locationOverlay = new SMMyLocationNewOverlay(getActivity(), locationProvider = new GpsMyLocationProvider(getActivity()), mapView);
        }
        locationOverlay.setRoute(route);
        showRouteOverview();
        drawPins();
    }

    // restart the route after changing bike/cargo vehicle
    public void restartRoute() {
        route = new SMRoute();
        route.init(startLocation, route.getEndLocation(), this, jsonRoot);
        locationOverlay.setRoute(route);
        mapView.getOverlays().remove(pathOverlay);
        showRouteOverview();
        drawPins();
    }

    // restart the route after it's been broken
    public void restartRoute(Location startStation, Location endStation, JsonNode jsonRoot, String startStationName, String endStationName, int statonIcon) {
        route = new SMRoute();
        route.isRouteBroken = true;
        route.startStation = startStation;
        route.endStation = endStation;
        route.startStationName = startStationName;
        route.endStationName = endStationName;
        route.stationIcon = statonIcon;
        route.init(startLocation, route.getEndLocation(), startStation, endStation, this, jsonRoot);
        locationOverlay.setRoute(route);
        mapView.getOverlays().remove(pathOverlay);
        ((SMRouteNavigationActivity) getActivity()).showRouteOverview();
        showRouteOverview();
        drawPins();
    }

    protected void showRouteOverview() {
        currentlyRouting = false;
        drawRoute();
        getMapActivity().setOverview(destination, Util.formatDistance(route.getEstimatedDistance()), route.getViaStreets());
        zoomToBoundingBox();
    }

    public void zoomToBoundingBox() {
        final BoundingBoxE6 boundingBox = BoundingBoxE6.fromLocations(route.getWaypoints(), COORDINATE_PADDING);
        ViewTreeObserver vto = mapView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            boolean ran = false; // in case removeGlobalOnLayoutListener()
                                 // doesn't work (happens on some systems)

            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                if (!ran) {
                    mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mapView.zoomToBoundingBox(boundingBox);
                }
                ran = true;
            }
        });
    }

    public void startRouting() {
        locationOverlay.enableMyLocation(new GpsMyLocationProvider(getActivity()));
        routingStarted = true;
        locationOverlay.setCustomIconCentered(R.drawable.direction_arrow, true);
        currentlyRouting = true;
        getMapActivity().reloadInstructions(route.getTurnInstructions(), true);
        resetZoom();
        rotateMap();
        setTrackingMode(true);
        if (lastAnimLoc != null) {
            animateMap(lastAnimLoc);
        } else if (SMLocationManager.getInstance().getLastKnownLocation() != null) {
            animateMap(SMLocationManager.getInstance().getLastKnownLocation());
        }
    }

    private void resetZoom() {
        mapView.getController().setZoom(IbikePreferences.DEFAULT_ZOOM_LEVEL);
        mapView.getOverlayManager().previousZoom = mapView.getOverlayManager().currentZoom;
        mapView.getOverlayManager().currentZoom = (int) IbikePreferences.DEFAULT_ZOOM_LEVEL;
        mapView.invalidate();
    }

    protected SMRouteNavigationActivity getMapActivity() {
        return (SMRouteNavigationActivity) getActivity();
    }

    public void drawPins() {
        GeoPoint start = new GeoPoint(startLocation);
        GeoPoint end = new GeoPoint(endLocation);
        OverlayItem startItem = new OverlayItem("StartItem", "This is start item", start);
        startItem.setMarkerHotspot(HotspotPlace.BOTTOM_CENTER);
        startItem.setMarker(getResources().getDrawable(R.drawable.marker_start));
        OverlayItem endItem = new OverlayItem("EndItem", "This is end item", end);
        endItem.setMarker(getResources().getDrawable(R.drawable.marker_finish));
        List<OverlayItem> markerList = new LinkedList<OverlayItem>();
        markerList.add(startItem);
        if (markerOverlay != null) {
            mapView.getOverlays().remove(markerOverlay);
        }
        markerOverlay = new ItemizedIconOverlay<OverlayItem>(getActivity(), markerList, new OnItemGestureListener<OverlayItem>() {
            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                LOG.d("item tapped! " + item.mTitle);
                return true;
            }

            public boolean onItemLongPress(final int index, final OverlayItem item) {
                LOG.d("item long pressed!" + item.mTitle);
                return true;
            }
        });
        mapView.getOverlays().add(markerOverlay);

        List<OverlayItem> markerList2 = new LinkedList<OverlayItem>();
        markerList2.add(endItem);
        if (markerOverlayB != null) {
            mapView.getOverlays().remove(markerOverlayB);
        }
        markerOverlayB = new ItemizedIconOverlay<OverlayItem>(getActivity(), markerList2, new OnItemGestureListener<OverlayItem>() {
            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                LOG.d("item tapped! " + item.mTitle);
                return true;
            }

            public boolean onItemLongPress(final int index, final OverlayItem item) {
                LOG.d("item long pressed!" + item.mTitle);
                return true;
            }
        });
        mapView.getOverlays().add(markerOverlayB);
        if (route != null && route.startStation != null & route.endStation != null) {
            GeoPoint startStationPoint = new GeoPoint(route.startStation);
            GeoPoint endStationPoint = new GeoPoint(route.endStation);
            OverlayItem startStationItem = new OverlayItem("StartStationItem", "This is start item", startStationPoint);
            startStationItem.setMarkerHotspot(HotspotPlace.BOTTOM_CENTER);
            startStationItem.setMarker(getResources().getDrawable(R.drawable.route_metro));
            OverlayItem endStationItem = new OverlayItem("EndStationItem", "This is end item", endStationPoint);
            endStationItem.setMarker(getResources().getDrawable(R.drawable.route_metro));
            List<OverlayItem> markerStationList = new LinkedList<OverlayItem>();
            markerStationList.add(startStationItem);
            if (markerStationOverlay != null) {
                mapView.getOverlays().remove(markerStationOverlay);
            }
            markerStationOverlay = new ItemizedIconOverlay<OverlayItem>(getActivity(), markerStationList, new OnItemGestureListener<OverlayItem>() {
                public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                    LOG.d("item tapped! " + item.mTitle);
                    return true;
                }

                public boolean onItemLongPress(final int index, final OverlayItem item) {
                    LOG.d("item long pressed!" + item.mTitle);
                    return true;
                }
            });
            mapView.getOverlays().add(markerStationOverlay);

            List<OverlayItem> markerStationList2 = new LinkedList<OverlayItem>();
            markerStationList2.add(endStationItem);
            if (markerStationOverlayB != null) {
                mapView.getOverlays().remove(markerStationOverlayB);
            }
            markerStationOverlayB = new ItemizedIconOverlay<OverlayItem>(getActivity(), markerStationList2, new OnItemGestureListener<OverlayItem>() {
                public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                    LOG.d("item tapped! " + item.mTitle);
                    return true;
                }

                public boolean onItemLongPress(final int index, final OverlayItem item) {
                    LOG.d("item long pressed!" + item.mTitle);
                    return true;
                }
            });
            mapView.getOverlays().add(markerStationOverlayB);
        }

    }

    public void drawRoute() {
        if (route == null || route.getWaypoints() == null)
            return;
        mapView.getOverlays().remove(pathOverlay);
        if (pathOverlay2 != null) {
            mapView.getOverlays().remove(pathOverlay2);
        }
        pathOverlay = new PathOverlay(getRouteColor(), getActivity());
        pathOverlay.setAlpha(currentPathAlpha);
        pathOverlay.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH));

        if (route.endStation != null && route.station1 != null && route.station2 != null) {
            // broken route
            int i = 0;
            for (Location loc : route.waypoints) {
                if (i++ > route.waypointStation1)
                    break;
                pathOverlay.addPoint(new GeoPoint(loc));
            }
            pathOverlay.addPoint(new GeoPoint(route.startStation));
            mapView.getOverlays().add(pathOverlay);

            pathOverlay2 = new PathOverlay(getRouteColor(), getActivity());
            pathOverlay2.setAlpha(currentPathAlpha);
            pathOverlay2.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH));
            pathOverlay2.addPoint(new GeoPoint(route.endStation));
            i = 0;
            for (Location loc : route.waypoints) {
                if (i++ < route.waypointStation2)
                    continue;
                pathOverlay2.addPoint(new GeoPoint(loc));
            }
            mapView.getOverlays().add(pathOverlay2);

        } else {
            for (Location loc : route.getWaypoints()) {
                pathOverlay.addPoint(new GeoPoint(loc));
            }
            mapView.getOverlays().add(pathOverlay);
        }

    }

    public void redrawRoute() {
        if (route == null || route.getWaypoints() == null || getActivity() == null) {
            return;
        }
        mapView.getOverlays().remove(pathOverlay);
        if (pathOverlay2 != null) {
            mapView.getOverlays().remove(pathOverlay2);
        }
        pathOverlay = new PathOverlay(getRouteColor(), getActivity());
        pathOverlay.setAlpha(currentPathAlpha);
        pathOverlay.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH));
        if (route.isRouteBroken && route.endStation != null) {
            // broken route
            if (route.routePhase != SMRoute.TO_DESTINATION) {
                int i = 0;
                for (Location loc : route.waypoints) {
                    if (i++ > route.waypointStation1)
                        break;
                    pathOverlay.addPoint(new GeoPoint(loc));
                }
            }
            pathOverlay.addPoint(new GeoPoint(route.startStation));
            mapView.getOverlays().add(pathOverlay);
            pathOverlay2 = new PathOverlay(getRouteColor(), getActivity());
            pathOverlay2.setAlpha(currentPathAlpha);
            pathOverlay2.getPaint().setStrokeWidth(Util.dp2px(IbikePreferences.ROUTE_STROKE_WIDTH));
            pathOverlay2.addPoint(new GeoPoint(route.endStation));
            int i = 0;
            for (Location loc : route.waypoints) {
                if (i++ < route.waypointStation2 && route.routePhase != SMRoute.TO_DESTINATION)
                    continue;
                pathOverlay2.addPoint(new GeoPoint(loc));
            }
            mapView.getOverlays().add(pathOverlay2);
        } else {
            for (Location loc : route.getWaypoints()) {
                pathOverlay.addPoint(new GeoPoint(loc));
            }
            mapView.getOverlays().add(pathOverlay);
        }
        drawPins();
    }

    // SMLocationListener callback:

    @Override
    public void onLocationChanged(Location location) {
        lastAnimLoc = location;
        if (currentlyRouting && route != null && location != null) {
            route.visitLocation(location);
            
            try {
                getMapActivity().updateTime(getEstimatedArrivalTime());
            } catch (Exception e) {

            }
        }
        mapView.lastLocation = location;
    }

    Location lastAnimLoc;

    public void animateMap(Location location) {
        if (getTrackingMode() && !noTracking) {

            int orientation = (int) route.lastCorrectedHeading;
            if (orientation < 0)
                orientation += 360;

            if (!mapView.directionShown || !routingStarted) { // || locationOverlay.isTooFarFromRoute
                orientation = 0;
            }
            int instructionsViewHeight = Util.dp2px(70);
            if (!mapView.directionShown && ((SMRouteNavigationActivity) getMapActivity()).instructionsViewState == InstrcutionViewState.Normal) {
                mapView.locationArrowOffsset = -instructionsViewHeight;
            } else if (((SMRouteNavigationActivity) getMapActivity()).instructionsViewState == InstrcutionViewState.Normal) {
                mapView.locationArrowOffsset = 0;
                mapView.locationArrowOffsset -= instructionsViewHeight;
                mapView.locationArrowOffsset += ((Util.getScreenHeight() - instructionsViewHeight) / 6);
            } else {
                mapView.locationArrowOffsset = (int) (Util.getScreenHeight() / 6);
            }

            if (location != null) {
                mapView.getController().animateToForTracking(location.getLatitude(), location.getLongitude(), Math.toRadians(orientation),
                        mapView.locationArrowOffsset, routingStarted);
            }
        }
    }

    // SMRouteListener callbacks:

    @Override
    public void updateTurn(boolean firstElementRemoved) {
        LOG.d("updateTurn() next turn: "
                + (route.getTurnInstructions().size() > 0 ? route.getTurnInstructions().get(0).fullDescriptionString : "null"));
        if (currentlyRouting) {
            getMapActivity().reloadInstructions(route.getTurnInstructions(), firstElementRemoved);
        } else {
            LOG.d("not updating turn because currentlyRouting is false");
        }

    }

    @Override
    public void reachedDestination() {
        LOG.d("reachedDestination()");
        currentlyRouting = false;
        (new DB(getActivity())).saveFinishedRoute(startLocation, endLocation, startName, endName, new SimpleDateFormat(
                "E', 'd' 'MMM' 'y' 'k':'m':'s' 'z' 'Z", Locale.US).format(new Date(startLocation.getTime())), new SimpleDateFormat(
                "E', 'd' 'MMM' 'y' 'k':'m':'s' 'z' 'Z", Locale.US).format(new Date(endLocation.getTime())), route.visitedLocations.toString());//
        mapView.getOverlays().remove(pathOverlay);
        getMapActivity().showRouteFinishedDialog();
        mapView.directionShown = false;
        rotateMap();
    }

    @Override
    public void startRoute() {
        if (currentlyRouting) {
            return;
        }
        getMapActivity().setInstructionViewState(InstrcutionViewState.Normal);
        getMapActivity().hideOverview();
        drawRoute();
        startRouting();
    }

    @Override
    public void routeNotFound() {
        currentlyRouting = false;
        getMapActivity().setInstructionViewState(InstrcutionViewState.Invisible);
        Util.showSimpleMessageDlg(getActivity(), IbikeApplication.getString("error_route_not_found"));
    }

    @Override
    public void serverError() {
        routeColor = IbikePreferences.ROUTE_COLOR;
        Util.launchNoConnectionDialog(getActivity());
        getMapActivity().hideProgressBar();
        route.recalculationInProgress = false;
        redrawRoute();
    }

    @Override
    public void routeRecalculationStarted() {
        routeColor = IbikePreferences.ROUTE_DIMMED_COLOR;
        redrawRoute();
        if (getMapActivity() != null) {
            getMapActivity().showProgressBar();
        }
        locationOverlay.isTooFarFromRoute = true;
        rotateMap();
        if (mapView.tracking && lastAnimLoc != null) {
            animateMap(lastAnimLoc);
        }
        LOG.d("Route recalculation started");
    }

    @Override
    public void routeRecalculationDone() {
        routeColor = IbikePreferences.ROUTE_COLOR;
        if (getMapActivity() != null) {
            getMapActivity().updateTime(getEstimatedArrivalTime());
            getMapActivity().hideProgressBar();
            locationOverlay.isTooFarFromRoute = false;
            isRecalculation = true;
            if (!((SMRouteNavigationActivity) getMapActivity()).bicycleTypeChanged) {
                rotateMap();
            }
            if (mapView.tracking && lastAnimLoc != null && !((SMRouteNavigationActivity) getMapActivity()).bicycleTypeChanged) {
                animateMap(lastAnimLoc);
            }
        }
    }

    @Override
    public void updateRoute() {
        if (isRecalculation) {
            redrawRoute();
        }
        if (getMapActivity() != null) {
            getMapActivity().reloadInstructions(route.getTurnInstructions(), isRecalculation);
            isRecalculation = false;
            if (((SMRouteNavigationActivity) getMapActivity()).bicycleTypeChanged) {
                ((SMRouteNavigationActivity) getMapActivity()).onNewBicycleRoute();
            }
        }
    }

    // MapListener callbacks:

    boolean noTracking = false;

    @Override
    public boolean onScroll(final ScrollEvent event) {
        if (event.getUserAction() && mapView.tracking && !(mapView.isPinchZooming || System.currentTimeMillis() - mapView.lastPinchTimestamp < 200)) {
            getMapActivity().stopTrackingUser();

        }
        return false;
    }

    @Override
    public boolean onZoom(ZoomEvent event) {
        mapView.getOverlayManager().previousZoom = mapView.getOverlayManager().currentZoom;
        mapView.getOverlayManager().currentZoom = (int) event.getZoomLevel();
        mapView.invalidate();
        return false;
    }

    public String getEstimatedArrivalTime() {
        return this.formatArrivalTime((int) route.getEstimatedArrivalTime());
    }

    public void animateTo(Location location) {
        mapView.getController().animateTo(new GeoPoint(location));
    }

    public void recalculateRoute() {
        route.recalculateRoute(route.getTurnInstructions().get(0).getLocation(), true);
    }

    public int getLastIcon() {
        if (mapView.directionShown && routingStarted) {
            return R.drawable.compass_tracking;
        } else {
            return R.drawable.icon_locate_me;
        }
    }

    public void switchTracking() {
        if (mapView.directionShown || !routingStarted) {
            locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
        } else {
            locationOverlay.setCustomIconCentered(R.drawable.direction_arrow, true);
        }
        if (routingStarted) {
            mapView.directionShown = !mapView.directionShown;
        }
        rotateMap();
        if (mapView.tracking && lastAnimLoc != null) {
            animateMap(lastAnimLoc);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void rotateMap() {
        if (mapView.directionShown && currentlyRouting && route != null && route.getWaypoints() != null && route.getWaypoints().size() > 1
                && route.lastVisitedWaypointIndex >= 0) { // && !locationOverlay.isTooFarFromRoute mapView.tracking &&
            final float newBearing = currentlyRouting ? -(float) route.lastCorrectedHeading : 0;
            if (mapView.getMapOrientation() == newBearing) {
                return;
            }
            if (-mapView.getMapOrientation() < 0) {
                mapView.getOverlayManager().previousBearing = 360 - mapView.getMapOrientation();
            } else {
                mapView.getOverlayManager().previousBearing = -mapView.getMapOrientation();
            }
            if (-newBearing < 0) {
                mapView.getOverlayManager().currentBearing = 360 - newBearing;
            } else {
                mapView.getOverlayManager().currentBearing = -newBearing;
            }
            locationOverlay.mapOrientation = newBearing;
            mapView.setMapOrientation(newBearing);
        } else {
            mapView.getOverlayManager().currentBearing = 0;
            locationOverlay.mapOrientation = 0;
            mapView.setMapOrientation(0);
            locationOverlay.mapOrientation = 0;
        }
    }

    public void rotateMap(float orientation) {
        if (mapView.getMapOrientation() == orientation) {
            return;
        }
        if (-mapView.getMapOrientation() < 0) {
            mapView.getOverlayManager().previousBearing = 360 - mapView.getMapOrientation();
        } else {
            mapView.getOverlayManager().previousBearing = -mapView.getMapOrientation();
        }
        if (-orientation < 0) {
            mapView.getOverlayManager().currentBearing = 360 - orientation;
        } else {
            mapView.getOverlayManager().currentBearing = -orientation;
        }
        locationOverlay.mapOrientation = orientation;
        mapView.setMapOrientation(orientation);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == mAccelerometer) {
            accValues.put(event.values);
            // System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            if (Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]) > SensorManager.MAGNETIC_FIELD_EARTH_MAX * 1.5) {
                return;
            }
            if (event.values.length < 3) {
                return;
            }
            magValues.put(event.values);
            // System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }

        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, accValues.get(), magValues.get());
            // SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X,
            // SensorManager.AXIS_Z, remapMatrix);
            SensorManager.getOrientation(mR, mOrientation); // remapMatrx
            if (SMLocationManager.getInstance().hasValidLocation()) {
                Location currentLoc = SMLocationManager.getInstance().getLastValidLocation();
                float azimuth = -(float) Math.toDegrees(mOrientation[0]);
                final GeomagneticField geoField = new GeomagneticField(Double.valueOf(currentLoc.getLatitude()).floatValue(), Double.valueOf(
                        currentLoc.getLongitude()).floatValue(), Double.valueOf(currentLoc.getAltitude()).floatValue(), System.currentTimeMillis());
                azimuth += geoField.getDeclination(); // converts magnetic north
                // into true north
                // LOG.d("azimuth = " + azimuth);
                compassOrientation = azimuth;// - bearing;
                // if (Math.abs(locationOverlay.compassOrientation - compassOrientation) > 5) {
                // locationOverlay.compassOrientation = compassOrientation;
                // mapView.invalidate();
                // }
                // }
                // }
            }
        }
    }

    @Override
    public boolean onDoubleTapZoom() {
        // getMapActivity().stopTrackingUser();
        // locationOverlay.setCustomIconCentered(R.drawable.tracking_dot, false);
        if (mapView.tracking) {
            animateMap(mapView.lastLocation);
        }
        return true;
    }

    @Override
    public boolean onPinchZoom() {
        if (!mapView.tracking) {
            // getMapActivity().stopTrackingUser();
        }
        return false;
    }

    public void getRouteForNewBicycleType() {
        if (!route.getRouteForNewBicycleType(SMLocationManager.getInstance().getLastValidLocation())) {
            if (getActivity() != null) {
                getMapActivity().onNewBicycleRoute();
                Util.launchNoConnectionDialog(getMapActivity());
            }
        }
    }

}
