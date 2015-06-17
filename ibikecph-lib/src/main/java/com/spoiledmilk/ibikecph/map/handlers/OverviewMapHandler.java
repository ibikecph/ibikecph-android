package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
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
    private UserLocationOverlay locationOverlay;


    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);

        Log.d("JC", "Instantiating OverviewMapHandler");

        addGPSOverlay();
        showStatisticsInfoPane();
    }

    private void showStatisticsInfoPane() {
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, new TrackingInfoPaneFragment());
        ft.commit();
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
    }

    public void addGPSOverlay() {
        GpsLocationProvider pr = new GpsLocationProvider(this.mapView.getContext());
        locationOverlay = new UserLocationOverlay(pr, this.mapView);

        locationOverlay.enableMyLocation();
        locationOverlay.setDrawAccuracyEnabled(true);
        locationOverlay.enableFollowLocation();
        locationOverlay.setPersonBitmap( BitmapFactory.decodeResource(this.mapView.getResources(), R.drawable.tracking_dot));
        this.mapView.getOverlays().add(locationOverlay);
        this.mapView.invalidate();
    }

    @Override
    public void destructor() {
        Log.d("JC", "Destructing OverviewMapHandler");

        // Remove the marker if it's there.
        if (curMarker != null) {
            mapView.removeMarker(curMarker);
            curMarker = null;
        }

        // Remove the GPS overlay
        if (locationOverlay != null) {
            locationOverlay.disableFollowLocation();
            this.mapView.getOverlays().remove(locationOverlay);
            this.mapView.invalidate();
            locationOverlay=null;
        }
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
                Bitmap bitmap = BitmapFactory.decodeResource(IbikeApplication.getContext().getResources(), R.drawable.marker_finish);
                Bitmap newImage = Bitmap.createBitmap(bitmap, 0, 0, 38, 38);
                Drawable d = new BitmapDrawable(IbikeApplication.getContext().getResources(), newImage);

                m.setImage(d);
                mapView.addMarker(m);

                // Invalidate the view so the marker gets drawn.
                mapView.invalidate();

                curMarker = m;

                // Center the map around the marker
                mapView.setCenter(location, true);

                showAddressInfoPane(address);
            }

            @Override
            public void onFailure() {

            }
        });
    }
}
