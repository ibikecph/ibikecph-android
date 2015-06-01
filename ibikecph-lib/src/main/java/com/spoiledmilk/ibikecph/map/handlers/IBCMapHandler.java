package com.spoiledmilk.ibikecph.map.handlers;

import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spoiledmilk.ibikecph.map.IBCMapView;

/**
 * Created by jens on 6/1/15.
 */
public abstract class IBCMapHandler implements MapViewListener {

    protected IBCMapView mapView;

    public IBCMapHandler(IBCMapView mapView) {
        this.mapView = mapView;
    }

    public void destructor() {}

}
