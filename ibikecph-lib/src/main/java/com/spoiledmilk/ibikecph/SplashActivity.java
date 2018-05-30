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

import com.spoiledmilk.ibikecph.introduction.IntroductionActivity;
//import com.spoiledmilk.ibikecph.login.LoginSplashActivity;
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

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Class<? extends Activity> nextActivity;
				//if (IBikeApplication.isUserLogedIn()) {
				nextActivity = getMapActivityClass();
				//} else {
				//	nextActivity = getLoginActivityClass();
				//}

				// First - let's launch the map activity class or login activity.
				Intent i = new Intent(SplashActivity.this, nextActivity);
				startActivity(i);

				// Then, let's launch all relevant introduction activities
				IntroductionActivity.startIntroductionActivities(SplashActivity.this);

				// Finally - let's kill this SplashActivity.
				finish();
			}
		}, timeout);

	}

	protected Class<? extends Activity> getMapActivityClass() {
		return MapActivity.class;
	}

	/*protected Class<? extends Activity> getLoginActivityClass() {
		return LoginSplashActivity.class;
	}*/

}
