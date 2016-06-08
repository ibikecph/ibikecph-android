package com.spoiledmilk.ibikecph.map.states;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.fragments.RouteSelectionFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.Address;
import com.squareup.okhttp.Route;

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

    protected NavigationMapHandler mapHandler;

    protected RouteSelectionFragment routeSelectionFragment;
    private List<RouteCallback> routeCallbacks = new ArrayList<>();

    protected abstract class RouteCallback implements Geocoder.RouteCallback {
        boolean cancelled = false;
        public void cancel() {
            cancelled = true;
            routeCallbacks.remove(this);
        }
    }

    public RouteSelectionState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        mapHandler = new NavigationMapHandler(activity.getMapView());
        activity.getMapView().setMapViewListener(mapHandler);

        // Enabled the user location, so the compass can be clicked
        activity.getMapView().setUserLocationEnabled(true);
        activity.getMapView().getUserLocationOverlay().setDrawAccuracyEnabled(false);

        // Creating the route selection fragment
        routeSelectionFragment = new RouteSelectionFragment();

        // Add the navigation map handler to the arguments
        Bundle b = new Bundle();
        b.putSerializable("NavigationMapHandler", mapHandler);
        routeSelectionFragment.setArguments(b);

        // Add the fragment to the activity
        fragmentTransaction.add(R.id.topFragment, routeSelectionFragment);

        // If a destination is already provided - might be because we are returning to the state.
        if(destination != null) {
            fetchRoute();
        }
    }

    @Override
    public void transitionAway(MapState to, FragmentTransaction fragmentTransaction) {
        mapHandler.cleanUp();
        // No need for a user location overlay afterwards - the future state will enabled this.
        activity.getMapView().setUserLocationEnabled(false);
        fragmentTransaction.remove(routeSelectionFragment);
        routeSelectionFragment = null;
        cancelRequests();
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

    public void setType(RouteType routeType) {
        this.routeType = routeType;
        fetchRoute();
    }

    /**
     * Calculates a new route when either the source or destination address change
     */
    private void fetchRoute() {
        if(destination == null) {
            new RuntimeException("A route to nowhere - that doesn't make any sense");
        }
        if(source == null) {
            new RuntimeException("A route from nowhere - that doesn't make any sense");
        }
        LatLng sourceLocation = source.getLocation();
        LatLng destinationLocation = destination.getLocation();

        RouteCallback routeCallback = new RouteCallback() {
            @Override
            public void onSuccess(SMRoute route) {
                if(!cancelled) {
                    routeCallbacks.remove(this);

                    // TODO: This could probably be removed or moved to the geocoder.
                    route.startStationName = source.getStreetAddress();
                    route.endStationName = destination.getStreetAddress();

                    route.startAddress = source;
                    route.endAddress = destination;
                    setRoute(route);
                }
            }

            @Override
            public void onSuccess(boolean isBreak) {
                if(!cancelled) {
                    routeCallbacks.remove(this);
                    // TODO: Refactor the use of MapActivity.isBreakChosen - so it's no longer needed.
                    if (MapActivity.isBreakChosen) {
                        Log.d("DV_break", "NavigationMaphandler: Calling showRoute with breakRoute!");
                        activity.getMapView().showMultipleRoutes();
                    } else {
                        MapActivity.breakFrag.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure() {
                if(!cancelled) {
                    displayTryAgain();
                }
            }

        };
        routeCallbacks.add(routeCallback);

        Geocoder.getRoute(sourceLocation, destinationLocation, routeCallback, null, routeType);
    }

    protected void cancelRequests() {
        for(RouteCallback callback: routeCallbacks) {
            callback.cancel();
        }
    }

    protected void setRoute(SMRoute route) {
        this.route = route;
        routeSelectionFragment.refreshView();
        activity.getMapView().showRoute(route);
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

    public void flipRoute() {
        setSourceAndDestination(destination, source);
    }

    public void startNavigation() {
        NavigatingState state = (NavigatingState) activity.changeState(NavigatingState.class);
        state.setRoute(route);
    }

    public SMRoute getRoute() {
        return route;
    }
}
