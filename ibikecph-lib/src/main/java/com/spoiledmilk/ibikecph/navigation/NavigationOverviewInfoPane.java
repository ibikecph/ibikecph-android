package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;

/**
 * Created by jens on 6/1/15.
 */
public class NavigationOverviewInfoPane extends InfoPaneFragment {
    private SMRoute route;

    public NavigationOverviewInfoPane(SMRoute route) {
        this.route = route;
    }


    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_navigation_overview, container, false);

        TextView addressView = (TextView) v.findViewById(R.id.navigationOverviewAddress);

        addressView.setText("foo: " + route.endStationName);
        return v;
    }
}
