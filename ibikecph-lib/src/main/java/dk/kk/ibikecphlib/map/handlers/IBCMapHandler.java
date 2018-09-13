package dk.kk.ibikecphlib.map.handlers;

import com.mapbox.mapboxsdk.views.MapViewListener;
import dk.kk.ibikecphlib.map.IBCMapView;

import dk.kk.ibikecphlib.map.IBCMapView;

/**
 * Created by jens on 6/1/15.
 */
public abstract class IBCMapHandler implements MapViewListener {

    protected IBCMapView mapView;

    public IBCMapHandler(IBCMapView mapView) {
        this.mapView = mapView;
    }

    public void destructor() {}

    public boolean onBackPressed() {
        return true;
    }

}
