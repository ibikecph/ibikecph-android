package com.spoiledmilk.ibikecph.map;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.R;

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

    private String[] imageType = new String[20]; // Image for each type of transportation
    private String[] startTime = new String[20]; // Start time for each step of the full route
    private String[] arrivalTime = new String[20]; // Arrival time of each step of the full route
    private String[] typeAndTime = new String[20]; // Transportation type and distance time or station name
    private String[] fromTo = new String[20]; // From A to B of each step of the full route

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
    }

    @Override
    public void onResume() {
        super.onResume();
        dummyData();
        int marginPx = convertToDp(10);
        int paddingPx = convertToDp(10);
        tableLayout = (TableLayout) root.findViewById(R.id.tableLayout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(marginPx, 0, marginPx, 0);

        for (int i = 0; i < 5; i++) {

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
            ImageView typeIcon = new ImageView(getActivity());
            ImageView lineIcon = new ImageView(getActivity());
            TextView startTime = new TextView(getActivity());
            TextView arrivalTime = new TextView(getActivity());
            TextView type = new TextView(getActivity());
            TextView fromTo = new TextView(getActivity());

            // Set margin
            imageLayout.setLayoutParams(params);
            timeLayout.setLayoutParams(params);
            destLayout.setLayoutParams(params);
            imageLayout.requestLayout();
            timeLayout.requestLayout();
            destLayout.requestLayout();

            // Text settings
            arrivalTime.setTextColor(Color.GRAY);
            arrivalTime.setTextSize(10);
            arrivalTime.setPadding(paddingPx, 0, 0, 0);
            fromTo.setTextColor(Color.GRAY);
            fromTo.setTextSize(10);

            // Set text and image
            typeIcon.setImageResource(R.drawable.btn_train_enabled);
            startTime.setText(this.startTime[i]);
            arrivalTime.setText(this.arrivalTime[i]);
            type.setText(typeAndTime[i]);
            fromTo.setText(this.fromTo[i]);

            // Add the views
            imageLayout.addView(typeIcon);
            // Don't set lineIcon after last stop
            if (i < 4) {
                lineIcon.setImageResource(R.drawable.fav_star);
                imageLayout.addView(lineIcon);
            }
            timeLayout.addView(startTime);
            timeLayout.addView(arrivalTime);
            destLayout.addView(type);
            destLayout.addView(fromTo);
            tableLayout.addView(tableRow);
            tableRow.addView(outerLayout);
            outerLayout.addView(imageLayout);
            outerLayout.addView(timeLayout);
            outerLayout.addView(destLayout);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        Log.d("DV", "Fragment Clicked!");
    }

    public int convertToDp(int input) {
        //Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        //Convert the dps to pixels, based on density scale
        return (int) (input * scale + 0.5f);
    }

    public void dummyData() {
        for (int i = 0; i < 5; i++) {
            startTime[i] = "11" + ":0" + i;
            arrivalTime[i] = "\n12" + ":1" + i;
            typeAndTime[i] = "Cykel " + i + " km " + "   0t " + i + "m";
            fromTo[i] = "Fra nuværende position til\nRandom st. ";
        }

    }


}
