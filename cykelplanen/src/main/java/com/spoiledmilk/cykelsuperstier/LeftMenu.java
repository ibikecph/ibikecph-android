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
import android.widget.LinearLayout;
import com.spoiledmilk.cykelsuperstier.favorites.AddFavoriteFragment;
import com.spoiledmilk.cykelsuperstier.map.OverlaysActivity;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenuItem;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingWelcomeActivity;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import io.realm.Realm;

public class LeftMenu extends com.spoiledmilk.ibikecph.LeftMenu {

	boolean remindersExpanded = false;
	boolean wasBtnDoneVisible = false;
	LinearLayout remindersContainer;
	LinearLayout remindersSettingsContainer;

	int repetition;
	int settingsHeight = 0;
	boolean isAnimationStarted = false;
	boolean checked1 = false, checked2 = false, checked3 = false, checked4 = false, checked5 = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View ret = super.onCreateView(inflater, container, savedInstanceState);

        // Find the ID of the "voice" menu item. Append the overlays after that.
        for (int i = 0; i<this.menuItems.size(); i++) {
            //if (this.menuItems.get(i).getLabelID().equals("voice")) {
            if (this.menuItems.get(i).getLabelID().equals("favorites")) {
                this.menuItems.add(i+1, new LeftMenuItem("map_overlays", R.drawable.ic_menu_overlays, "spawnOverlaysActivity"));
            }

            if (this.menuItems.get(i).getLabelID().equals("about_app_ibc")) {
                this.menuItems.get(i).setLabelID("about_app_cp");
            }
        }

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

		LOG.d("repetition = " + repetition);
		settingsHeight = Util.dp2px(220);
	}

	@Override
	public void initStrings() {
		super.initStrings();

	}

	@Override
	protected AddFavoriteFragment getAddFavoriteFragment() {
		return new AddFavoriteFragment();
	}


}
