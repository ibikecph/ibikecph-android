// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.spoiledmilk.cykelsuperstier.LeftMenu;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.SplashActivity;
import com.spoiledmilk.cykelsuperstier.navigation.SMRouteNavigationActivity;
import com.spoiledmilk.cykelsuperstier.search.SearchActivity;
import com.spoiledmilk.ibikecph.IBikeCPHApplication;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.map.OverlayType;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.Util;

public class MapActivity extends com.spoiledmilk.ibikecph.map.MapActivity {

	// private static final String HOCKEY_APP_ID =
	// "a678431adeb2e89877a2bac70a1a0bba";

	TranslateAnimation animation;
	float posX = 0;
	float touchX = 0;
	int maxSlide = 0;
	boolean slidden = false;
	int moveCount = 0;
	LinearLayout stationsContainer;
	View swiperDisabledView;
	DrawerLayout drawerLayout;
    SharedPreferences.OnSharedPreferenceChangeListener overlayListener;

    boolean drawnPath,
            drawnService,
            drawnStrain,
            drawnMetro,
            drawnLocalTrain;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		maxSlide = (int) (4 * Util.getScreenWidth() / 5);

		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth() * 4 / 5,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        drawnPath = drawnService = drawnStrain = drawnMetro = drawnLocalTrain = false;

        // overlay listener
        SharedPreferences prefs = IbikeApplication.getSettings().getPrefs();
        overlayListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences p, String key) {
                IbikePreferences settings = IbikeApplication.getSettings();

                System.out.println(String.format("listener: %s", key));

                if (key.equals(settings.getPrefOverlayKey(OverlayType.PATH))) {
                    updateOverlay(OverlayType.PATH);
                } else if (key.equals(settings.getPrefOverlayKey(OverlayType.SERVICE))) {
                    updateOverlay(OverlayType.SERVICE);
                } else if (key.equals(settings.getPrefOverlayKey(OverlayType.S_TRAIN))) {
                    updateOverlay(OverlayType.S_TRAIN);
                } else if (key.equals(settings.getPrefOverlayKey(OverlayType.METRO))) {
                    updateOverlay(OverlayType.METRO);
                } else if (key.equals(settings.getPrefOverlayKey(OverlayType.LOCAL_TRAIN))) {
                    updateOverlay(OverlayType.LOCAL_TRAIN);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(overlayListener);
	}

	private boolean isSlidden() {
		return super.slidden;
	}

	@Override
	public com.spoiledmilk.ibikecph.LeftMenu getLeftMenu() {
        return (leftMenu == null ? new LeftMenu() : leftMenu);
	}

	@Override
	protected Class<?> getNavigationClass() {
		return SMRouteNavigationActivity.class;
	}

    @Override
    public void onStart() {
        super.onStart();

        initOverlays();
    }

	@Override
	public void onResume() {
		super.onResume();

		reloadStrings();
	}

	@Override
	protected void checkForCrashes() {
		try {
			// CrashManager.register(this, HOCKEY_APP_ID);
		} catch (Exception e) {

		}
	}

    @Override
	protected Class<?> getSplashActivityClass() {
		return SplashActivity.class;
	}

	@Override
	public void reloadStrings() {
		super.reloadStrings();
	}

	@Override
	protected Class<?> getSearchActivity() {
		return SearchActivity.class;
	}

	@Override
	public int getOverlaysShown() {
        IbikePreferences settings = IbikeApplication.getSettings();
		int ret = 0;
		if (settings.getOverlay(OverlayType.PATH))
			ret |= 1;
		if (settings.getOverlay(OverlayType.SERVICE))
			ret |= 2;
		if (settings.getOverlay(OverlayType.S_TRAIN))
			ret |= 4;
		if (settings.getOverlay(OverlayType.METRO))
			ret |= 8;
		if (settings.getOverlay(OverlayType.LOCAL_TRAIN))
			ret |= 16;
		return ret;
	}

    private void initOverlays() {
        updateOverlay(OverlayType.PATH);
        updateOverlay(OverlayType.SERVICE);
        updateOverlay(OverlayType.S_TRAIN);
        updateOverlay(OverlayType.METRO);
        updateOverlay(OverlayType.LOCAL_TRAIN);
    }

    synchronized private void updateOverlay(OverlayType type) {
        if (mapFragment.overlaysManager == null) return;

        System.out.println(String.format("upate: %s", type));


        IbikePreferences settings = IbikeApplication.getSettings();

        switch (type) {
            case PATH:
                if (settings.getOverlay(type)) {
                    if (drawnPath) return;
                    mapFragment.overlaysManager.drawBikeRoutes(this);
                    drawnPath = true;
                } else {
                    mapFragment.overlaysManager.removeBikeRoutes();
                    drawnPath = false;
                }
                break;

            case SERVICE:
                if (settings.getOverlay(type)) {
                    if (drawnService) return;
                    mapFragment.overlaysManager.drawServiceStations(this);
                    drawnService = true;
                } else {
                    mapFragment.overlaysManager.removeServiceStations();
                    drawnService = false;
                }
                break;

            case S_TRAIN:
                if (settings.getOverlay(type)) {
                    if (drawnStrain) return;
                    mapFragment.overlaysManager.drawsTrainStations(this);
                    drawnStrain = true;
                } else {
                    mapFragment.overlaysManager.removesTrainStations();
                    drawnStrain = false;
                }
                break;

            case METRO:
                if (settings.getOverlay(type)) {
                    if (drawnMetro) return;
                    mapFragment.overlaysManager.drawMetroStations(this);
                    drawnMetro = true;
                } else {
                    mapFragment.overlaysManager.removeMetroStations();
                    drawnMetro = false;
                }
                break;

            case LOCAL_TRAIN:
                if (settings.getOverlay(type)) {
                    if (drawnLocalTrain) return;
                    mapFragment.overlaysManager.drawlocalTrainStations(this);
                    drawnLocalTrain = true;
                } else {
                    mapFragment.overlaysManager.removelocalTrainStations();
                    drawnLocalTrain = false;
                }
                break;

            default:
                break;
        }
    }

}
