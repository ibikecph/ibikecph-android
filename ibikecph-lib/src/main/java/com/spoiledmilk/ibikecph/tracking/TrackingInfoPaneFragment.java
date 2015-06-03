package com.spoiledmilk.ibikecph.tracking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.InfoPaneFragment;

/**
 * Shows summary statistics on the main map view
 */
public class TrackingInfoPaneFragment extends InfoPaneFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_tracking_statistics, container);

        return v;
    }

}