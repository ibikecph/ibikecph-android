// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.ibikecph.map.overlays;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;

import java.util.List;

/**
 * Activity for toggling station overlays on map.
 * @author markus
 *
 */
public class OverlaysActivity extends Activity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(IBikeApplication.getString("map_overlays"));

        setContentView(R.layout.overlays_activity);

        // Initializes the list of overlays
        List<SelectableOverlay> overlays = SelectableOverlayFactory.getInstance().getSelectableOverlays();
        ListView overlaysList = (ListView) findViewById(R.id.overlaysList);
        ListAdapter overlaysListAdapter = new ArrayAdapter<SelectableOverlay>(this, R.layout.overlays_list_item, overlays) {
            LayoutInflater inflater = LayoutInflater.from(getContext());

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                final SelectableOverlay overlay = getItem(position);
                if(view == null) {
                    view = inflater.inflate(R.layout.overlays_list_item, parent, false);
                }

                TextView name = (TextView) view.findViewById(R.id.overlayName);
                name.setText(overlay.getName());

                SelectableOverlayIcon icon = (SelectableOverlayIcon) view.findViewById(R.id.overlayIcon);
                icon.setOverlay(overlay);

                // Set a colored stroke on an icon instead, using the overlay.getColor();

                final CheckBox checkBox = (CheckBox) view.findViewById(R.id.overlayCheckBox);
                checkBox.setOnCheckedChangeListener(null); // It might have been connected before
                checkBox.setChecked(overlay.isSelected());
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d("OverlaysActivity", "onCheckedChanged called on " + overlay.getName() + " with " + isChecked);
                        overlay.setSelected(isChecked);
                    }
                });

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkBox.toggle();
                    }
                });

                return view;
            }

        };

        // Register the adapter on the activity's primary list view
        overlaysList.setAdapter(overlaysListAdapter);
    }



    @Override
    public void onResume() {
        super.onResume();
        // Tell Google Analytics that the user has resumed on this screen.
        IBikeApplication.sendGoogleAnalyticsActivityEvent(this);
    }
}
