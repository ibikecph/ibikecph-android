package com.spoiledmilk.ibikecph.map;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.handlers.IBCMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.TrackDisplayHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * This is the main class for maps in the I Bike CPH apps. It extends MapView from Mapbox but uses the tiles from the
 * City of Copenhagen tileservers. It serves some different purposes, including
 *   * Showing a regular map of the city
 *   * Helping in navigation
 *   * Showing past tracks
 * These different contexts have different demands for the map, so to accomodate this without cluttering it all together
 * in the same class, we implement some different implementations of the MapViewListener, that are (re-)assigned on
 * every state change of the map.
 */
public class IBCMapView extends MapView {

    private UserLocationOverlay userLocationOverlay;
    private Marker curAddressMarker;

    public enum MapState {
        DEFAULT,
        TRACK_DISPLAY,
        NAVIGATION_OVERVIEW
    }

    private MapState state = MapState.DEFAULT;
    private IBCMapHandler curHandler;
    private IBCMapActivity parentActivity;

    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase tileProvider, Handler tileRequestCompleteHandler, AttributeSet attrs) {
        super(aContext, tileSizePixels, tileProvider, tileRequestCompleteHandler, attrs);
    }
    public IBCMapView(Context aContext) {
        super(aContext);
    }
    public IBCMapView(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
    }
    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase aTileProvider) {
        super(aContext, tileSizePixels, aTileProvider);
    }

    /**
     * Do some initializations that are always needed
     */
    public void init(MapState initialState, IBCMapActivity parent) {
        WebSourceTileLayer ws = new WebSourceTileLayer("ibikecph", "http://tiles.ibikecph.dk/tiles/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(17);
        this.parentActivity = parent;

        this.setTileSource(ws);
        this.setCenter(new LatLng(Util.COPENHAGEN));
        this.setZoom(17);
        this.setMaxZoomLevel(19);

        //this.setMapRotationEnabled(true);
        changeState(initialState);

    }

    /**
     * Different map contexts have different listener behaviors. We assign the proper one based on the state variable.
     * This function should be called on every state change.
     */
    private void updateListeners() {
        // Ask the old handler to clean up
        if (curHandler != null) {
            curHandler.destructor();
        }

        // Figure out which one is going to be the new handler.
        switch (state) {
            case TRACK_DISPLAY:
                curHandler = new TrackDisplayHandler(this);
                break;
            case NAVIGATION_OVERVIEW:
                curHandler = new NavigationMapHandler(this);
                break;
            case DEFAULT:
            default:
                curHandler = new OverviewMapHandler(this);
                break;
        }

        // ... and apply it
        this.setMapViewListener(curHandler);
    }

    public void changeState(MapState newState) {
        state = newState;
        updateListeners();
    }

    /**
     * Handle long presses on the map.
     * @param position
     */
    @Override
    public void onLongPress(ILatLng position) {
        super.onLongPress(position);
        Log.d("JC", "Long pressed on map " + position.toString());
    }


    /**
     * Starts routing. This is a two-stage process, in which we first show the route to the user. Then they press "Go"
     * and we zoom to the first instruction.
     * @param route
     */
    public void showRoute(SMRoute route) {
        changeState(MapState.NAVIGATION_OVERVIEW);

        ((NavigationMapHandler) getMapHandler()).showRouteOverview(route);
    }

    public void showRoute(final Address a) {
        // First we need an SMRoute. Let's create one from the address
        Location curLoc = IbikeApplication.getService().getLastValidLocation();

        // If we don't have a fresh GPS coordinate, go with the best that we have.
        if (curLoc == null) {
            curLoc = IbikeApplication.getService().getLastKnownLocation();
        }

        // If we still don't have a fix, let the user know with a Toast
        if (curLoc == null) {
            Toast.makeText(IbikeApplication.getContext(), IbikeApplication.getString("error_no_gps_location"), Toast.LENGTH_LONG).show();
            return;
        }

        Geocoder.getRoute(new LatLng(curLoc), a.getLocation(), new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                route.endStationName = a.getStreetAddress();
                showRoute(route);
            }

            @Override
            public void onFailure() {

            }

        }, null);

    }

    public void stopRouting() {
        changeState(MapState.DEFAULT);
    }

    public IBCMapHandler getMapHandler() {
        return this.curHandler;
    }

    public Activity getParentActivity() {
        return parentActivity;
    }

    /**
     * Adds a GPS location dot.
     */
    public UserLocationOverlay addGPSOverlay() {
        GpsLocationProvider pr = new GpsLocationProvider(this.getContext());
        userLocationOverlay = new IBCUserLocationOverlay(pr, this);

        this.setUserLocationEnabled(true);

        userLocationOverlay.enableMyLocation();
        userLocationOverlay.setDrawAccuracyEnabled(true);
        userLocationOverlay.enableFollowLocation();
        userLocationOverlay.setPersonBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.tracking_dot));



        this.getOverlays().add(userLocationOverlay);
        this.invalidate();

        return userLocationOverlay;
    }

    /**
     * Removed the stored GPS overlay from the map view.
     */
    public void removeGPSOverlay() {
        if (userLocationOverlay != null) {
            userLocationOverlay.disableFollowLocation();
            this.getOverlays().remove(userLocationOverlay);
            this.invalidate();
        }

        userLocationOverlay = null;

    }

    public UserLocationOverlay getGPSOverlay() {
        return userLocationOverlay;
    }

    public void showAddressInfoPane(Address a) {
        // Show the infopane
        FragmentManager fm = this.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Prepare the infopane with the address we just got.
        AddressDisplayInfoPaneFragment adp = new AddressDisplayInfoPaneFragment();

        // Supply the address
        Bundle arguments = new Bundle();
        arguments.putSerializable("address", a);
        adp.setArguments(arguments);

        ft.replace(R.id.infoPaneContainer, adp);
        ft.commit();
    }

    public void showAddress(Address a) {
        showAddressInfoPane(a);

        removeAddressMarker();

        // Put a marker on the map
        Marker m = new Marker(a.getStreetAddress(), a.getPostCodeAndCity(), (LatLng) a.getLocation());
        m.setIcon(new Icon(this.getResources().getDrawable(R.drawable.marker_finish)));
        this.addMarker(m);

        this.curAddressMarker = m;

        // Invalidate the view so the marker gets drawn.
        this.invalidate();
    }


    public void removeAddressMarker() {
        if (curAddressMarker != null) {
            this.getOverlays().remove(curAddressMarker);
            this.removeMarker(curAddressMarker);
            curAddressMarker = null;
        }
        this.invalidate();
    }



}
