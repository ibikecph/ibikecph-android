// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.cykelsuperstier.map;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.OverlayType;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

/**
 * Activity for toggling station overlays on map.
 * @author markus
 *
 */
public class OverlaysActivity extends Activity {

    boolean isPathSelected, isServiceSelected;

    ActionBar actionBar;

    TextView textPath, textService;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.overlays_activity);

        IbikePreferences settings = IbikeApplication.getSettings();
        isPathSelected       = settings.getOverlay(OverlayType.PATH);
        isServiceSelected    = settings.getOverlay(OverlayType.SERVICE);

        actionBar = getActionBar();

        textPath       = (TextView) findViewById(R.id.pathText);
        textService    = (TextView) findViewById(R.id.serviceText);

        refreshOverlays();
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    public void onPathContainerClick(View v) {
        isPathSelected = !isPathSelected;
        updateContainer(OverlayType.PATH, isPathSelected);
        IbikeApplication.getSettings().setOverlay(OverlayType.PATH, isPathSelected);
    }

    public void onServiceContainerClick(View v) {
        isServiceSelected = !isServiceSelected;
        updateContainer(OverlayType.SERVICE, isServiceSelected);
        IbikeApplication.getSettings().setOverlay(OverlayType.SERVICE, isServiceSelected);
    }


    public void refreshOverlays() {
        updateContainer(OverlayType.PATH, isPathSelected);
        updateContainer(OverlayType.SERVICE, isServiceSelected);
    }

    private void updateContainer(OverlayType type, boolean selected) {
        View view;
        ImageView checkbox;
        ImageView image;
        int imageWhite, imageGrey;
        TextView text;

        switch (type) {
            case PATH:
                view = findViewById(R.id.pathContainer);
                checkbox = (ImageView) findViewById(R.id.pathCheckbox);
                image = (ImageView) findViewById(R.id.pathImg);
                imageWhite = R.drawable.bike_icon_white;
                imageGrey = R.drawable.bike_icon_gray;
                text = (TextView) findViewById(R.id.pathText);
                break;

            case SERVICE:
                view = findViewById(R.id.serviceContainer);
                checkbox = (ImageView) findViewById(R.id.serviceCheckbox);
                image = (ImageView) findViewById(R.id.serviceImg);
                imageWhite = R.drawable.service_pump_icon_white;
                imageGrey = R.drawable.service_pump_icon_gray;
                text = (TextView) findViewById(R.id.serviceText);
                break;

            default:
                return;
        }

        view.setBackgroundColor(selected ? Color.rgb(236, 104, 0) : Color.rgb(255, 255, 255));
        checkbox.setImageResource(selected ? R.drawable.check_in_orange : R.drawable.check_field);
        image.setImageResource(selected ? imageWhite : imageGrey);
        text.setTextColor(selected ? Color.WHITE : getResources().getColor(R.color.DarkGrey));
    }

    private void initStrings() {
        actionBar.setTitle(CykelsuperstierApplication.getString("map_overlays"));
        textPath.setTypeface(CykelsuperstierApplication.getNormalFont());
        textPath.setText(CykelsuperstierApplication.getString("cycle_super_highways"));
        textService.setTypeface(CykelsuperstierApplication.getNormalFont());
        textService.setText(CykelsuperstierApplication.getString("service_stations"));
    }
}
