package com.spoiledmilk.ibikecph.navigation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;

/**
 * Created by jens on 6/1/15.
 */
public class NavigationOverviewInfoPane extends InfoPaneFragment {
    //private SMRoute route;

    private String endStationName;
    private NavigationMapHandler parent;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.endStationName =  getArguments().getString("endStationName");
        this.parent = (NavigationMapHandler) getArguments().getSerializable("NavigationMapHandler");
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_navigation_overview, container, false);

        TextView addressView = (TextView) v.findViewById(R.id.navigationOverviewAddress);
        addressView.setText("foo: " + endStationName);

        Button goButton = (Button) v.findViewById(R.id.navigationOverviewGoButton);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.goButtonClicked();
            }
        });

        return v;
    }

    public void setParent(NavigationMapHandler parent) {
        this.parent = parent;
    }
}
