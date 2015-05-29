package com.spoiledmilk.ibikecph.map.listeners;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;

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
    public void onLongPressMap(MapView _mapView, ILatLng location) {
        MapView mapView = _mapView;

        // Geocode the location
        String title = "Title";
        String address = "Address";

        // Set a marker
        Marker m = new Marker(title, address, (LatLng) location);
        Bitmap bitmap = BitmapFactory.decodeResource(IbikeApplication.getContext().getResources(), R.drawable.location);
        Bitmap newImage = Bitmap.createBitmap(bitmap, 0, 0, 38, 38);
        Drawable d = new BitmapDrawable(IbikeApplication.getContext().getResources(), newImage);

        m.setImage(d);
        mapView.addMarker(m);

        // Invalidate the view so the marker gets drawn.
        mapView.invalidate();

        Log.d("JC", "Long pressed the overview map");
    }
}
