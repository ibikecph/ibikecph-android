package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;

import com.mapbox.mapboxsdk.overlay.ItemizedIconOverlay;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.map.IBCMarker;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kraen on 03-07-16.
 */
public class JourneyMarkerOverlay extends ItemizedIconOverlay {

    protected MapView mapView;

    public JourneyMarkerOverlay(MapView mapView) {
        super(mapView.getContext(), new ArrayList<Marker>(), new OnItemGestureListener<Marker>() {
            @Override
            public boolean onItemSingleTapUp(int index, Marker item) {
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, Marker item) {
                return false;
            }
        });
        this.mapView = mapView;
    }

    @Override
    protected void draw(Canvas c, MapView mapView, boolean shadow) {
        super.draw(c, mapView, shadow);
    }

    @Override
    public boolean addItem(Marker item) {
        boolean success = super.addItem(item);
        // We need to add this to the map view, for the marker to figure out it's position.
        item.addTo(mapView);
        return success;
    }
}
