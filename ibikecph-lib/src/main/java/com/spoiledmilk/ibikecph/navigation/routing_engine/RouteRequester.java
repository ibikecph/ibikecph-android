package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.os.AsyncTask;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.navigation.routing_engine.v5.Route;

/**
 * Created by kraen on 10-07-16.
 */
public abstract class RouteRequester extends AsyncTask<Void, Void, Boolean> {
    protected ILatLng start, end;
    protected Geocoder.RouteCallback callback;

    public RouteRequester(ILatLng start, ILatLng end, Geocoder.RouteCallback callback) {
        this.start = start;
        this.end = end;
        this.callback = callback;
    }
}
