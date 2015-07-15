package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 7/15/15.
 */
public class RouteETAFragment extends InfoPaneFragment {
    private NavigationMapHandler parent;
    private TextView durationText, lengthText, etaText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
        this.parent.setRouteETAFragment(this);
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.parent.setRouteETAFragment(this);
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.infopane_route_eta, container, false);

        lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        return v;
    }

    public void render() {
        int secondsToFinish = (int) this.parent.getRoute().getEstimatedArrivalTime();

        this.lengthText.setText(getFormattedDistance((int) this.parent.getRoute().getDistanceLeft()));

        // Set the duration label
        durationText.setText(TrackListAdapter.durationToFormattedTime(secondsToFinish));

        // Set the ETA label
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, secondsToFinish);
        Date arrivalTime = c.getTime();
        SimpleDateFormat dt = new SimpleDateFormat("HH:mm");
        etaText.setText(dt.format(arrivalTime));
    }

    public String getFormattedDistance(int distanceInMeters) {

        float distanceInKilometers = distanceInMeters / 1000;

        if (distanceInMeters < 20) {
            return distanceInMeters + " m";

        } else if (distanceInMeters < 1000) {
            // Round to nearest 50
            return roundToNearest50(distanceInMeters) + " m";

        } else if (distanceInMeters < 10000) {
            // Round to nearest tenth of a kilometer
            return String.format("%.1f km", distanceInKilometers);

        } else {
            // Round to nearest kilometer
            return Math.round(distanceInKilometers) + " km";
        }
    }

    // Adapted from:
    // http://stackoverflow.com/questions/25438203/how-to-round-to-nearest-50-or-100
    public int roundToNearest50(int x) {
        if (x%50 < 25) {
            return x - (x%50);
        }
        else if (x%50 > 25) {
            return x + (50 - (x%50));
        }
        else {
            return x + 25;
        }
    }

}
