package dk.kk.ibikecphlib.map.states;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;

import com.mapbox.mapboxsdk.geometry.LatLng;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.map.Geocoder;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.RouteType;
import dk.kk.ibikecphlib.map.fragments.BreakRouteSelectionFragment;
import dk.kk.ibikecphlib.map.fragments.RouteSelectionFragment;
import dk.kk.ibikecphlib.map.handlers.NavigationMapHandler;
import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteResponse;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.search.Address;

import java.util.ArrayList;
import java.util.List;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.map.Geocoder;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.RouteType;
import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteResponse;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.search.Address;

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
    protected Route route;

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
     * @param routeType the type of route
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
        final LatLng sourceLocation = source.getLocation();
        LatLng destinationLocation = destination.getLocation();

        Geocoder.RouteCallback routeCallback = new Geocoder.RouteCallback() {
            @Override
            public void onSuccess(Route route) {
                route.setStartAddress(source);
                route.setEndAddress(destination);
                setRoute(route);
            }

            @Override
            public void onSuccess(BreakRouteResponse breakRouteResponse) {
                // TODO: Make the Geocoder.getRoute accept Address as arguments instead of LatLng
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
     * Sets the route to be displayed and to be used, if navigation is started.
     * This overload should be used to display a broken route instead of a regular route.
     * @param route the route
     */
    public void setRoute(Route route) {
        this.route = route;
        activity.getMapView().showRoute(route);
        activity.getMapView().zoomToRoute(route);
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
        Route routeToNavigate = route;
        NavigatingState state = activity.changeState(NavigatingState.class);
        state.setRoute(routeToNavigate);
    }

    public Route getRoute() {
        return route;
    }
}
