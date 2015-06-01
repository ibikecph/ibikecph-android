package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;

/**
 * Created by jens on 6/1/15.
 */
public class NavigationOverviewInfoPane extends InfoPaneFragment {
    //private SMRoute route;

    private String endStationName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.endStationName =  getArguments().getString("endStationName");
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_navigation_overview, container, false);

        TextView addressView = (TextView) v.findViewById(R.id.navigationOverviewAddress);

        addressView.setText("foo: " + endStationName);
        return v;
    }
}
