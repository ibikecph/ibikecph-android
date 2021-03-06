package dk.kk.ibikecphlib.map.states;

import android.app.FragmentTransaction;

import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import dk.kk.ibikecphlib.map.handlers.IBCMapHandler;
import dk.kk.ibikecphlib.map.handlers.OverviewMapHandler;

/**
 * The initial state, which basically displays the users current location and allows for the user
 * to browse the map.
 * Created by kraen on 02-05-16.
 */
public class BrowsingState extends MapState {

    protected IBCMapHandler mapViewHandler;

    public BrowsingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        activity.getMapView().setUserLocationEnabled(true);
        activity.getMapView().getUserLocationOverlay().setDrawAccuracyEnabled(true);
        activity.getMapView().getUserLocationOverlay().enableFollowLocation();
        activity.getMapView().setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        activity.updateCompassIcon();

        mapViewHandler = new OverviewMapHandler(activity.getMapView());
        activity.getMapView().setMapViewListener(mapViewHandler);
    }

    @Override
    public void transitionAway(MapState to, FragmentTransaction fragmentTransaction) {
        activity.getMapView().setUserLocationEnabled(false);
        // TODO: Consider if we even need to destruct the map view at all.
        mapViewHandler.destructor();
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        return BackPressBehaviour.PROPAGATE;
    }
}
