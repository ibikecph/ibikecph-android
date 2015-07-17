package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 6/1/15.
 */
public class NavigationOverviewInfoPane extends InfoPaneFragment implements View.OnClickListener {
    private NavigationMapHandler parent;
    private ImageButton fastButton, cargoButton, greenButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SMRoute route = ((NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler")).getRoute();

        View v = inflater.inflate(R.layout.infopane_navigation_overview, container, false);

        TextView sourceView = (TextView) v.findViewById(R.id.navigationOverviewSource);
        sourceView.setText(IbikeApplication.getString("current_position"));

        TextView addressView = (TextView) v.findViewById(R.id.navigationOverviewDestination);
        addressView.setText(route.endStationName);

        ImageButton goButton = (ImageButton) v.findViewById(R.id.navigationOverviewGoButton);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.goButtonClicked();
            }
        });

        fastButton = (ImageButton) v.findViewById(R.id.navigationOverviewFastButton);
        cargoButton = (ImageButton) v.findViewById(R.id.navigationOverviewCargoButton);
        greenButton = (ImageButton) v.findViewById(R.id.navigationOverviewGreenButton);

        fastButton.setOnClickListener(this);
        cargoButton.setOnClickListener(this);
        greenButton.setOnClickListener(this);

        TextView durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        TextView lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        TextView etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        // Set the distance label
        float distance = route.getEstimatedDistance();

        if (distance > 1000) {
            distance /= 1000;
            lengthText.setText(String.format("%.1f km", distance));
        } else {
            lengthText.setText(String.format("%d m", (int) distance));
        }

        // Set the duration label
        float duration = route.getEstimatedArrivalTime();
        durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

        // Set the ETA label
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, (int) duration);
        Date arrivalTime = c.getTime();
        SimpleDateFormat dt = new SimpleDateFormat("HH:mm");
        etaText.setText(dt.format(arrivalTime));


        // Add the ability to flip the route
        ((ImageButton) v.findViewById(R.id.btnAddressSwap)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.flipRoute();
            }
        });


        return v;
    }

    public void setParent(NavigationMapHandler parent) {
        this.parent = parent;
    }

    /**
     * This is the click handler for the fast/cargo/green route buttons
     * @param v
     */
    @Override
    public void onClick(View v) {
        disableAllRouteButtons();

        if (v.getId() == R.id.navigationOverviewFastButton)  {
            fastButton.setImageResource(R.drawable.btn_route_fastest_enabled);
        } else if (v.getId() == R.id.navigationOverviewCargoButton) {
            cargoButton.setImageResource(R.drawable.btn_route_cargo_enabled);
        } else {
            greenButton.setImageResource(R.drawable.btn_route_green_enabled);
        }
    }

    public void disableAllRouteButtons() {
        fastButton.setImageResource(R.drawable.btn_route_fastest_disabled);
        cargoButton.setImageResource(R.drawable.btn_route_cargo_disabled);
        greenButton.setImageResource(R.drawable.btn_route_green_disabled);
    }
}
