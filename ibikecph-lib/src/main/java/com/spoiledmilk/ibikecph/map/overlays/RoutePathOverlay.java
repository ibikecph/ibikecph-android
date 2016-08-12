package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;

import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType;

/**
 * A PathOverlay making the draw method available for the RouteOverlay
 * Created by kraen on 03-07-16.
 */
public class RoutePathOverlay extends PathOverlay {

    enum Type {
        WALK,
        CYCLE
    }

    public RoutePathOverlay(Context context, TransportationType type) {
        super(getColor(context, type), 10);
    }

    protected static int getColor(Context context, TransportationType type) {
        if(type == TransportationType.BIKE) {
            return context.getResources().getColor(R.color.PrimaryColor);
        } else if(type == TransportationType.WALK) {
            return Color.GRAY;
        } else {
            return Color.GRAY;
        }
    }

    @Override
    protected void draw(Canvas canvas, MapView mapView, boolean shadow) {
        super.draw(canvas, mapView, shadow);
    }
}
