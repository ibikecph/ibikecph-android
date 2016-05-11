package com.spoiledmilk.ibikecph.map.states;

import com.spoiledmilk.ibikecph.map.MapActivity;

/**
 * Showing an overview of the route on the map - here the departure and destination can be changed
 * and swapped.
 * Created by kraen on 02-05-16.
 */
public class RouteSelectionState extends MapState {

    public RouteSelectionState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void transitionAway(MapState to) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
