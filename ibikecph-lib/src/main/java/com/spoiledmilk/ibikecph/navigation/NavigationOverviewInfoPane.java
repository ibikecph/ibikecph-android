package com.spoiledmilk.ibikecph.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 6/1/15.
 */
public class NavigationOverviewInfoPane extends InfoPaneFragment implements View.OnClickListener {
    private NavigationMapHandler parent;
    private ImageButton fastButton, cargoButton, greenButton, breakButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SMRoute route = ((NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler")).getRoute();

        View v = inflater.inflate(R.layout.infopane_navigation_overview, container, false);

        TextView sourceText = (TextView) v.findViewById(R.id.navigationOverviewSource);

        TextView destinationText = (TextView) v.findViewById(R.id.navigationOverviewDestination);

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

        float distance;
        float duration;
        long arrivalTime = 0;

        if (IbikeApplication.getAppName().equals("Cykelplanen")) {
            breakButton = (ImageButton) v.findViewById(R.id.navigationOverviewBreakButton);
            breakButton.setVisibility(View.VISIBLE);
            breakButton.setOnClickListener(this);

            cargoButton.setVisibility(View.GONE);

            // Set the distance label
            if (MapActivity.isBreakChosen && Geocoder.totalDistance != null) {
                distance = Geocoder.totalDistance.get(NavigationMapHandler.obsInt.getPageValue());
                duration = Geocoder.totalTime.get(NavigationMapHandler.obsInt.getPageValue());
                arrivalTime = Geocoder.arrivalTime.get(NavigationMapHandler.obsInt.getPageValue());
                //sourceText.setText(Geocoder.from.get(NavigationMapHandler.obsInt.getPageValue()));
                sourceText.setText(IbikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
                destinationText.setText(Geocoder.to.get(NavigationMapHandler.obsInt.getPageValue()));

            } else {
                distance = route.getEstimatedDistance();
                duration = route.getEstimatedArrivalTime();
                //sourceText.setText(route.startAddress.getStreetAddress());
                sourceText.setText(IbikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
                destinationText.setText(route.endAddress.getStreetAddress());
            }
        } else {
            // Set the distance label
            distance = route.getEstimatedDistance();
            duration = route.getEstimatedArrivalTime();
            sourceText.setText(route.startAddress.getStreetAddress());
            destinationText.setText(route.endAddress.getStreetAddress());
        }

        TextView durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        TextView lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        TextView etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);


        if (distance > 1000) {
            distance /= 1000;
            lengthText.setText(String.format("%.1f km", distance));
        } else {
            lengthText.setText(String.format("%d m", (int) distance));
        }

        // Set the duration label
        durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

        boolean hourFormat = MapActivity.format;

        SimpleDateFormat sdf = null;
        if (hourFormat) {
            sdf = new SimpleDateFormat("HH:mm");
        } else {
            sdf = new SimpleDateFormat("HH:mm a");
        }

        if (arrivalTime > 0) {
            arrivalTime = arrivalTime*1000;
            etaText.setText(sdf.format(arrivalTime).toString());
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.SECOND, (int) duration);
            Date arrivalTimee = c.getTime();
            etaText.setText(sdf.format(arrivalTimee));
        }


        // Add the ability to flip the route
        ((ImageButton) v.findViewById(R.id.btnAddressSwap)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.flipRoute();
            }
        });

        // Only show the go button if the route starts at the current location
        if (parent.getRoute().startAddress.isCurrentLocation()) {
            v.findViewById(R.id.navigationOverviewGoButtonContainer).setVisibility(View.VISIBLE);
        }else if(MapActivity.isBreakChosen){
            v.findViewById(R.id.navigationOverviewGoButtonContainer).setVisibility(View.VISIBLE);
        }        else {
            v.findViewById(R.id.navigationOverviewGoButtonContainer).setVisibility(View.GONE);
        }

        // Highlight the relevant route type button
        switch (route.getType()) {
            case GREEN:
                greenButton.setImageResource(R.drawable.btn_route_green_enabled);
                break;
            case CARGO:
                cargoButton.setImageResource(R.drawable.btn_route_cargo_enabled);
                break;
            case FASTEST:
                fastButton.setImageResource(R.drawable.btn_route_fastest_enabled);
                break;
            case BREAK:
                breakButton.setImageResource(R.drawable.btn_train_enabled);
                break;
            default:
                break;
        }

        sourceText.setOnClickListener(this);
        destinationText.setOnClickListener(this);

        ((TextView) v.findViewById(R.id.newRouteText)).setText(IbikeApplication.getString("Start"));
        if (IbikeApplication.getAppName().equals("Cykelplanen")) {
            ((TextView) v.findViewById(R.id.newRouteText)).setTextColor(getResources().getColor(R.color.CPActionBar));

        }

        return v;
    }

    public void setParent(NavigationMapHandler parent) {
        this.parent = parent;
    }

    /**
     * This is the click handler for the fast/cargo/green route buttons
     *
     * @param v
     */
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.navigationOverviewFastButton) {
            disableAllRouteButtons();
            fastButton.setImageResource(R.drawable.btn_route_fastest_enabled);
            MapActivity.isBreakChosen = false;
            this.parent.changeRouteType(RouteType.FASTEST);

        } else if (v.getId() == R.id.navigationOverviewCargoButton) {
            disableAllRouteButtons();
            cargoButton.setImageResource(R.drawable.btn_route_cargo_enabled);

            this.parent.changeRouteType(RouteType.CARGO);

        } else if (v.getId() == R.id.navigationOverviewGreenButton) {
            disableAllRouteButtons();
            MapActivity.isBreakChosen = false;
            greenButton.setImageResource(R.drawable.btn_route_green_enabled);

            this.parent.changeRouteType(RouteType.GREEN);

        } else if (v.getId() == R.id.navigationOverviewBreakButton) {
            disableAllRouteButtons();
            breakButton.setImageResource(R.drawable.btn_train_enabled);
            MapActivity.isBreakChosen = true;
            MapActivity.pager.setAdapter(null);
            MapActivity.tabs.setVisibility(View.GONE);
            MapActivity.progressBarHolder.setVisibility(View.VISIBLE);

            this.parent.changeRouteType(RouteType.BREAK);
        } else if (v.getId() == R.id.navigationOverviewSource) {
            MapActivity activity = (MapActivity) this.getActivity();
            Intent i = new Intent(activity, SearchAutocompleteActivity.class);
            activity.startActivityForResult(i, MapActivity.REQUEST_CHANGE_SOURCE_ADDRESS);

        } else if (v.getId() == R.id.navigationOverviewDestination) {
            MapActivity activity = (MapActivity) this.getActivity();
            Intent i = new Intent(activity, SearchAutocompleteActivity.class);
            activity.startActivityForResult(i, MapActivity.REQUEST_CHANGE_DESTINATION_ADDRESS);
        }
    }

    public void disableAllRouteButtons() {
        fastButton.setImageResource(R.drawable.btn_route_fastest_disabled);
        cargoButton.setImageResource(R.drawable.btn_route_cargo_disabled);
        greenButton.setImageResource(R.drawable.btn_route_green_disabled);
        if (IbikeApplication.getAppName().equals("Cykelplanen")) {
            MapActivity.progressBarHolder.setVisibility(View.GONE);
            breakButton.setImageResource(R.drawable.btn_train_disabled);
            MapActivity.breakFrag.setVisibility(View.GONE);
        }
    }

}
