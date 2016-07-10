package com.spoiledmilk.ibikecph.map.fragments;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.states.NavigatingState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 7/15/15.
 */
public class NavigationETAFragment extends MapStateFragment {
    private TextView durationText, lengthText, etaText;
    private TextView textAddress;

    protected SimpleDateFormat dateFormat;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (DateFormat.is24HourFormat(this.getActivity())) {
            dateFormat = new SimpleDateFormat("HH:mm");
        } else {
            dateFormat = new SimpleDateFormat("hh:mm a");
        }

        View v = inflater.inflate(R.layout.navigation_fragment, container, false);

        textAddress = (TextView) v.findViewById(R.id.textAddress);

        lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        render();
        return v;
    }

    public void render() {
        Journey journey = getMapState(NavigatingState.class).getJourney();
        if (journey == null) {
            return;
        } else {
            this.lengthText.setText(getFormattedDistance(Math.round(journey.getEstimatedDistanceLeft())));

            // Set the address text
            textAddress.setText(journey.getEndAddress().getDisplayName());

            // Set the duration label
            int durationLeft = journey.getEstimatedDurationLeft();
            durationText.setText(TrackListAdapter.durationToFormattedTime(durationLeft));

            etaText.setText(dateFormat.format(journey.getArrivalTime()));
        }
    }

    public String getFormattedDistance(float distanceInMeters) {
        if (distanceInMeters < 20) {
            return Math.round(distanceInMeters) + " m";
        } else if (distanceInMeters < 1000) {
            // Round to nearest 50
            return roundToNearest50(Math.round(distanceInMeters)) + " m";
        } else if (distanceInMeters < 10000) {
            // Round to nearest tenth of a kilometer
            return String.format(IBikeApplication.getLocale(), "%.1f km", distanceInMeters / 1000.0f);
        } else {
            // Round to nearest kilometer
            return Math.round(distanceInMeters / 1000.0f) + " km";
        }
    }

    // Adapted from:
    // http://stackoverflow.com/questions/25438203/how-to-round-to-nearest-50-or-100
    public int roundToNearest50(int x) {
        if (x % 50 < 25) {
            return x - (x % 50);
        } else if (x % 50 > 25) {
            return x + (50 - (x % 50));
        } else {
            return x + 25;
        }
    }

}
