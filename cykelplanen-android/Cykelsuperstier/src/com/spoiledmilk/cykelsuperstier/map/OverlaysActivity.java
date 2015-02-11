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

/**
 * Activity for toggling station overlays on map.
 * @author markus
 *
 */
public class OverlaysActivity extends Activity {

    ActionBar actionBar;
    TextView textPath,
            textService,
            textStrain,
            textMetro,
            textLocalTrain;

    boolean isPathSelected,
            isServiceSelected,
            isStrainSelected,
            isMetroSelected,
            isLocalTrainSelected;

    enum OverlayType {
        PATH,
        SERVICE,
        S_TRAIN,
        METRO,
        LOCAL_TRAIN
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.overlays_activity);

        actionBar = getActionBar();

        textPath       = (TextView) findViewById(R.id.pathText);
        textService    = (TextView) findViewById(R.id.serviceText);
        textStrain     = (TextView) findViewById(R.id.strainText);
        textMetro      = (TextView) findViewById(R.id.metroText);
        textLocalTrain = (TextView) findViewById(R.id.localTrainText);
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    public void onPathContainerClick(View v) {
        updateOverlay(OverlayType.PATH, isPathSelected);

//        TODO:
//        if (isPathSelected)
//            mapFragment.overlaysManager.removeBikeRoutes();
//        else
//            mapFragment.overlaysManager.drawBikeRoutes(this);

        isPathSelected = !isPathSelected;
    }

    public void onServiceContainerClick(View v) {
        updateOverlay(OverlayType.SERVICE, isServiceSelected);

//        TODO:
//        if (isServiceSelected)
//            mapFragment.overlaysManager.removeServiceStations();
//        else
//            mapFragment.overlaysManager.drawServiceStations(this);

        isServiceSelected = !isServiceSelected;
    }

    public void onStrainContainerClick(View v) {
        updateOverlay(OverlayType.S_TRAIN, isStrainSelected);

//        TODO:
//        if (isStrainSelected)
//            mapFragment.overlaysManager.removesTrainStations();
//        else
//            mapFragment.overlaysManager.drawsTrainStations(this);

        isStrainSelected = !isStrainSelected;
    }

    public void onMetroContainerClick(View v) {
        updateOverlay(OverlayType.METRO, isMetroSelected);

//        TODO:
//        if (isMetroSelected)
//            mapFragment.overlaysManager.removeMetroStations();
//        else
//            mapFragment.overlaysManager.drawMetroStations(this);

        isMetroSelected = !isMetroSelected;
    }

    public void onLocalTrainContainerClick(View v) {
        updateOverlay(OverlayType.LOCAL_TRAIN, isLocalTrainSelected);

//        TODO:
//        if (isLocalTrainSelected)
//            mapFragment.overlaysManager.removelocalTrainStations();
//        else
//            mapFragment.overlaysManager.drawlocalTrainStations(this);

        isLocalTrainSelected = !isLocalTrainSelected;
    }


    public void refreshOverlays(int overlaysShown) {
        if (((overlaysShown & 1) > 0 && !isPathSelected) || ((overlaysShown & 1) == 0 && isPathSelected))
            onPathContainerClick(findViewById(R.id.pathContainer));
        if (((overlaysShown & 2) > 0 && !isServiceSelected) || ((overlaysShown & 2) == 0 && isServiceSelected))
            onServiceContainerClick(findViewById(R.id.serviceContainer));
        if (((overlaysShown & 4) > 0 && !isStrainSelected) || ((overlaysShown & 4) == 0 && isStrainSelected))
            onStrainContainerClick(findViewById(R.id.strainContainer));
        if (((overlaysShown & 8) > 0 && !isMetroSelected) || ((overlaysShown & 8) == 0 && isMetroSelected))
            onMetroContainerClick(findViewById(R.id.metroContainer));
        if (((overlaysShown & 16) > 0 && !isLocalTrainSelected) || ((overlaysShown & 16) == 0 && isLocalTrainSelected))
            onLocalTrainContainerClick(findViewById(R.id.localTrainContainer));
    }

    private void initStrings() {
        actionBar.setTitle("Dummy"); // TODO: get real title
        textPath.setTypeface(CykelsuperstierApplication.getNormalFont());
        textPath.setText(CykelsuperstierApplication.getString("marker_type_1"));
        textService.setTypeface(CykelsuperstierApplication.getNormalFont());
        textService.setText(CykelsuperstierApplication.getString("marker_type_2"));
        textStrain.setTypeface(CykelsuperstierApplication.getNormalFont());
        textStrain.setText(CykelsuperstierApplication.getString("marker_type_3"));
        textMetro.setTypeface(CykelsuperstierApplication.getNormalFont());
        textMetro.setText(CykelsuperstierApplication.getString("marker_type_4"));
        textLocalTrain.setTypeface(CykelsuperstierApplication.getNormalFont());
        textLocalTrain.setText(CykelsuperstierApplication.getString("marker_type_5"));
    }

    private void updateOverlay(OverlayType type, boolean selected) {
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

            case S_TRAIN:
                view = findViewById(R.id.strainContainer);
                checkbox = (ImageView) findViewById(R.id.strainCheckbox);
                image = (ImageView) findViewById(R.id.strainImg);
                imageWhite = R.drawable.s_togs_icon_white;
                imageGrey = R.drawable.s_togs_icon;
                text = (TextView) findViewById(R.id.strainText);
                break;

            case METRO:
                view = findViewById(R.id.metroContainer);
                checkbox = (ImageView) findViewById(R.id.metroCheckbox);
                image = (ImageView) findViewById(R.id.metroImg);
                imageWhite = R.drawable.metro_icon_white;
                imageGrey = R.drawable.metro_icon;
                text = (TextView) findViewById(R.id.metroText);
                break;

            case LOCAL_TRAIN:
                view = findViewById(R.id.localTrainContainer);
                checkbox = (ImageView) findViewById(R.id.localTrainCheckbox);
                image = (ImageView) findViewById(R.id.localTrainImg);
                imageWhite = R.drawable.local_train_icon_white;
                imageGrey = R.drawable.local_train_icon_gray;
                text = (TextView) findViewById(R.id.localTrainText);
                break;

            default:
                return;
        }

        view.setBackgroundColor(selected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
        checkbox.setImageResource(selected ? R.drawable.check_field : R.drawable.check_in_orange);
        image.setImageResource(selected ? imageGrey : imageWhite);
        text.setTextColor(selected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
    }

}
