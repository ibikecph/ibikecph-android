// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.spoiledmilk.ibikecph.login.LoginSplashActivity;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.util.Util;
/**
 * Splash screen. Creates any directory needed for runtime, if needed.
 * @author jens
 *
 */
public class SplashActivity extends Activity {

	int timeout = 800;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		Util.init(getWindowManager());

		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("timeout")) {
			timeout = getIntent().getExtras().getInt("timeout");
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		// Tell Google Analytics that the user has resumed on this screen.
		IBikeApplication.sendGoogleAnalyticsActivityEvent(this);

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent i;
				if (IBikeApplication.isUserLogedIn()) {
					i = new Intent(SplashActivity.this, getMapActivityClass());
				} else {
					i = new Intent(SplashActivity.this, getLoginActivityClass());
				}
				startActivity(i);
				finish();
			}
		}, timeout);

	}

	protected Class<?> getMapActivityClass() {
		return MapActivity.class;
	}

	protected Class<?> getLoginActivityClass() {
		return LoginSplashActivity.class;
	}

}
