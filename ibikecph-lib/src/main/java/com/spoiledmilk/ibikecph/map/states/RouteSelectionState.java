package com.spoiledmilk.ibikecph.map.states;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;

import com.mapbox.mapboxsdk.geometry.LatLng;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.fragments.BreakRouteSelectionFragment;
import com.spoiledmilk.ibikecph.map.fragments.RouteSelectionFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteResponse;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;

import java.util.ArrayList;
import java.util.List;

/**
 * Showing an overview of the route on the map - here the departure and destination can be changed
 * and swapped.
 * Created by kraen on 02-05-16.
 */
public class RouteSelectionState extends MapState {

    protected Address source = Address.fromCurLoc();
    protected Address destination;
    protected RouteType routeType = RouteType.FASTEST;

    // A representation of the route after it's been fetched from the geocoder
    protected SMRoute route;
    protected Journey journey;

    protected RouteSelectionFragment routeSelectionFragment;

    public interface RouteTypeChangeListener {
        void routeTypeChanged(RouteType newType);
    }
    private List<RouteTypeChangeListener> routeTypeChangeListeners = new ArrayList<>();

    public RouteSelectionState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        activity.getMapView().setMapViewListener(NavigationMapHandler.class);

        // Enabled the user location, so the compass can be clicked
        activity.getMapView().setUserLocationEnabled(true);
        activity.getMapView().getUserLocationOverlay().setDrawAccuracyEnabled(false);

        routeSelectionFragment = activity.createRouteSelectionFragment();
        // Add the route selection fragment as a route type change listener, so it can update when
        // the route type changes.
        addRouteTypeChangeListener(routeSelectionFragment);
        // Add the fragment to the activity
        fragmentTransaction.add(R.id.topFragment, routeSelectionFragment);

        // If a destination is already provided - might be because we are returning to the state.
        if(destination != null) {
            fetchRoute();
        }
    }

    @Override
    public void transitionAway(MapState to, FragmentTransaction fragmentTransaction) {
        // No need for a user location overlay afterwards - the future state will enabled this.
        activity.getMapView().setUserLocationEnabled(false);
        // Cancel any requests that will be resolved asynchronously.
        Geocoder.cancelRequests();
        // Remove the route selection fragment as a route type change listener.
        removeRouteTypeChangeListener(routeSelectionFragment);
        // Then remove the route selection fragment
        fragmentTransaction.remove(routeSelectionFragment);
        // Remove any journey overlays from the map view
        activity.getMapView().clear();
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        DestinationPreviewState state = activity.changeState(DestinationPreviewState.class);
        state.setDestination(destination);
        return BackPressBehaviour.STOP_PROPAGATION;
    }

    public void setDestination(Address destination) {
        this.destination = destination;
        // activity.getMapView().showRoute(source, destination);
        fetchRoute();
    }

    public void setDestination(LatLng destination) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setSource(Address source) {
        this.source = source;
        fetchRoute();
    }

    public void setSource(LatLng source) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void setSourceAndDestination(Address source, Address destination) {
        this.source = source;
        this.destination = destination;
        fetchRoute();
    }

    public RouteType getType() {
        return routeType;
    }

    /**
     * Set the route type
     * @param routeType
     */
    public void setType(RouteType routeType) {
        this.routeType = routeType;
        fetchRoute();
        notifyRouteTypeChangeListeners();
    }

    public void addRouteTypeChangeListener(RouteTypeChangeListener listener) {
        routeTypeChangeListeners.add(listener);
    }

    public void removeRouteTypeChangeListener(RouteTypeChangeListener listener) {
        if(routeTypeChangeListeners.contains(listener)) {
            routeTypeChangeListeners.remove(listener);
        }
    }

    protected void notifyRouteTypeChangeListeners() {
        for(RouteTypeChangeListener listener: routeTypeChangeListeners) {
            listener.routeTypeChanged(routeType);
        }
    }

    /**
     * Flip around the source and destination addresses
     */
    public void flipRoute() {
        setSourceAndDestination(destination, source);
    }

    /**
     * Calculates a new route when either the source or destination address change
     */
    protected void fetchRoute() {
        if(destination == null) {
            new RuntimeException("A route to nowhere - that doesn't make any sense");
        }
        if(source == null) {
            new RuntimeException("A route from nowhere - that doesn't make any sense");
        }
        LatLng sourceLocation = source.getLocation();
        LatLng destinationLocation = destination.getLocation();

        Geocoder.RouteCallback routeCallback = new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                route.startAddress = source;
                route.endAddress = destination;
                // TODO: Make the route callback called with a Journey instead.
                setJourney(new Journey(route));
            }

            @Override
            public void onSuccess(BreakRouteResponse breakRouteResponse) {
                breakRouteResponse.setStartAddress(source);
                breakRouteResponse.setEndAddress(destination);
                if(routeSelectionFragment instanceof BreakRouteSelectionFragment) {
                    BreakRouteSelectionFragment fragment = ((BreakRouteSelectionFragment) routeSelectionFragment);
                    fragment.brokenRouteReady(breakRouteResponse);
                }
            }

            @Override
            public void onFailure() {
                displayTryAgain();
            }

        };
        // Cancel any pending requests, making sure they will not override the result of this
        Geocoder.cancelRequests();
        Geocoder.getRoute(sourceLocation, destinationLocation, routeCallback, routeType);
    }

    /**
     * Sets the journey to be displayed and to be used, if navigation is started.
     * This overload should be used to display a broken route instead of a regular route.
     * @param journey
     */
    public void setJourney(Journey journey) {
        this.journey = journey;
        activity.getMapView().showJourney(journey);
        activity.getMapView().zoomToJourney(journey);
        routeSelectionFragment.refreshView();
    }

    /**
     * If fetching the route fails, we might want to display a dialog asking the user if she would
     * like to try again.
     */
    protected void displayTryAgain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.mapActivityContext);
        String[] options = {IBikeApplication.getString("Cancel"), IBikeApplication.getString("Try_again")};
        builder.setTitle(IBikeApplication.getString("error_route_not_found"))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            activity.changeState(BrowsingState.class);
                        } else {
                            fetchRoute();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                activity.changeState(DestinationPreviewState.class);
            }
        });
        dialog.show();
    }

    public void startNavigation() {
        // Using a local variable here, as transitioning away will reset the route on the state.
        Journey journeyToNavigate = null;
        if(route != null) {
            journeyToNavigate = new Journey(route);
        } else if(journey != null) {
            journeyToNavigate = journey;
        }
        NavigatingState state = activity.changeState(NavigatingState.class);
        state.setJourney(journeyToNavigate);
    }

    public SMRoute getRoute() {
        return route;
    }

    public Journey getJourney() {
        return journey;
    }
}
