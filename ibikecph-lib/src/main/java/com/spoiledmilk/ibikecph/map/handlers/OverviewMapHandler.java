package com.spoiledmilk.ibikecph.map.handlers;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.states.BrowsingState;
import com.spoiledmilk.ibikecph.map.states.DestinationPreviewState;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.tracking.TrackingStatisticsFragment;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

/**
 * Created by jens on 5/29/15.
 */
public class OverviewMapHandler extends IBCMapHandler {
    private Marker curMarker;
    public static boolean isWatchingAddress = false;
    public static Address addressBeingWatched = null;
    private IBCMapView mapView;
    private IBikePreferences settings;

    public OverviewMapHandler(IBCMapView mapView) {
        super(mapView);
        this.mapView = mapView;
        settings = IBikeApplication.getSettings();

        for (Overlay o : mapView.getOverlays()) {
            Log.d("JC", "Overlay of type: " + o.getClass().getName());
        }
    }

    private void showStatisticsInfoPane() {
        MapActivity.topFragment.setVisibility(View.VISIBLE);
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.topFragment, new TrackingStatisticsFragment());
        ft.commit();

        isWatchingAddress = false;
    }


    private void disableStatisticsInfoPane() {
        MapActivity.topFragment.setVisibility(View.GONE);
        Log.d("DV", "Infopanefragment removed!");
        //OverviewMapHandler.isWatchingAddress = false;
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
            activity.changeState(BrowsingState.class);
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
            if (settings.getTrackingEnabled()) {
                showStatisticsInfoPane();
            } else {
                disableStatisticsInfoPane();
            }
            this.mapView.removeAddressMarker();

            isWatchingAddress = false;

            return false;
        } else {
            return true;
        }
    }
}
