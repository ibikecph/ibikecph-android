package com.spoiledmilk.ibikecph.map.states;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState {

    protected SMRoute route;

    protected MapState previousState;

    protected NavigationMapHandler handler;

    public NavigatingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        // Save the state we came from to be able to transition back.
        previousState = from;

        activity.getMapView().setUserLocationEnabled(true);
        UserLocationOverlay userLocationOverlay = activity.getMapView().getUserLocationOverlay();
        userLocationOverlay.enableFollowLocation();
        userLocationOverlay.setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        activity.getMapView().setMapViewListener(NavigationMapHandler.class);
        // Hang onto this for later use - transition it's behaviour to this class over time
        handler = (NavigationMapHandler) activity.getMapView().getMapHandler();
    }

    @Override
    public void transitionAway(MapState to) {
        activity.getMapView().setUserLocationEnabled(false);
        // Called to hide the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();
        // TODO: Remove the destructor call, when the handler no longer exists.
        handler.destructor();
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        activity.changeState(previousState);
        return BackPressBehaviour.STOP_PROPAGATION;
    }

    protected void showFragments() {
        TurnByTurnInstructionFragment turnByTurnFragment = new TurnByTurnInstructionFragment();
        NavigationETAFragment navigationETAFragment = new NavigationETAFragment();

        Bundle b = new Bundle();
        b.putSerializable("NavigationMapHandler", handler);
        turnByTurnFragment.setArguments(b);
        navigationETAFragment.setArguments(b);

        FragmentManager fm = activity.getMapView().getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(R.id.turnByTurnContainer, turnByTurnFragment, "TurnByTurnPane");
        ft.replace(R.id.topFragment, navigationETAFragment, "NavigationETAFragment");
        ft.commit();

        // TODO: Make sure other navigation states removes their fragments instead of hiding it
        activity.findViewById(R.id.topFragment).setVisibility(View.VISIBLE);
    }

    public void setRoute(SMRoute route) {
        this.route = route;

        Location start = route.getWaypoints().get(0);
        activity.getMapView().setCenter(new LatLng(start), true);
        activity.getMapView().setZoom(17f);

        activity.getMapView()
                .getUserLocationOverlay()
                .setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        IBikeApplication.getService().addLocationListener(handler);

        handler.isRouting = true;

        // Called to show the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();
        // Make the MapView show the route.
        activity.getMapView().showRoute(route);
        // Show the ETA and turn-by-turn fragments
        showFragments();
    }
}
