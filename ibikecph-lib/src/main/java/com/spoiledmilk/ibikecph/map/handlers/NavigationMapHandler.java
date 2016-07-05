package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentTransaction;
import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.ObservablePageInteger;
import com.spoiledmilk.ibikecph.map.fragments.RouteSelectionFragment;

import java.io.Serializable;

/**
 * Created by jens on 5/30/15.
 * @deprecated Because it's used for both route selection and navigation - use MapStates instead.
 */
public class NavigationMapHandler extends IBCMapHandler implements Serializable {

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static int routePos = 0; // Keep track of which routePiece we are currently tracking

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static ObservablePageInteger obsInt;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static boolean displayExtraField = false;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static boolean displayGetOffAt = false;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static boolean isPublic = false;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static String getOffAt = "";

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static String lastType = "";

    public NavigationMapHandler(IBCMapView mapView) {
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
        Log.d("JC", "NavigationMapHandler.onTapMap");

    }

    @Override
    public void onLongPressMap(MapView mapView, final ILatLng iLatLng) {
        Log.d("NavigationMapHandler", "onLongPressMap() called");
    }

    @Override
    public void destructor() {
        Log.d("NavigationMapHandler", "destructor() called");
    }
}
