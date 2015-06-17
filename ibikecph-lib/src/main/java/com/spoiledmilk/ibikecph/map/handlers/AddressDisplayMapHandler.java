package com.spoiledmilk.ibikecph.map.handlers;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.map.IBCMapView;

/**
 * Created by jens on 6/17/15.
 */
public class AddressDisplayMapHandler extends IBCMapHandler {


    public AddressDisplayMapHandler(IBCMapView mapView) {
        super(mapView);
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

    }

    @Override
    public void onLongPressMap(MapView mapView, ILatLng iLatLng) {

    }
}
