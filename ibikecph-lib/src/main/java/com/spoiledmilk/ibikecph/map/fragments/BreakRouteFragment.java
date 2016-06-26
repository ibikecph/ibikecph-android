package com.spoiledmilk.ibikecph.map.fragments;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.BreakRouteRequester;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

import java.text.SimpleDateFormat;

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

    protected JsonNode jsonNode;

    // newInstance constructor for creating fragment with arguments
    public static BreakRouteFragment newInstance(BreakRouteRequester.BreakRouteResponse response, int position) {
        BreakRouteFragment breakRouteFragment = new BreakRouteFragment();
        breakRouteFragment.setData(response.getJsonNode().get(position));
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
            parseData();

            //dummyData();
            int marginPx = convertToDp(10);
            int paddingPx = convertToDp(10);
            tableLayout = (TableLayout) root.findViewById(R.id.tableLayout);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(marginPx, 0, marginPx, 0);

            for (int i = 0; i < jsonNode.path("journey").size(); i++) {

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
                if (i < jsonNode.path("journey").size() - 1) {
                    lineIconIV.setImageResource(R.drawable.route_line);
                }

                String type = jsonNode.path("journey").get(i).path("route_summary").path("type").textValue();
                if (type.equals("BIKE")) {
                    typeIconIV.setImageResource(R.drawable.route_bike);
                } else if (type.equals("M")) {
                    typeIconIV.setImageResource(R.drawable.route_metro);
                } else if (type.equals("S")) {
                    typeIconIV.setImageResource(R.drawable.route_s);
                } else if (type.equals("TOG")) {
                    typeIconIV.setImageResource(R.drawable.route_train);
                } else if (type.equals("WALK")) {
                    typeIconIV.setImageResource(R.drawable.route_walk);
                } else if (type.equals("IC")) {
                    typeIconIV.setImageResource(R.drawable.route_train);
                } else if (type.equals("LYN")) {
                    typeIconIV.setImageResource(R.drawable.route_train);
                } else if (type.equals("REG")) {
                    typeIconIV.setImageResource(R.drawable.route_train);
                } else if (type.equals("BUS")) {
                    typeIconIV.setImageResource(R.drawable.route_bus);
                } else if (type.equals("EXB")) {
                    typeIconIV.setImageResource(R.drawable.route_bus);
                } else if (type.equals("NB")) {
                    typeIconIV.setImageResource(R.drawable.route_bus);
                } else if (type.equals("TB")) {
                    typeIconIV.setImageResource(R.drawable.route_bus);
                } else if (type.equals("F")) {
                    typeIconIV.setImageResource(R.drawable.route_ship_direction);
                }


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
    protected void parseData() {
        String type;
        String from;
        String to;

        for (int i = 0; i < jsonNode.path("journey").size(); i++) {
            type = jsonNode.path("journey").get(i).path("route_summary").path("type").textValue();


            if (i == jsonNode.path("journey").size() - 1) {
                from = IBikeApplication.getString("From") + " " + IBikeApplication.getString(jsonNode.path("journey").get(i).path("route_name").get(0).textValue());
                to = " " + IBikeApplication.getString("To") + "\n" + IBikeApplication.getString(jsonNode.path("journey").get(i).path("route_name").get(1).textValue());
            } else {
                from = IBikeApplication.getString("From") + " " + jsonNode.path("journey").get(i).path("route_name").get(0).textValue();
                to = " " + IBikeApplication.getString("To") + "\n" + jsonNode.path("journey").get(i).path("route_name").get(1).textValue();
            }

            startTime[i] = timeStampFormat(jsonNode.path("journey").get(i).path("route_summary").path("departure_time").asLong());
            arrivalTime[i] = timeStampFormat(jsonNode.path("journey").get(i).path("route_summary").path("arrival_time").asLong());

            if (type.equals("BIKE")) {
                typeAndTime[i] = IBikeApplication.getString("vehicle_1") + " " + formatDistance(jsonNode.path("journey").get(i).path("route_summary").path("total_distance").doubleValue()) + "    " + formatTime((jsonNode.path("journey").get(i).path("route_summary").path("total_time").asDouble()));
                fromTo[i] = from + to;
            } else if (type.equals("WALK")) {
                typeAndTime[i] = IBikeApplication.getString("vehicle_2") + " " + formatDistance(jsonNode.path("journey").get(i).path("route_summary").path("total_distance").doubleValue()) + "    " + formatTime((jsonNode.path("journey").get(i).path("route_summary").path("total_time").asDouble()));
                fromTo[i] = from + to;
            } else {
                typeAndTime[i] = jsonNode.path("journey").get(i).path("route_name").get(0).textValue();
                fromTo[i] = jsonNode.path("journey").get(i).path("route_summary").path("name").textValue() + " " + IBikeApplication.getString("To") + "\n" + jsonNode.path("journey").get(i).path("route_name").get(1).textValue();
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

    public void setData(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
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

        String time;
        seconds = seconds * 1000;

        // 24-hour format
        if (DateFormat.is24HourFormat(this.getActivity())) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            //sdf.setTimeZone(TimeZone.getDefault());
            time = sdf.format(seconds).toString();
        }
        // 12-hour format
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            //sdf.setTimeZone(TimeZone.getDefault());
            time = sdf.format(seconds).toString();
        }

        return time;
    }

    public void dummyData() {
        for (int i = 0; i < 5; i++) {
            startTime[i] = "11" + ":0" + i;
            arrivalTime[i] = "\n12" + ":1" + i;
            typeAndTime[i] = "Cykel " + i + " km " + "   0t " + i + "m";
            fromTo[i] = "Fra nuværende position til\nRandom st. ";
        }
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
