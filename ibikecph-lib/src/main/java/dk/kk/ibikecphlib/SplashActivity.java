// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.app.Dialog;


import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.introduction.IntroductionActivity;
import dk.kk.ibikecphlib.login.LoginSplashActivity;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import dk.kk.ibikecphlib.introduction.IntroductionActivity;
import dk.kk.ibikecphlib.login.LoginSplashActivity;
import dk.kk.ibikecphlib.util.Util;

/**
 * Splash screen. Creates any directory needed for runtime, if needed.
 * @author jens
 *
 */
public class SplashActivity extends Activity {

	int timeout = 800;
	static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;

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

		// check that Google Play Services is updated. We do this to ensure that TSL (htts) libs
		// are patched on other versions of Android. Otherwise calling https APIs might fail,
		// because the server does not accept old vulnerable protocols.
		if( checkPlayServices() ) {
			// play services are updated, ok to move on
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Class<? extends Activity> nextActivity;
					if (IBikeApplication.isUserLogedIn()) {
						nextActivity = getMapActivityClass();
					} else {
						nextActivity = getLoginActivityClass();
					}

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
	}

	protected Class<? extends Activity> getMapActivityClass() {
		return MapActivity.class;
	}

	protected Class<? extends Activity> getLoginActivityClass() {
		return LoginSplashActivity.class;
	}

	protected boolean checkPlayServices() {
		try {
			LOG.d("Checking Google Play services");
			ProviderInstaller.installIfNeeded(this);
		} catch (GooglePlayServicesRepairableException e) {
			// Indicates that Google Play services is out of date, disabled, etc.
			LOG.d("Google Play services needs to be updated, asking user to update");
			// Prompt the user to install/update/enable Google Play services.
			Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(), REQUEST_CODE_RECOVER_PLAY_SERVICES);
			dialog.setCancelable(false);	// don't let user dismiss dialog
			dialog.show();
			return false;

		} catch (GooglePlayServicesNotAvailableException e) {
			// Indicates a non-recoverable error; the ProviderInstaller is not able
			// to install an up-to-date Provider.

			// without an updated google play services,
			// https call might fail if we're on an old android that only supports
			// insecure protocols.

			LOG.d("Google Play services could not be updated");
			finish();		// quit
			return false;
		}

		// If this is reached, you know that the provider was already up-to-date,
		// or was successfully updated.
		LOG.d("Google Play services are up to date");
		return true;
	}

}
