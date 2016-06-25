package com.spoiledmilk.ibikecph.map.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

/**
 * Created by kraen on 21-06-16.
 */
public class BreakRouteSelectionFragment extends RouteSelectionFragment {

    @Override
    public void refreshView() {
        super.refreshView();

        SMRoute route = mapState.getRoute();

        // TODO: Refactor so deprecated static members are no longer used.
        // Set the distance label
        if (MapActivity.isBreakChosen && Geocoder.totalBikeDistance != null) {
            float distance = 0;
            float duration;
            long arrivalTime = 0;

            distance = Geocoder.totalBikeDistance.get(NavigationMapHandler.obsInt.getPageValue());
            duration = Geocoder.totalTime.get(NavigationMapHandler.obsInt.getPageValue());
            arrivalTime = Geocoder.arrivalTime.get(NavigationMapHandler.obsInt.getPageValue());
            sourceText.setText(IBikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
            destinationText.setText(DestinationPreviewFragment.name);

            arrivalTime = arrivalTime * 1000;
            etaText.setText(dateFormat.format(arrivalTime).toString());

            // TODO: Change the location of this utility function
            durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

            if (distance > 1000) {
                distance /= 1000;
                lengthText.setText(String.format("%.1f km", distance));
            } else {
                lengthText.setText(String.format("%d m", (int) distance));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        breakButton.setVisibility(View.VISIBLE);
        cargoButton.setVisibility(View.GONE);
        return v;
    }
}
