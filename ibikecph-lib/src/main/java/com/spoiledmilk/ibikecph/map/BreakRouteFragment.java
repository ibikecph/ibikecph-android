package com.spoiledmilk.ibikecph.map;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;

/**
 * Created by Daniel on 12-11-2015.
 */
public class BreakRouteFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_POSITION = "position";
    private int position;
    private View root;

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


    // newInstance constructor for creating fragment with arguments
    public static BreakRouteFragment newInstance(int position) {
        BreakRouteFragment breakRouteFragment = new BreakRouteFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        breakRouteFragment.setArguments(b);
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
        position = getArguments().getInt(ARG_POSITION);
        MapActivity.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        JsonNode jsonNode = MapActivity.breakRouteJSON;
        setData(jsonNode.path("journeys").get(position));
        //dummyData();
        int marginPx = convertToDp(10);
        int paddingPx = convertToDp(10);
        tableLayout = (TableLayout) root.findViewById(R.id.tableLayout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(marginPx, 0, marginPx, 0);

        for (int i = 0; i < jsonNode.path("journeys").get(position).size(); i++) {

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
            if (i < jsonNode.path("journeys").get(position).size() - 1) {
                lineIconIV.setImageResource(R.drawable.route_line);
            }

            String type = jsonNode.path("journeys").get(position).get(i).path("route_summary").path("type").textValue();
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
            } //FÆRGE, ANDET?

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

        String type;

        for (int i = 0; i < jsonNode.size(); i++) {
            type = jsonNode.get(i).path("route_summary").path("type").textValue();
            startTime[i] = "";
            arrivalTime[i] = "";
            if (type.equals("BIKE")) {
                typeAndTime[i] = "Cykel " + formatDistance(jsonNode.get(i).path("route_summary").path("total_distance").doubleValue()) + "    " + formatTime((jsonNode.get(i).path("route_summary").path("total_time").asDouble()));
            } else if (type.equals("WALK")) {
                typeAndTime[i] = "Gå " + formatDistance(jsonNode.get(i).path("route_summary").path("total_distance").doubleValue()) + "    " + formatTime((jsonNode.get(i).path("route_summary").path("total_time").asDouble()));
            } else if (type.equals("TOG")) {
                typeAndTime[i] = jsonNode.get(i).path("route_name").get(0).textValue();
            } else {
                typeAndTime[i] = jsonNode.get(i).path("route_name").get(0).textValue();
            }
            fromTo[i] = "Fra " + jsonNode.get(i).path("route_name").get(0).textValue() + " til\n" + jsonNode.get(i).path("route_name").get(1).textValue();

        }

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
