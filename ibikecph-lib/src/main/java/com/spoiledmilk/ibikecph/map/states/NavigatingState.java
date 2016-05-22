package com.spoiledmilk.ibikecph.map.states;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.view.WindowManager;

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

    protected NavigationMapHandler mapHandler;

    protected Fragment turnByTurnFragment;
    protected Fragment navigationETAFragment;

    public NavigatingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        // Save the state we came from to be able to transition back.
        previousState = from;

        // The users location should be recorded and the map should rotate accordingly
        activity.getMapView().setUserLocationEnabled(true);
        UserLocationOverlay userLocationOverlay = activity.getMapView().getUserLocationOverlay();
        userLocationOverlay.enableFollowLocation();
        userLocationOverlay.setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        activity.getMapView().setMapViewListener(NavigationMapHandler.class);
        // Hang onto this for later use - transition it's behaviour to this class over time
        mapHandler = (NavigationMapHandler) activity.getMapView().getMapHandler();

        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void transitionAway(MapState to, FragmentTransaction fragmentTransaction) {
        activity.getMapView().setUserLocationEnabled(false);
        // Called to hide the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();

        if(turnByTurnFragment != null) {
            fragmentTransaction.remove(turnByTurnFragment);
        }
        if(navigationETAFragment != null) {
            fragmentTransaction.remove(navigationETAFragment);
        }
        // TODO: Remove the destructor call, when the handler has been refactored away.
        // mapHandler.destructor();

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        activity.changeState(previousState);
        return BackPressBehaviour.STOP_PROPAGATION;
    }

    /**
     * Adds the top and bottom fragments. Call this only when the route is available.
     */
    protected void addFragments() {
        // Create the fragments
        turnByTurnFragment = new TurnByTurnInstructionFragment();
        navigationETAFragment = new NavigationETAFragment();

        // Add the navigation map handler to the arguments
        Bundle b = new Bundle();
        b.putSerializable("NavigationMapHandler", mapHandler);
        turnByTurnFragment.setArguments(b);
        navigationETAFragment.setArguments(b);

        // Add the fragments to the activity
        activity.getFragmentManager().beginTransaction()
                .add(R.id.turnByTurnContainer, turnByTurnFragment, "TurnByTurnPane")
                .replace(R.id.topFragment, navigationETAFragment, "NavigationETAFragment")
                .commit();
    }

    public void setRoute(SMRoute route) {
        this.route = route;

        Location start = route.getWaypoints().get(0);
        activity.getMapView().setCenter(new LatLng(start), true);
        activity.getMapView().setZoom(17f);

        activity.getMapView()
                .getUserLocationOverlay()
                .setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        IBikeApplication.getService().addLocationListener(mapHandler);

        // FIXME: Remove the use of the handler and booleans like this.
        mapHandler.isRouting = true;

        // Called to show the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();
        // Make the MapView show the route.
        activity.getMapView().showRoute(route);
        // Show the ETA and turn-by-turn fragments
        addFragments();
    }
}
