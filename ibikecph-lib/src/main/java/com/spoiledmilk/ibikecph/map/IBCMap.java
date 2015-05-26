package com.spoiledmilk.ibikecph.map;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.AttributeSet;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.tileprovider.MapTileLayerBase;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * Created by jens on 5/26/15.
 */
public class IBCMap extends MapView {


    protected IBCMap(Context aContext, int tileSizePixels, MapTileLayerBase tileProvider, Handler tileRequestCompleteHandler, AttributeSet attrs) {
        super(aContext, tileSizePixels, tileProvider, tileRequestCompleteHandler, attrs);
        init();
    }

    public IBCMap(Context aContext) {
        super(aContext);
        init();
    }

    public IBCMap(Context aContext, AttributeSet attrs) {
        super(aContext, attrs);
        init();
    }

    protected IBCMap(Context aContext, int tileSizePixels, MapTileLayerBase aTileProvider) {
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

}
