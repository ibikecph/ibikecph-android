package com.spoiledmilk.ibikecph.map.states;

import com.spoiledmilk.ibikecph.map.MapActivity;

/**
 * The user is navigating a particular route from the current location towards a destination.
 * Created by kraen on 02-05-16.
 */
public class NavigatingState extends MapState {

    public NavigatingState() {
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
