package com.spoiledmilk.ibikecph.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 7/15/15.
 */
public class RouteETAFragment extends InfoPaneFragment {
    private TextView durationText, lengthText, etaText;
    private ImageView imgRouteType;
    private TextView textAddress;
    private TextView textProblem;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler")).setRouteETAFragment(this);
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.infopane_navigation, container, false);

        imgRouteType = (ImageView) v.findViewById(R.id.imgRouteType);
        textAddress = (TextView) v.findViewById(R.id.textAddress);
        textProblem = (TextView) v.findViewById(R.id.textProblem);

        lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        render((NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler"));

        textProblem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onProblemButtonClicked(v);
            }
        });

        return v;
    }

    public void render(NavigationMapHandler parent) {
        // If the size=0, we've actually already arrived, but render() is called before NavigationMapHandler gets its
        // reachedDestination() callback from the SMROute. Blame somebody else...
        if (parent.getRoute().getTurnInstructions().size() == 0)  return;

        int secondsToFinish = (int) parent.getRoute().getEstimatedArrivalTime();

        this.lengthText.setText(getFormattedDistance((int) parent.getRoute().getDistanceLeft()));

        // Set the address text
        textAddress.setText(parent.getRoute().endStationName);

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
        float distanceInKilometers = ((float) distanceInMeters) / 1000;

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

    public void onProblemButtonClicked(View v) {
        Log.d("JC", "User clicked Problem button");

        Intent i = new Intent(getActivity(), IssuesActivity.class);

        ArrayList<String> turnsArray = new ArrayList<String>();
        for (SMTurnInstruction instruction : NavigationMapHandler.getRoute().getTurnInstructions()) {
            turnsArray.add(instruction.generateFullDescriptionString());
        }

        i.putStringArrayListExtra("turns", turnsArray);
        i.putExtra("startLoc", NavigationMapHandler.getRoute().getStartLocation().toString());
        i.putExtra("endLoc", NavigationMapHandler.getRoute().getEndLocation().toString());
        i.putExtra("startName", NavigationMapHandler.getRoute().startStationName);
        i.putExtra("endName", NavigationMapHandler.getRoute().endStationName);

        startActivity(i);
    }

}
