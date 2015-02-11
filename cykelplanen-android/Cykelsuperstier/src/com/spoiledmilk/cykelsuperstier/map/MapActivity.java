// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.map;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.LeftMenu;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.SplashActivity;
import com.spoiledmilk.cykelsuperstier.navigation.SMRouteNavigationActivity;
import com.spoiledmilk.cykelsuperstier.search.SearchActivity;
import com.spoiledmilk.ibikecph.util.LOG;
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
	boolean isPathSelected = false;
	boolean isServiceSelected = false;
	boolean isStrainSelected = false;
	boolean isMetroSelected = false;
	boolean isLocalTrainSelected = false;
	View swiperDisabledView;
	DrawerLayout drawerLayout;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		maxSlide = (int) (4 * Util.getScreenWidth() / 5);
		
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth() * 4 / 5,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

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

	// TODO: Perhaps move these to LeftMenu?
	public void onPathContainerClick(View v) {

		v.setBackgroundColor(isPathSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox1))
				.setImageResource(isPathSelected ? R.drawable.check_field : R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgPath)).setImageResource(isPathSelected ? R.drawable.bike_icon_gray : R.drawable.bike_icon_white);

		((TextView) v.findViewById(R.id.textPath)).setTextColor(isPathSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isPathSelected)
			mapFragment.overlaysManager.removeBikeRoutes();
		else
			mapFragment.overlaysManager.drawBikeRoutes(this);
		isPathSelected = !isPathSelected;
	}

	public void onServiceContainerClick(View v) {
		v.setBackgroundColor(isServiceSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox2)).setImageResource(isServiceSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgService)).setImageResource(isServiceSelected ? R.drawable.service_pump_icon_gray
				: R.drawable.service_pump_icon_white);

		Log.d("JC", Boolean.toString(v == null));

		((TextView) v.findViewById(R.id.textService)).setTextColor(isServiceSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);

		if (isServiceSelected)
			mapFragment.overlaysManager.removeServiceStations();
		else
			mapFragment.overlaysManager.drawServiceStations(this);
		isServiceSelected = !isServiceSelected;
	}

	public void onStrainContainerClick(View v) {
		v.setBackgroundColor(isStrainSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox3)).setImageResource(isStrainSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgStrain)).setImageResource(isStrainSelected ? R.drawable.s_togs_icon
				: R.drawable.s_togs_icon_white);
		((TextView) v.findViewById(R.id.textStrain)).setTextColor(isStrainSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isStrainSelected)
			mapFragment.overlaysManager.removesTrainStations();
		else
			mapFragment.overlaysManager.drawsTrainStations(this);
		isStrainSelected = !isStrainSelected;
	}

	public void onMetroContainerClick(View v) {
		v.setBackgroundColor(isMetroSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox4)).setImageResource(isMetroSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgMetro)).setImageResource(isMetroSelected ? R.drawable.metro_icon : R.drawable.metro_icon_white);
		((TextView) v.findViewById(R.id.textMetro)).setTextColor(isMetroSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isMetroSelected)
			mapFragment.overlaysManager.removeMetroStations();
		else
			mapFragment.overlaysManager.drawMetroStations(this);
		isMetroSelected = !isMetroSelected;
	}

	public void onLocalTrainContainerClick(View v) {
		v.setBackgroundColor(isLocalTrainSelected ? Color.rgb(255, 255, 255) : Color.rgb(236, 104, 0));
		((ImageView) findViewById(R.id.imgCheckbox5)).setImageResource(isLocalTrainSelected ? R.drawable.check_field
				: R.drawable.check_in_orange);
		((ImageView) findViewById(R.id.imgLocalTrain)).setImageResource(isLocalTrainSelected ? R.drawable.local_train_icon_gray
				: R.drawable.local_train_icon_white);
		((TextView) v.findViewById(R.id.textLocalTrain)).setTextColor(isLocalTrainSelected ? getResources().getColor(R.color.DarkGrey) : Color.WHITE);
		if (isLocalTrainSelected)
			mapFragment.overlaysManager.removelocalTrainStations();
		else
			mapFragment.overlaysManager.drawlocalTrainStations(this);
		isLocalTrainSelected = !isLocalTrainSelected;
	}

	@Override
	protected Class<?> getSearchActivity() {
		return SearchActivity.class;
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

	@Override
	public int getOverlaysShown() {
		int ret = 0;
		if (isPathSelected)
			ret |= 1;
		if (isServiceSelected)
			ret |= 2;
		if (isStrainSelected)
			ret |= 4;
		if (isMetroSelected)
			ret |= 8;
		if (isLocalTrainSelected)
			ret |= 16;
		return ret;
	}

}
