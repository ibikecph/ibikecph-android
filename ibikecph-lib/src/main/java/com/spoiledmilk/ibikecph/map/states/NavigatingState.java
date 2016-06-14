package com.spoiledmilk.ibikecph.map.states;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.read_aloud.NavigationOracle;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState {

    protected SMRoute route;

    protected NavigationOracle navigationOracle;
    protected IBikePreferences preferences;

    protected MapState previousState;

    protected NavigationMapHandler mapHandler;

    protected Fragment turnByTurnFragment;
    protected Fragment navigationETAFragment;
    protected ImageButton readAloudButton;

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

        // Show the read aloud button
        readAloudButton = (ImageButton) activity.findViewById(R.id.readAloudButton);
        readAloudButton.setVisibility(View.VISIBLE);

        if(activity.getApplication() instanceof IBikeApplication) {
            preferences = IBikeApplication.getSettings();
        } else {
            throw new RuntimeException("Expected the IBikeApplication");
        }

        readAloudUpdated(preferences.getReadAloud());
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
        mapHandler.destructor();

        // Let's no longer keep the screen on
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Hide the read aloud button
        activity.findViewById(R.id.readAloudButton).setVisibility(View.GONE);
        // If the navigation oracle has been created, let's disable it
        if(navigationOracle != null) {
            navigationOracle.disable();
        }
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
        if(route != null) {
            Location start = route.getWaypoints().get(0);
            activity.getMapView().setCenter(new LatLng(start), true);
            activity.getMapView().setZoom(17f);

            activity.getMapView()
                    .getUserLocationOverlay()
                    .setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

            IBikeApplication.getService().addLocationListener(mapHandler);

            // FIXME: Remove the use of the handler and static booleans like this.
            mapHandler.isRouting = true;

            // Called to show the button from the action bar, that prompts the user to report problems
            activity.invalidateOptionsMenu();
            // Make the MapView show the route.
            activity.getMapView().showRoute(route);
            // Show the ETA and turn-by-turn fragments
            addFragments();
        }

        if(navigationOracle != null) {
            navigationOracle.setRoute(route);
        }
    }

    /**
     * Set the read aloud feature on or off
     * @param readAloud true if the user should start hearing the navigation instructions read aloud
     */
    public void setReadAloud(boolean readAloud) {
        Log.d("NavigationState", "setReadAloud called with " + readAloud);
        preferences.setReadAloud(readAloud);
        readAloudUpdated(readAloud);
    }

    public boolean getReadAloud() {
        return preferences.getReadAloud();
    }

    public void toggleReadAloud() {
        this.setReadAloud(!getReadAloud());
    }

    public void readAloudUpdated(boolean readAloud) {
        Log.d("NavigationState", "readAloudUpdated called with " + readAloud);
        // Register or deregister a listener on the location (and eventually route)
        if(readAloud) {
            readAloudButton.setAlpha(0.66f);
            readAloudButton.setImageResource(R.drawable.read_aloud_enabled);
            navigationOracle = new NavigationOracle(activity, route, new NavigationOracle.NavigationOracleListener() {
                @Override
                public void enabled() {
                    IBikeApplication.getService().addLocationListener(navigationOracle);
                    readAloudButton.setAlpha(1f);
                }

                @Override
                public void disabled() {
                    Log.d("NavigationState", "The Navigation Oracle got disabled");
                    IBikeApplication.getService().removeLocationListener(navigationOracle);
                    navigationOracle = null;
                    readAloudUpdated(false);
                }

                @Override
                public void initError() {
                    String err = IBikeApplication.getString("read_aloud_error_initalizing");
                    Toast.makeText(activity, err, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void unsupportedLanguage() {
                    String err = IBikeApplication.getString("read_aloud_error_unsupported_language");
                    Toast.makeText(activity, err, Toast.LENGTH_SHORT).show();
                }
            });
        } else if(navigationOracle != null) {
            // Ask the navigation oracle to disable itself
            navigationOracle.disable();
        } else { // readAloud == false && navigationOracle == null
            readAloudButton.setAlpha(1f);
            readAloudButton.setImageResource(R.drawable.read_aloud_disabled);
        }
    }
}
