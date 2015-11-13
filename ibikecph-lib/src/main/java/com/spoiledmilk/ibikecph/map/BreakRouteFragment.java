package com.spoiledmilk.ibikecph.map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.R;

/**
 * Created by Daniel on 12-11-2015.
 */
public class BreakRouteFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_POSITION = "position";
    private int position;
    private View root;
    private TextView randomText;

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
        randomText = (TextView) root.findViewById(R.id.randomText);
        randomText.setText("Fragment number " + position);
        Log.d("DV", "Fragment onResume!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        Log.d("DV", "Fragment Clicked!");
    }

}
