package com.spoiledmilk.ibikecph.map.states;

import android.view.View;

import com.mapbox.mapboxsdk.geometry.LatLng;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.search.Address;

/**
 * Showing an overview of the route on the map - here the departure and destination can be changed
 * and swapped.
 * Created by kraen on 02-05-16.
 */
public class RouteSelectionState extends MapState {

    Address source;
    Address destination;

    NavigationMapHandler mapHandler;

    public RouteSelectionState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        mapHandler = new NavigationMapHandler(activity.getMapView());
        activity.getMapView().setMapViewListener(mapHandler);
        activity.getMapView().showRoute(source, destination);
        activity.findViewById(R.id.infoPaneContainer).setVisibility(View.VISIBLE);
    }

    @Override
    public void transitionAway(MapState to) {
        mapHandler.cleanUp();
        activity.findViewById(R.id.infoPaneContainer).setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        DestinationPreviewState state = new DestinationPreviewState();
        activity.changeState(state);
        state.setDestination(destination);
    }

    public void setDestination(Address destination) {
        this.destination = destination;
    }

    public void setDestination(LatLng destination) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void setSource(Address source) {
        this.source = source;
    }

    public void setSource(LatLng source) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
