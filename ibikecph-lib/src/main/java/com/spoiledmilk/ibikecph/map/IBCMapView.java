package com.spoiledmilk.ibikecph.map;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.listeners.OverviewMapListener;
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
    public enum MapState {
        DEFAULT,
        TRACK_DISPLAY
    }

    private MapState state = MapState.DEFAULT;

    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase tileProvider, Handler tileRequestCompleteHandler, AttributeSet attrs) {
        super(aContext, tileSizePixels, tileProvider, tileRequestCompleteHandler, attrs);
        init();
    }

    public IBCMapView(Context aContext) {
        super(aContext);
        init();
    }

    public IBCMapView(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
        init();
    }

    protected IBCMapView(Context aContext, int tileSizePixels, MapTileLayerBase aTileProvider) {
        super(aContext, tileSizePixels, aTileProvider);
        init();
    }

    /**
     * Do some initializations that are always needed
     */
    public void init() {
        WebSourceTileLayer ws = new WebSourceTileLayer("ibikecph", "http://tiles.ibikecph.dk/tiles/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(17);

        this.setTileSource(ws);
        this.setCenter(new LatLng(Util.COPENHAGEN));
        this.setZoom(17);

        this.setMapRotationEnabled(true);


        updateListeners();


    }

    /**
     * Different map contexts have different listener behaviors. We assign the proper one based on the state variable.
     */
    private void updateListeners() {
        switch (state) {
            case DEFAULT:
                this.setMapViewListener(new OverviewMapListener());
                break;
        }
    }

    public void addGPSOverlay() {
        // Make a location overlay
        GpsLocationProvider pr = new GpsLocationProvider(this.getContext());
        UserLocationOverlay myLocationOverlay = new UserLocationOverlay(pr, this);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setPersonBitmap( BitmapFactory.decodeResource(this.getResources(), R.drawable.tracking_dot));
        this.getOverlays().add(myLocationOverlay);
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

}
