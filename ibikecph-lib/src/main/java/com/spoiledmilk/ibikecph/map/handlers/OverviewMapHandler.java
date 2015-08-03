package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.tracking.TrackingInfoPaneFragment;

/**
 * Created by jens on 5/29/15.
 */
public class OverviewMapHandler extends IBCMapHandler {
    private Marker curMarker;
    boolean isWatchingAddress = false;
    private IBCMapView mapView;

    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);
        this.mapView = mapView;

        Log.d("JC", "Instantiating OverviewMapHandler");

        mapView.addGPSOverlay();
        showStatisticsInfoPane();

        View userTrackingButton = mapView.getParentActivity().findViewById(R.id.userTrackingButton);
        if (userTrackingButton != null) {
            userTrackingButton.setVisibility(View.VISIBLE);
        }

        for (Overlay o : mapView.getOverlays()) {
            Log.d("JC", "Overlay of type: " + o.getClass().getName());
        }
    }

    private void showStatisticsInfoPane() {
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, new TrackingInfoPaneFragment());
        ft.commit();

        isWatchingAddress = false;
    }

    @Override
    public void destructor() {
        Log.d("JC", "Destructing OverviewMapHandler");

        // Remove the marker if it's there.
        if (curMarker != null) {
            mapView.removeMarker(curMarker);
            curMarker = null;
        }

        /*
        View userTrackingButton = mapView.getParentActivity().findViewById(R.id.userTrackingButton);
        if (userTrackingButton != null) {
            userTrackingButton.setVisibility(View.GONE);
        }
        */


        mapView.removeGPSOverlay();
    }

    @Override
    public void onShowMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onHideMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMarker(MapView mapView, Marker marker) {
        this.mapView.removeAddressMarker();
        showStatisticsInfoPane();
    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {

    }

    @Override
    public void onLongPressMap(final MapView _mapView, final ILatLng location) {
        Log.d("JC", "OverviewMapHandler.onLongPressMap");

        Geocoder.getAddressForLocation(location, new Geocoder.GeocoderCallback() {
            @Override
            public void onSuccess(Address address) {
                // This refers to the FIELD, not the argument to the method (which I renamed to _mapView). This is
                // because we want it to be an IBCMapView.
                mapView.showAddress(address);
                isWatchingAddress = true;
            }

            @Override
            public void onFailure() {

            }
        });


    }

    /**
     * If the user presses the back button we should clean up and return the map in the default state.
     * @return
     */
    public boolean onBackPressed() {
        if (isWatchingAddress) {
            showStatisticsInfoPane();
            this.mapView.removeAddressMarker();

            isWatchingAddress = false;

            return false;
        } else {
            return true;
        }
    }
}
