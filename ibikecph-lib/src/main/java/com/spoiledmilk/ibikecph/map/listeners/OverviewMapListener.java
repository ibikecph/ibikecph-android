package com.spoiledmilk.ibikecph.map.listeners;

import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spoiledmilk.ibikecph.map.IBCMapView;

/**
 * Created by jens on 5/29/15.
 */
public class OverviewMapListener implements MapViewListener {
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

    }

    @Override
    public void onLongPressMap(MapView _mapView, ILatLng iLatLng) {
        IBCMapView mapView = (IBCMapView) _mapView;

        Log.d("JC", "Long pressed the overview map");
    }
}
