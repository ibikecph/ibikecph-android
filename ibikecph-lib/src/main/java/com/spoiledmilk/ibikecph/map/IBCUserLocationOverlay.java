package com.spoiledmilk.ibikecph.map;

import com.mapbox.mapboxsdk.overlay.GpsLocationProvider;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.mapbox.mapboxsdk.views.MapView;

/**
 * Created by jens on 7/10/15.
 */
public class IBCUserLocationOverlay extends UserLocationOverlay {
    public IBCUserLocationOverlay(GpsLocationProvider pr, IBCMapView ibcMapView) {
        super(pr, ibcMapView);
    }

    public void onDetach(MapView mapView) {
        super.onDetach(mapView);
    }
}
