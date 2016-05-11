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

    @Override
    public void populateMenu() {
        super.populateMenu();
        // Find the ID of the "voice" menu item. Append the overlays after that.
        for (LeftMenuItem item: menuItems) {
            if (item.getLabelID().equals("favorites")) {
                Log.d("JC", "Adding overlays menu");
                //this.cpMenuItems.add(i+1, new LeftMenuItem("map_overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
            }
            if (item.getLabelID().equals("about_app_ibc")) {
                item.setLabelID("about_app_cp");
            }
        }
    }

    /*
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
	*/

	@Override
	public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), com.spoiledmilk.cykelsuperstier.AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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

}
