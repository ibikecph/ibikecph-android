package com.spoiledmilk.ibikecph.map.fragments;

import android.app.Fragment;

import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.states.MapState;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;

/**
 * Any fragment intended for the map activity when using the map states.
 * Created by kraen on 20-05-16.
 */
public abstract class MapStateFragment extends Fragment {

    protected <S extends MapState> S getMapState(Class<S> stateClass) {
        if(getActivity() instanceof MapActivity) {
            MapActivity activity = (MapActivity) getActivity();
            if(stateClass.isInstance(activity.getState())) {
                return stateClass.cast(activity.getState());
            } else {
                throw new RuntimeException("Activity's state was not " + stateClass.getSimpleName());
            }
        } else {
            throw new RuntimeException("MapStateFragments must be attached to a MapActivity");
        }
    }
}
