package com.spoiledmilk.ibikecph.map.handlers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.IBCMapView;

/**
 * Created by jens on 5/29/15.
 */
public class OverviewMapHandler extends IBCMapHandler {
    private Marker curMarker;
    private UserLocationOverlay locationOverlay;

    @Override
    public void destructor() {
        // Remove the GPS overlay
        if (locationOverlay != null) {
            locationOverlay.disableFollowLocation();
            this.mapView.getOverlays().remove(locationOverlay);
        }
    }

    public static class Address {
        public String street;
        public String houseNumber;
        public String zip;
        public String city;
        public double lat;
        public double lon;

        public Address(String street, String houseNumber, String zip, String city, double lat, double lon) {
            this.street = street;
            this.houseNumber = houseNumber;
            this.zip = zip;
            this.city = city;
            this.lat = lat;
            this.lon = lon;
        }

        public String getStreetAddress() {
            return this.street + " " + this.houseNumber;
        }

        public String getPostCodeAndCity() {
            return this.zip + " " + this.city;
        }
    }

    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);

        addGPSOverlay();
    }

    public void addGPSOverlay() {
        GpsLocationProvider pr = new GpsLocationProvider(this.mapView.getContext());
        locationOverlay = new UserLocationOverlay(pr, this.mapView);

        locationOverlay.enableMyLocation();
        locationOverlay.setDrawAccuracyEnabled(true);
        locationOverlay.enableFollowLocation();
        locationOverlay.setPersonBitmap( BitmapFactory.decodeResource(this.mapView.getResources(), R.drawable.tracking_dot));
        this.mapView.getOverlays().add(locationOverlay);
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
    public void onLongPressMap(MapView _mapView, final ILatLng location) {
        final MapView mapView = _mapView;

        if (curMarker != null) {
            mapView.removeMarker(curMarker);
            curMarker = null;
        }

        Geocoder.getAddressForLocation(location, new Geocoder.GeocoderCallback() {
            @Override
            public void onSuccess(Address address) {
                Marker m = new Marker(address.getStreetAddress(), address.getPostCodeAndCity(), (LatLng) location);

                // Set a marker
                Bitmap bitmap = BitmapFactory.decodeResource(IbikeApplication.getContext().getResources(), R.drawable.location);
                Bitmap newImage = Bitmap.createBitmap(bitmap, 0, 0, 38, 38);
                Drawable d = new BitmapDrawable(IbikeApplication.getContext().getResources(), newImage);

                m.setImage(d);
                mapView.addMarker(m);

                // Invalidate the view so the marker gets drawn.
                mapView.invalidate();

                curMarker = m;
            }

            @Override
            public void onFailure() {

            }
        });

        Log.d("JC", "Long pressed the overview map");
    }
}
