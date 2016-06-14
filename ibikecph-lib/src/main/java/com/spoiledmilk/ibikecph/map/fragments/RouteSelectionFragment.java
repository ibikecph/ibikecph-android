package com.spoiledmilk.ibikecph.map.fragments;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 6/1/15.
 */
public class RouteSelectionFragment extends MapStateFragment implements View.OnClickListener {
    private ImageButton fastButton, cargoButton, greenButton, breakButton;

    protected RouteSelectionState mapState;

    protected View v;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapState = getMapState(RouteSelectionState.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.route_selection_fragment, container, false);

        View startRouteButton = (View) v.findViewById(R.id.startRouteButton);
        startRouteButton.setOnClickListener(this);

        fastButton = (ImageButton) v.findViewById(R.id.routeSelectionFastButton);
        cargoButton = (ImageButton) v.findViewById(R.id.routeSelectionCargoButton);
        greenButton = (ImageButton) v.findViewById(R.id.routeSelectionGreenButton);
        breakButton = (ImageButton) v.findViewById(R.id.routeSelectionBreakButton);

        fastButton.setOnClickListener(this);
        cargoButton.setOnClickListener(this);
        greenButton.setOnClickListener(this);
        breakButton.setOnClickListener(this);

        // Show or hide the break route button based on the setting
        boolean breakRouteEnabled = getResources().getBoolean(R.bool.breakRouteEnabled);
        breakButton.setVisibility(breakRouteEnabled ? View.VISIBLE : View.GONE);

        // Add the ability to flip the route
        ((ImageButton) v.findViewById(R.id.btnAddressSwap)).setOnClickListener(new View.OnClickListener() {
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
        if (IBikeApplication.getAppName().equals("CykelPlanen")) {
            ((TextView) v.findViewById(R.id.startRouteButtonText)).setTextColor(getResources().getColor(R.color.CPActionBar));

        }

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
            disableAllRouteButtons();
            fastButton.setImageResource(R.drawable.btn_route_fastest_enabled);
            MapActivity.isBreakChosen = false;
            mapState.setType(RouteType.FASTEST);

        } else if (v.getId() == R.id.routeSelectionCargoButton) {
            disableAllRouteButtons();
            cargoButton.setImageResource(R.drawable.btn_route_cargo_enabled);

            mapState.setType(RouteType.CARGO);

        } else if (v.getId() == R.id.routeSelectionGreenButton) {
            disableAllRouteButtons();
            MapActivity.isBreakChosen = false;
            greenButton.setImageResource(R.drawable.btn_route_green_enabled);

            mapState.setType(RouteType.GREEN);

        } else if (v.getId() == R.id.routeSelectionBreakButton) {
            // TODO: Move all this controller code to the map state
            disableAllRouteButtons();
            breakButton.setImageResource(R.drawable.btn_train_enabled);
            MapActivity.isBreakChosen = true;
            NavigationMapHandler.routePos = 0;
            MapActivity.pager.setAdapter(null);
            MapActivity.tabs.setVisibility(View.GONE);
            MapActivity.progressBarHolder.setVisibility(View.VISIBLE);

            mapState.setType(RouteType.BREAK);
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
        if (IBikeApplication.getAppName().equals("CykelPlanen")) {
            MapActivity.progressBarHolder.setVisibility(View.GONE);
            breakButton.setImageResource(R.drawable.btn_train_disabled);
            MapActivity.breakFrag.setVisibility(View.GONE);
        }

        NavigationMapHandler.displayExtraField = false;
        NavigationMapHandler.displayGetOffAt = false;
        NavigationMapHandler.isPublic = false;
        NavigationMapHandler.getOffAt = "";
        NavigationMapHandler.lastType = "";

        if (Geocoder.arrayLists != null) {
            for (int i = 0; i < Geocoder.arrayLists.size(); i++) {
                for (int j = 0; j < Geocoder.arrayLists.get(i).size(); j++) {
                    Geocoder.arrayLists.get(i).get(j).removeListeners();
                    IBikeApplication.getService().removeLocationListener(Geocoder.arrayLists.get(i).get(j));
                }
            }
        }

    }

    public void refreshView() {
        TextView sourceText = (TextView) v.findViewById(R.id.navigationOverviewSource);
        TextView destinationText = (TextView) v.findViewById(R.id.navigationOverviewDestination);
        TextView durationText = (TextView) v.findViewById(R.id.navigationOverviewRouteDuration);
        TextView lengthText = (TextView) v.findViewById(R.id.navigationOverviewRouteLength);
        TextView etaText = (TextView) v.findViewById(R.id.navigationOverviewRouteETA);

        SMRoute route = mapState.getRoute();
        if(route == null) {
            sourceText.setText("");
            destinationText.setText("");
        } else {
            float distance;
            float duration;
            long arrivalTime = 0;

            boolean breakRouteEnabled = getResources().getBoolean(R.bool.breakRouteEnabled);
            if(breakRouteEnabled) {
                throw new RuntimeException("Break route needs to be refactored.");
            }
            /*
            // TODO: Move this to the cykelplanen directory.
            if (IBikeApplication.getAppName().equals("CykelPlanen")) {
                breakButton = (ImageButton) v.findViewById(R.id.navigationOverviewBreakButton);
                boolean breakRouteEnabled = getResources().getBoolean(R.bool.breakRouteEnabled);
                Log.d(getClass().getSimpleName(), "breakRouteEnabled = " + breakRouteEnabled);
                breakButton.setVisibility(breakRouteEnabled ? View.VISIBLE : View.GONE);

                cargoButton.setVisibility(View.GONE);

                // Set the distance label
                if (MapActivity.isBreakChosen && Geocoder.totalBikeDistance != null) {
                    distance = Geocoder.totalBikeDistance.get(NavigationMapHandler.obsInt.getPageValue());
                    duration = Geocoder.totalTime.get(NavigationMapHandler.obsInt.getPageValue());
                    arrivalTime = Geocoder.arrivalTime.get(NavigationMapHandler.obsInt.getPageValue());
                    sourceText.setText(IBikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
                    destinationText.setText(DestinationPreviewFragment.name);
                } else {
                    distance = route.getEstimatedDistance();
                    duration = route.getEstimatedArrivalTime();
                    sourceText.setText(IBikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
                    destinationText.setText(route.endAddress.getStreetAddress());
                }
            } else {
                // Set the distance label
                distance = route.getEstimatedDistance();
                duration = route.getEstimatedArrivalTime();
                sourceText.setText(route.startAddress.getStreetAddress());
                destinationText.setText(route.endAddress.getStreetAddress());
            }
            */

            sourceText.setText(route.startAddress.getStreetAddress());
            destinationText.setText(route.endAddress.getStreetAddress());

            distance = route.getEstimatedDistance();
            duration = route.getEstimatedArrivalTime();

            if (distance > 1000) {
                distance /= 1000;
                lengthText.setText(String.format("%.1f km", distance));
            } else {
                lengthText.setText(String.format("%d m", (int) distance));
            }

            // Set the duration label
            durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

            SimpleDateFormat sdf = null;
            if (DateFormat.is24HourFormat(this.getActivity())) {
                sdf = new SimpleDateFormat("HH:mm");
            } else {
                sdf = new SimpleDateFormat("HH:mm a");
            }

            if (arrivalTime > 0) {
                // TODO: Move this the the Cykelplanen directory.
                arrivalTime = arrivalTime * 1000;
                etaText.setText(sdf.format(arrivalTime).toString());
            } else {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.SECOND, (int) duration);
                Date arrivalTimee = c.getTime();
                etaText.setText(sdf.format(arrivalTimee));
            }
            // Only show the go button if the route starts at the current location
            if (route != null && route.startAddress.isCurrentLocation()) {
                v.findViewById(R.id.startRouteButton).setVisibility(View.VISIBLE);
            } else if (MapActivity.isBreakChosen) {
                v.findViewById(R.id.startRouteButton).setVisibility(View.VISIBLE);
            } else {
                v.findViewById(R.id.startRouteButton).setVisibility(View.GONE);
            }
        }

        // Make sure the buttons are in the disabled state if not enabled.
        int greenButtonDrawable = R.drawable.btn_route_green_disabled;
        int cargoButtonDrawable = R.drawable.btn_route_cargo_disabled;
        int fastButtonDrawable = R.drawable.btn_route_fastest_disabled;
        int breakButtonDrawable = R.drawable.btn_train_disabled;

        if(route != null) {
            // Highlight the relevant route type button
            switch (route.getType()) {
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
        }

        greenButton.setImageResource(greenButtonDrawable);
        cargoButton.setImageResource(cargoButtonDrawable);
        fastButton.setImageResource(fastButtonDrawable);
        breakButton.setImageResource(breakButtonDrawable);
    }
}
