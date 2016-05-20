package com.spoiledmilk.ibikecph.map.states;

import com.spoiledmilk.ibikecph.map.MapActivity;

/**
 * The abstract MapState class supports transitions to and from states of the MapActivity.
 * Created by kraen on 02-05-16.
 */
public abstract class MapState {

    protected MapActivity activity;

    public void setMapActivity(MapActivity activity) {
        this.activity = activity;
    }

    /**
     * Transition the activity from some state to this state.
     * @param from
     */
    public abstract void transitionTowards(MapState from);

    /**
     * Transition the activity to some other state from this state.
     * @param to
     */
    public abstract void transitionAway(MapState to);

    /**
     * Called when the user presses the back button.
     */
    public abstract BackPressBehaviour onBackPressed();

    public enum BackPressBehaviour {
        PROPAGATE,
        STOP_PROPAGATION
    }
}
