package dk.kk.ibikecphlib.map.handlers;

import android.util.Log;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.views.MapView;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.map.IBCMapView;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.states.BrowsingState;
import dk.kk.ibikecphlib.map.states.DestinationPreviewState;
import dk.kk.ibikecphlib.util.IBikePreferences;


/**
 * Created by jens on 5/29/15.
 */
public class OverviewMapHandler extends IBCMapHandler {
    private Marker curMarker;
    public static boolean isWatchingAddress = false;
    private IBCMapView mapView;
    private IBikePreferences settings;

    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);
        this.mapView = mapView;
        settings = IBikeApplication.getSettings();
    }


    @Override
    public void destructor() {
        Log.d("JC", "Destructing OverviewMapHandler");

        // Remove the marker if it's there.
        if (curMarker != null) {
            mapView.removeMarker(curMarker);
            curMarker = null;
        }

        mapView.removeUserLocationOverlay();
    }

    @Override
    public void onShowMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onHideMarker(MapView mapView, Marker marker) {

    }

    @Override
    public void onTapMarker(MapView mapView, Marker marker) {
        /*
        There is a bug so that the marker can't be removed on click. Check out this gitissue
        https://github.com/mapbox/mapbox-android-sdk/issues/567
         */
    }

    @Override
    public void onLongPressMarker(MapView mapView, Marker marker) {
    }

    @Override
    public void onTapMap(MapView mapView, ILatLng iLatLng) {
        if(this.mapView.getParentActivity() instanceof MapActivity) {
            MapActivity activity = (MapActivity) this.mapView.getParentActivity();
            // Change state to the browsing state.
            if(!(activity.getState() instanceof BrowsingState)) {
                activity.changeState(BrowsingState.class);
            }
        }
    }

    @Override
    public void onLongPressMap(final MapView _mapView, final ILatLng location) {
        if(mapView.getParentActivity() instanceof MapActivity) {
            MapActivity activity = (MapActivity) mapView.getParentActivity();
            DestinationPreviewState state = (DestinationPreviewState) activity.changeState(DestinationPreviewState.class);
            state.setDestination(location);
        }
    }

    /**
     * If the user presses the back button we should clean up and return the map in the default state.
     *
     * @return
     */
    public boolean onBackPressed() {
        if (isWatchingAddress) {
            this.mapView.removeDestinationPreviewMarker();

            isWatchingAddress = false;

            return false;
        } else {
            return true;
        }
    }
}
