package com.spoiledmilk.ibikecph.map.states;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.NavigationState;
import com.spoiledmilk.ibikecph.navigation.NavigationStateListener;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.read_aloud.NavigationOracle;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Route;
import com.spoiledmilk.ibikecph.navigation.routing_engine.TurnInstruction;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState implements NavigationStateListener, LocationListener, Serializable {

    protected NavigationState navigationState;

    protected NavigationOracle navigationOracle;
    protected IBikePreferences preferences;

    protected MapState previousState;

    protected TurnByTurnInstructionFragment turnByTurnFragment;
    protected NavigationETAFragment navigationETAFragment;
    protected ImageButton readAloudButton;

    public NavigatingState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        // Save the state we came from to be able to transition back.
        previousState = from;

        navigationState = new NavigationState(this);
        navigationState.addListener(this);

        // The users location should be recorded and the map should rotate accordingly
        activity.getMapView().setUserLocationEnabled(true);
        UserLocationOverlay userLocationOverlay = activity.getMapView().getUserLocationOverlay();
        userLocationOverlay.enableFollowLocation();
        userLocationOverlay.setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);

        // Used to update the map orientation when navigating
        IBikeApplication.getService().addLocationListener(this);

        activity.getMapView().setMapViewListener(NavigationMapHandler.class);

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

        // No need for this state to receive updates on location changes
        IBikeApplication.getService().removeLocationListener(this);
        // Reset the orientation of the map
        activity.getMapView().setMapOrientation(0);
        // Let's no longer keep the screen on
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Hide the read aloud button
        activity.findViewById(R.id.readAloudButton).setVisibility(View.GONE);
        // If the navigation oracle has been created, let's disable it
        if(navigationOracle != null) {
            navigationOracle.disable();
        }
        // Remove this as a listener of the NavigationState.
        navigationState.removeListener(this);
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
        // Add the fragments to the activity
        activity.getFragmentManager().beginTransaction()
                .add(R.id.turnByTurnContainer, turnByTurnFragment, "TurnByTurnPane")
                .replace(R.id.topFragment, navigationETAFragment, "NavigationETAFragment")
                .commit();
    }

    public void setRoute(Route route) {
        // Lock the map view to the users current location
        activity.getMapView()
                .getUserLocationOverlay()
                .setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        // Zoom in on the current location
        activity.getMapView().setZoom(17f);
        // Show the Route on the map view
        activity.getMapView().showRoute(route);

        navigationState.setRoute(route);

        // Called to show the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();
        // Show the ETA and turn-by-turn fragments
        addFragments();
    }

    public Route getRoute() {
        if(navigationState != null) {
            return navigationState.getRoute();
        } else {
            return null;
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
            navigationOracle = new NavigationOracle(activity, this, new NavigationOracle.NavigationOracleListener() {
                @Override
                public void enabled() {
                    readAloudButton.setAlpha(1f);
                }

                @Override
                public void disabled() {
                    Log.d("NavigationState", "The Navigation Oracle got disabled");
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

    @Override
    public void onLocationChanged(Location location) {
        // TODO: Consider only doing this when a bearing is available.
        activity.getMapView().setMapOrientation(-1 * location.getBearing());
        if(navigationOracle != null && navigationOracle.isEnabled()) {
            navigationOracle.onLocationChanged(location);
        }
        if(turnByTurnFragment != null && turnByTurnFragment.isAdded()) {
            turnByTurnFragment.render();
        }
        if(navigationETAFragment != null && navigationETAFragment.isAdded()) {
            navigationETAFragment.render();
        }
    }

    @Override
    public void destinationReached() {
        Log.d("NavigatingState", "destinationReached called");
        // We are done with the journey
        turnByTurnFragment.reachedDestination();
        if(navigationOracle != null) {
            navigationOracle.destinationReached();
        }
    }

    @Override
    public void navigationStarted() {
        if(navigationOracle != null) {
            navigationOracle.navigationStarted();
        }
    }

    @Override
    public void routeNotFound() {
        if(navigationOracle != null) {
            navigationOracle.routeNotFound();
        }
    }

    @Override
    public void routeRecalculationStarted() {
        if(navigationOracle != null) {
            navigationOracle.routeRecalculationStarted();
        }
    }

    @Override
    public void routeRecalculationCompleted() {
        if(navigationOracle != null) {
            navigationOracle.routeRecalculationCompleted();
        }
    }

    @Override
    public void serverError() {
        if(navigationOracle != null) {
            navigationOracle.serverError();
        }
        Toast.makeText(activity, IBikeApplication.getString("error_route_not_found"), Toast.LENGTH_SHORT).show();
    }

    public void reportProblem() {
        Intent i = new Intent(activity, IssuesActivity.class);
        ArrayList<String> turnsArray = new ArrayList<>();
        for (TurnInstruction instruction: navigationState.getUpcomingSteps()) {
            turnsArray.add(instruction.toDisplayString());
        }
        Route route = getRoute();
        i.putStringArrayListExtra("turns", turnsArray);
        i.putExtra("startLoc", route.getStartLocation().toString());
        i.putExtra("endLoc", route.getEndLocation().toString());
        i.putExtra("startName", route.getStartAddress().getName());
        i.putExtra("endName", route.getEndAddress().getName());
        activity.startActivity(i);
    }

    public NavigationState getNavigationState() {
        return navigationState;
    }
}
