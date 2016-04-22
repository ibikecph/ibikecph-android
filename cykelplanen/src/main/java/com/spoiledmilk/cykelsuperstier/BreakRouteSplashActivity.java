// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.spoiledmilk.cykelsuperstier.map.MapActivity;
import com.spoiledmilk.ibikecph.util.Util;

public class BreakRouteSplashActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Util.getScreenHeight() >= 800)
			setContentView(R.layout.activity_break_route_splash);
		else
			setContentView(R.layout.activity_break_route_splash_small);
	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		TextView textTitle = (TextView) findViewById(R.id.textTitle);
		textTitle.setTypeface(CykelPlanenApplication.getBoldFont());
		textTitle.setTextColor(Color.rgb(243, 109, 0));
		textTitle.setText(CykelPlanenApplication.getString("break_route_title"));

		TextView textBreakRoute = (TextView) findViewById(R.id.textBreakRoute);
		textBreakRoute.setTextColor(Color.rgb(131, 131, 131)); // 60, 60, 60
		textBreakRoute.setText(CykelPlanenApplication.getString("break_route_text_top"));

		TextView textBreakRouteHint = (TextView) findViewById(R.id.textBreakRouteHint);
		textBreakRouteHint.setTextColor(Color.rgb(131, 131, 131));
		textBreakRouteHint.setText(CykelPlanenApplication.getString("break_route_text_bottom"));

		TextView textSkip = (TextView) findViewById(R.id.textSkip);
		textSkip.setTypeface(CykelPlanenApplication.getBoldFont());
		textSkip.setTextColor(Color.rgb(243, 109, 0));
		textSkip.setText(CykelPlanenApplication.getString("skip"));
	}

	public void onBtnSkipClick(View view) {
		Intent i = new Intent(this, MapActivity.class);
		startActivity(i);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		finish();
	}
}
