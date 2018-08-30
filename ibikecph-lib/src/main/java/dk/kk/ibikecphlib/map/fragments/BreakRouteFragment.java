package dk.kk.ibikecphlib.map.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.navigation.NavigationState;
import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteResponse;
import dk.kk.ibikecphlib.navigation.routing_engine.Leg;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.navigation.routing_engine.TransportationType;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.tracking.TrackListAdapter;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.navigation.NavigationState;
import dk.kk.ibikecphlib.navigation.routing_engine.BreakRouteResponse;
import dk.kk.ibikecphlib.navigation.routing_engine.Leg;
import dk.kk.ibikecphlib.navigation.routing_engine.Route;
import dk.kk.ibikecphlib.navigation.routing_engine.TransportationType;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.tracking.TrackListAdapter;

/**
 * Created by Daniel on 12-11-2015.
 */
public class BreakRouteFragment extends Fragment implements View.OnClickListener {

    private View root;
    private boolean hasDataBeenSet = false;

    private LinearLayout imageLayout, timeLayout, destLayout, outerLayout;
    private TableRow tableRow;
    private TableLayout tableLayout;

    private String[] startTime = new String[20]; // Start time for each step of the full route
    private String[] arrivalTime = new String[20]; // Arrival time of each step of the full route
    private String[] typeAndTime = new String[20]; // Transportation type and distance time or station name
    private String[] fromTo = new String[20]; // From A to B of each step of the full route

    ImageView typeIconIV;
    ImageView lineIconIV;
    TextView startTimeTV;
    TextView arrivalTimeTV;
    TextView typeTV;
    TextView fromToTV;

    // protected JsonNode jsonNode;
    protected Route route;

    public void setRoute(Route route) {
        this.route = route;
    }

    // newInstance constructor for creating fragment with arguments
    public static BreakRouteFragment newInstance(BreakRouteResponse response, int position) {
        BreakRouteFragment breakRouteFragment = new BreakRouteFragment();
        // breakRouteFragment.setData(response.getJsonNode().get(position));
        breakRouteFragment.setRoute(response.getRoute(position));
        return breakRouteFragment;
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.break_route_fragment, container, false);
        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasDataBeenSet) {
            parseJourney();

            int marginPx = convertToDp(10);
            int paddingPx = convertToDp(10);
            tableLayout = (TableLayout) root.findViewById(R.id.tableLayout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(marginPx, 0, marginPx, 0);

            for(int i = 0; i < route.getLegs().size(); i++) {
                // Layouts
                imageLayout = new LinearLayout(getActivity());
                timeLayout = new LinearLayout(getActivity());
                destLayout = new LinearLayout(getActivity());
                outerLayout = new LinearLayout(getActivity());
                tableRow = new TableRow(getActivity());

                // Layout settings
                imageLayout.setOrientation(LinearLayout.VERTICAL);
                timeLayout.setOrientation(LinearLayout.VERTICAL);
                destLayout.setOrientation(LinearLayout.VERTICAL);
                tableRow.setGravity(Gravity.CENTER_HORIZONTAL);

                // Instantiate views
                typeIconIV = new ImageView(getActivity());
                lineIconIV = new ImageView(getActivity());
                startTimeTV = new TextView(getActivity());
                arrivalTimeTV = new TextView(getActivity());
                typeTV = new TextView(getActivity());
                fromToTV = new TextView(getActivity());

                // Set margin
                imageLayout.setLayoutParams(params);
                timeLayout.setLayoutParams(params);
                destLayout.setLayoutParams(params);
                imageLayout.requestLayout();
                timeLayout.requestLayout();
                destLayout.requestLayout();

                // Text settings
                arrivalTimeTV.setTextColor(Color.GRAY);
                arrivalTimeTV.setTextSize(10);
                arrivalTimeTV.setPadding(paddingPx, 0, 0, 0);
                fromToTV.setTextColor(Color.GRAY);
                fromToTV.setTextSize(10);

                // Set text and image
                startTimeTV.setText(this.startTime[i]);
                arrivalTimeTV.setText(this.arrivalTime[i]);
                typeTV.setText(typeAndTime[i]);
                fromToTV.setText(this.fromTo[i]);
                // Don't set lineIcon after last stop
                if (i != route.getLegs().size()-1) {
                    lineIconIV.setImageResource(R.drawable.route_line);
                }

                // Update the route type icon
                int typeIconId = route.getLegs().get(i).getTransportType().getDrawableId(TransportationType.DrawableSize.SMALL);
                typeIconIV.setImageResource(typeIconId);

                // Add the views
                imageLayout.addView(typeIconIV);
                imageLayout.addView(lineIconIV);
                timeLayout.addView(startTimeTV);
                timeLayout.addView(arrivalTimeTV);
                destLayout.addView(typeTV);
                destLayout.addView(fromToTV);
                tableLayout.addView(tableRow);
                tableRow.addView(outerLayout);
                outerLayout.addView(imageLayout);
                outerLayout.addView(timeLayout);
                outerLayout.addView(destLayout);

            }
        }
        hasDataBeenSet = true;
    }

    /**
     * Turn the break route data into an internal representation.
     * Don't call this before the fragment has been attached to an activity.
     */
    protected void parseJourney() {
        for(int i = 0; i < route.getLegs().size(); i++) {
            Leg leg = route.getLegs().get(i);
            String from, to;

            Address start = leg.getStartAddress();
            Address end = leg.getEndAddress();

            from = IBikeApplication.getString("From") + " " + (start == null ? "?" : start.getDisplayName());
            to = " " + IBikeApplication.getString("To") + "\n" + (end == null ? "?" : end.getDisplayName());

            startTime[i] = timeStampFormat(leg.getDepartureTime());
            arrivalTime[i] = timeStampFormat(leg.getArrivalTime());

            double duration = NavigationState.getBikingDuration(route, leg);

            if (leg.getTransportType() == TransportationType.BIKE) {
                typeAndTime[i] = IBikeApplication.getString("vehicle_1") + " ";
                typeAndTime[i] += formatDistance(leg.getDistance()) + " ";
                typeAndTime[i] += "(" + formatTime(duration) + ")";
                fromTo[i] = from + to;
            } else if (leg.getTransportType() == TransportationType.WALK) {
                typeAndTime[i] = IBikeApplication.getString("vehicle_2") + " ";
                typeAndTime[i] += formatDistance(leg.getDistance()) + " ";
                typeAndTime[i] += "(" + formatTime(duration) + ")";
                fromTo[i] = from + to;
            } else {
                typeAndTime[i] = leg.getStartAddress().getDisplayName();
                fromTo[i] = "";
                if(route.description != null && !route.description.isEmpty()) {
                    fromTo[i] += route.description + " ";
                }
                fromTo[i] += IBikeApplication.getString("To").toLowerCase() + "\n";
                fromTo[i] += leg.getEndAddress().getDisplayName();
            }
        }
    }

    @Override
    public void onClick(View view) {
    }

    public int convertToDp(int input) {
        //Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        //Convert the dps to pixels, based on density scale
        return (int) (input * scale + 0.5f);
    }

    public String formatDistance(double distance) {
        String formattedDistance;
        if (distance > 1000) {
            distance /= 1000;
            formattedDistance = (String.format("%.1f km", distance));
        } else {
            formattedDistance = (String.format("%d m", (int) distance));
        }
        return formattedDistance;
    }

    public String formatTime(double seconds) {
        String time = TrackListAdapter.durationToFormattedTime(seconds);
        return time;
    }

    public String timeStampFormat(long seconds) {
        seconds = seconds * 1000;
        return DateFormat.getTimeFormat(this.getContext()).format(seconds);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
