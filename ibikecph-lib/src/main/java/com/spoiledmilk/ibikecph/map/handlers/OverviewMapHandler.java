package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Icon;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.AddressDisplayInfoPaneFragment;
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

    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);

        Log.d("JC", "Instantiating OverviewMapHandler");

        mapView.addGPSOverlay();
        showStatisticsInfoPane();
    }

    private void showStatisticsInfoPane() {
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, new TrackingInfoPaneFragment());
        ft.commit();

        isWatchingAddress = false;
    }

    private void showAddressInfoPane(Address a) {
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Prepare the infopane with the address we just got.
        AddressDisplayInfoPaneFragment adp = new AddressDisplayInfoPaneFragment();

        // Supply the address
        Bundle arguments = new Bundle();
        arguments.putSerializable("address", a);
        adp.setArguments(arguments);

        ft.replace(R.id.infoPaneContainer, adp);
        ft.commit();

        isWatchingAddress = true;
    }

    @Override
    public void destructor() {
        Log.d("JC", "Destructing OverviewMapHandler");

        // Remove the marker if it's there.
        if (curMarker != null) {
            mapView.removeMarker(curMarker);
            curMarker = null;
        }

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
        removeMarker();
        showStatisticsInfoPane();
    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {

    }

    public void removeMarker() {
        if (curMarker != null) {
            mapView.getOverlays().remove(curMarker);
            mapView.removeMarker(curMarker);
            curMarker = null;
        }
        mapView.invalidate();
    }

    @Override
    public void onLongPressMap(final MapView _mapView, final ILatLng location) {
        Log.d("JC", "OverviewMapHandler.onLongPressMap");
        final MapView mapView = _mapView;

        removeMarker();

        Geocoder.getAddressForLocation(location, new Geocoder.GeocoderCallback() {
            @Override
            public void onSuccess(Address address) {
                Marker m = new Marker(address.getStreetAddress(), address.getPostCodeAndCity(), (LatLng) location);

                // Set a marker
                m.setIcon(new Icon(mapView.getResources().getDrawable(R.drawable.marker_finish)));
                mapView.addMarker(m);

                // Invalidate the view so the marker gets drawn.
                mapView.invalidate();

                curMarker = m;

                showAddressInfoPane(address);
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
            removeMarker();

            isWatchingAddress = false;

            return false;
        } else {
            return true;
        }
    }
}
