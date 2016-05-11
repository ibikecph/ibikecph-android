package com.spoiledmilk.ibikecph.map.states;

import com.spoiledmilk.ibikecph.map.MapActivity;

/**
 * The user has selected a potential destination location.
 * Created by kraen on 02-05-16.
 */
public class DestinationSelectedState extends MapState {

    public DestinationSelectedState() {
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
