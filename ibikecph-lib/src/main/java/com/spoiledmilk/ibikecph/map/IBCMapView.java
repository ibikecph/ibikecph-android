package com.spoiledmilk.ibikecph.map;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.map.handlers.IBCMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.TrackDisplayHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.ArrayList;

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
    public enum MapState {
        DEFAULT,
        TRACK_DISPLAY,
        NAVIGATION_OVERVIEW
    }

    private MapState state = MapState.DEFAULT;
    private IBCMapHandler curHandler;
    private InfoPaneFragment infoPane = null;

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
    public void init(MapState initialState, InfoPaneFragment infoPane) {
        WebSourceTileLayer ws = new WebSourceTileLayer("ibikecph", "http://tiles.ibikecph.dk/tiles/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(19);

        this.setTileSource(ws);
        this.setCenter(new LatLng(Util.COPENHAGEN));
        this.setZoom(17);

        //this.setMapRotationEnabled(true);
        changeState(initialState);

        this.infoPane = infoPane;
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
            case DEFAULT:
                curHandler = new OverviewMapHandler(this);
                break;
            case TRACK_DISPLAY:
                curHandler = new TrackDisplayHandler(this);
                break;
            case NAVIGATION_OVERVIEW:
                curHandler = new NavigationMapHandler(this);
                break;
        }

        // ... and apply it
        this.setMapViewListener(curHandler);
    }

    private void changeState(MapState newState) {
        state = newState;
        updateListeners();
    }



    /**
     * Handle long presses on the map.
     * @param position
     */
    public void onLongPress(LatLng position) {
        Log.d("JC", "Long pressed on map " + position.toString());

        // We only do long presses on
        if (state != MapState.DEFAULT) return;

    }


    /**
     * Starts routing. This is a two-stage process, in which we first show the route to the user. Then they press "Go"
     * and we zoom to the first instruction.
     * @param route
     */
    public void startRouting(SMRoute route) {
        changeState(MapState.NAVIGATION_OVERVIEW);

        // TODO: Fix confusion between Location and LatLng objects
        PathOverlay path = new PathOverlay(Color.RED, 10);
        ArrayList<LatLng> waypoints = new ArrayList<LatLng>();
        for (Location loc : route.waypoints) {
            path.addPoint(loc.getLatitude(), loc.getLongitude());
            waypoints.add(new LatLng(loc));
        }

        // Get rid of old overlays
        this.getOverlays().clear();

        // Show the whole route, zooming to make it fit
        this.getOverlays().add(path);
        this.zoomToBoundingBox(BoundingBox.fromLatLngs(waypoints), true, true, true, true);
    }

    public Fragment getInfoPane() {
        return infoPane;
    }
}
