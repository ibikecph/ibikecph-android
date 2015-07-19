// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.spoiledmilk.cykelsuperstier.map.OverlaysActivity;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenuItem;
import com.spoiledmilk.ibikecph.LeftMenuItemAdapter;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingWelcomeActivity;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import io.realm.Realm;

import java.util.ArrayList;

public class LeftMenu extends com.spoiledmilk.ibikecph.LeftMenu {
	int settingsHeight = 0;

    private ArrayList<LeftMenuItem> cpMenuItems = new ArrayList<LeftMenuItem>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("JC", "CP LeftMenu onCreate");

        // TODO: Technically we should be able to just do everything we need on menuItems directly.
        cpMenuItems.addAll(this.menuItems);

        // Find the ID of the "voice" menu item. Append the overlays after that.
        for (int i = 0; i<this.cpMenuItems.size(); i++) {
            if (this.cpMenuItems.get(i).getLabelID().equals("favorites")) {
                Log.d("JC", "Adding overlays menu");
                this.cpMenuItems.add(i+1, new LeftMenuItem("map_overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
            }

            if (this.cpMenuItems.get(i).getLabelID().equals("about_app_ibc")) {
                this.cpMenuItems.get(i).setLabelID("about_app_cp");
            }
        }


    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View ret = super.onCreateView(inflater, container, savedInstanceState);

        Log.d("JC", "CP LeftMenu onCreateView");
        this.menuList.setAdapter(new LeftMenuItemAdapter(IbikeApplication.getContext(), cpMenuItems));
        super.menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String handler = cpMenuItems.get(position).getHandler();
                spawnFunction(handler);
            }
        });

		return ret;
	}

	@Override
	public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), com.spoiledmilk.cykelsuperstier.AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	public void spawnTTSSettingsActivity() {
		Log.i("JC", "CP: Spawning TTS settings");
		super.spawnTTSSettingsActivity();
	}


	public void spawnOverlaysActivity() {
		Log.d("JC", "CP: Spawning overlays");
        Intent i = new Intent(getActivity(), OverlaysActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

	public void spawnFavoritesListActivity() {
		Log.i("JC", "CP: Spawning Favorites settings");
		super.spawnFavoritesListActivity();
	}

	public void spawnLoginActivity() {
		super.spawnLoginActivity();
	}

    public void spawnTrackingActivity() {
        Intent i;
        IbikePreferences settings = IbikeApplication.getSettings();
        if (!settings.getTrackingEnabled() &&
                Realm.getInstance(IbikeApplication.getContext()).allObjects(Track.class).size() == 0) {
            i = new Intent(getActivity(), TrackingWelcomeActivity.class);
        } else {
            i = new Intent(getActivity(), TrackingActivity.class);
        }
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void initStrings() {
		super.initStrings();

	}

}
