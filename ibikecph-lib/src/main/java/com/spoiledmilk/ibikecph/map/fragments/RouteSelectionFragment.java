package com.spoiledmilk.ibikecph.map.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 6/1/15.
 */
public class RouteSelectionFragment extends MapStateFragment implements View.OnClickListener, RouteSelectionState.RouteTypeChangeListener {
    protected ImageButton fastButton, cargoButton, greenButton, breakButton;
    protected TextView sourceText, destinationText, durationText, lengthText, etaText;

    protected RouteSelectionState mapState;

    protected View v;

    protected SimpleDateFormat dateFormat;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapState = getMapState(RouteSelectionState.class);

        if (DateFormat.is24HourFormat(this.getActivity())) {
            dateFormat = new SimpleDateFormat("HH:mm");
        } else {
            dateFormat = new SimpleDateFormat("hh:mm a");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(getLayoutResource(), container, false);

        View startRouteButton = v.findViewById(R.id.startRouteButton);
        startRouteButton.setOnClickListener(this);

        fastButton = (ImageButton) v.findViewById(R.id.routeSelectionFastButton);
        cargoButton = (ImageButton) v.findViewById(R.id.routeSelectionCargoButton);
        greenButton = (ImageButton) v.findViewById(R.id.routeSelectionGreenButton);
        breakButton = (ImageButton) v.findViewById(R.id.routeSelectionBreakButton);

        sourceText = (TextView) v.findViewById(R.id.navigationOverviewSource);
        destinationText = (TextView) v.findViewById(R.id.navigationOverviewDestination);
        durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        fastButton.setOnClickListener(this);
        cargoButton.setOnClickListener(this);
        greenButton.setOnClickListener(this);
        breakButton.setOnClickListener(this);

        // Add the ability to flip the route
        v.findViewById(R.id.btnAddressSwap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapState.flipRoute();
            }
        });

        TextView sourceText = (TextView) v.findViewById(R.id.navigationOverviewSource);
        TextView destinationText = (TextView) v.findViewById(R.id.navigationOverviewDestination);
        sourceText.setOnClickListener(this);
        destinationText.setOnClickListener(this);

        ((TextView) v.findViewById(R.id.startRouteButtonText)).setText(IBikeApplication.getString("Start"));

        return v;
    }

    /**
     * This is the click handler for the fast/cargo/green route buttons
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startRouteButton) {
            mapState.startNavigation();
        } else if (v.getId() == R.id.routeSelectionFastButton) {
            mapState.setType(RouteType.FASTEST);
        } else if (v.getId() == R.id.routeSelectionCargoButton) {
            mapState.setType(RouteType.CARGO);
        } else if (v.getId() == R.id.routeSelectionGreenButton) {
            mapState.setType(RouteType.GREEN);
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

    public void refreshView() {
        Journey journey = mapState.getJourney();
        if(journey == null) {
            sourceText.setText("");
            destinationText.setText("");
        } else {
            float distance;
            float duration;

            sourceText.setText(journey.getStartAddress().getDisplayName());
            destinationText.setText(journey.getEndAddress().getDisplayName());

            distance = journey.getEstimatedDistance();
            duration = journey.getEstimatedDuration();

            if (distance > 1000) {
                distance /= 1000;
                lengthText.setText(String.format("%.1f km", distance));
            } else {
                lengthText.setText(String.format("%d m", (int) distance));
            }

            // Set the duration label
            durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

            etaText.setText(dateFormat.format(journey.getArrivalTime()));

            // Only show the go button if the route starts at the current location or the route type
            // is break route.
            if (journey.getStartAddress().isCurrentLocation() || mapState.getType() == RouteType.BREAK) {
                v.findViewById(R.id.startRouteButton).setVisibility(View.VISIBLE);
            } else {
                v.findViewById(R.id.startRouteButton).setVisibility(View.GONE);
            }
        }
    }

    /**
     * A way to get the fragment, as a method to enable override.
     * @return
     */
    protected int getLayoutResource() {
        return R.layout.route_selection_fragment;
    }

    @Override
    public void routeTypeChanged(RouteType newType) {
        // Make sure the buttons are in the disabled state if not enabled.
        int greenButtonDrawable = R.drawable.btn_route_green_disabled;
        int cargoButtonDrawable = R.drawable.btn_route_cargo_disabled;
        int fastButtonDrawable = R.drawable.btn_route_fastest_disabled;
        int breakButtonDrawable = R.drawable.btn_train_disabled;

        // TODO: Remove the need for this
        MapActivity.isBreakChosen = newType == RouteType.BREAK;

        switch (newType) {
            case FASTEST:
                fastButtonDrawable = R.drawable.btn_route_fastest_enabled;
                break;
            case CARGO:
                cargoButtonDrawable = R.drawable.btn_route_cargo_enabled;
                break;
            case GREEN:
                greenButtonDrawable = R.drawable.btn_route_green_enabled;
                break;
            case BREAK:
                breakButtonDrawable = R.drawable.btn_train_enabled;
                break;
            default:
                break;
        }

        greenButton.setImageResource(greenButtonDrawable);
        cargoButton.setImageResource(cargoButtonDrawable);
        fastButton.setImageResource(fastButtonDrawable);
        breakButton.setImageResource(breakButtonDrawable);
    }
}
