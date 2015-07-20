package com.spoiledmilk.ibikecph.map;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Marker;

/**
 * Created by jens on 7/20/15.
 */
public class IBCMarker extends Marker {

    private MarkerType type;

    public IBCMarker(String title, String description, LatLng latLng, MarkerType type) {
        super(title, description, latLng);
        this.type = type;
    }

    public MarkerType getType() {
        return type;
    }
}
