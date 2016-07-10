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
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.fragments.NavigationETAFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.TurnByTurnInstructionFragment;
import com.spoiledmilk.ibikecph.navigation.read_aloud.NavigationOracle;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState implements SMRouteListener, LocationListener, Serializable {

    protected SMRoute route;
    protected Journey journey;

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

    public void setJourney(Journey journey) {
        this.journey = journey;
        // Lock the map view to the users current location
        activity.getMapView()
                .getUserLocationOverlay()
                .setTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW_BEARING);
        // Zoom in on the current location
        activity.getMapView().setZoom(17f);
        // Show the Journey on the map view
        activity.getMapView().showJourney(journey);
        // Set the current route
        setRoute(journey.getRoutes().get(0));
        // Called to show the button from the action bar, that prompts the user to report problems
        activity.invalidateOptionsMenu();
        // Show the ETA and turn-by-turn fragments
        addFragments();
    }

    protected void setRoute(SMRoute route) {
        if(this.route != null) {
            this.route.removeListener(this);
            BikeLocationService.getInstance().removeLocationListener(this.route);
        }
        this.route = route;
        if(this.route != null) {
            this.route.addListener(this);
            BikeLocationService.getInstance().addLocationListener(this.route);
        }
    }

    public SMRoute getRoute() {
        return route;
    }

    public SMRoute getNextRoute() {
        int currentRouteIndex = journey.getRoutes().indexOf(route);
        if(currentRouteIndex < journey.getRoutes().size()-1) {
            return journey.getRoutes().get(currentRouteIndex+1);
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
    }

    @Override
    public void reachedDestination() {
        SMRoute nextRoute = getNextRoute();
        if(nextRoute == null) {
            // We are done with the journey
            turnByTurnFragment.reachedDestination();
        } else {
            // Switch to the next route in the journey
            setRoute(nextRoute);
        }
        if(navigationOracle != null) {
            navigationOracle.reachedDestination();
        }
    }

    @Override
    public void updateRoute() {
        // When the route is updated, the fragments should re-render, if added to an activity.
        if(turnByTurnFragment != null && turnByTurnFragment.isAdded()) {
            turnByTurnFragment.render();
        }
        if(navigationETAFragment != null && navigationETAFragment.isAdded()) {
            navigationETAFragment.render();
        }
        if(navigationOracle != null) {
            navigationOracle.updateRoute();
        }
    }

    @Override
    public void startRoute() {
        if(navigationOracle != null) {
            navigationOracle.startRoute();
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
    public void routeRecalculationDone() {
        if(navigationOracle != null) {
            navigationOracle.routeRecalculationDone();
        }
    }

    @Override
    public void serverError() {
        if(navigationOracle != null) {
            navigationOracle.serverError();
        }
        Toast.makeText(activity, IBikeApplication.getString("error_route_not_found"), Toast.LENGTH_SHORT).show();
    }

    public Journey getJourney() {
        return journey;
    }

    public void reportProblem() {
        Intent i = new Intent(activity, IssuesActivity.class);
        ArrayList<String> turnsArray = new ArrayList<>();
        for (SMTurnInstruction instruction : route.getUpcomingTurnInstructions()) {
            turnsArray.add(instruction.fullDescriptionString);
        }
        i.putStringArrayListExtra("turns", turnsArray);
        i.putExtra("startLoc", route.getStartLocation().toString());
        i.putExtra("endLoc", route.getEndLocation().toString());
        i.putExtra("startName", route.startStationName);
        i.putExtra("endName", route.endStationName);
        activity.startActivity(i);
    }
}
