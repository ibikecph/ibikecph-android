// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.spoiledmilk.cykelsuperstier.login.LoginSplashActivity;
import com.spoiledmilk.cykelsuperstier.map.MapActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Util;

public class SplashActivity extends com.spoiledmilk.ibikecph.SplashActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		Util.init(getWindowManager());
		// Log.d("SplashActivity", "Created CykelPlanen's SplashActivity");
	}

	@Override
	protected Class<? extends Activity> getMapActivityClass() {
		return MapActivity.class;
	}

	@Override
	protected Class<? extends Activity> getLoginActivityClass() {
		return LoginSplashActivity.class;
	}

}
