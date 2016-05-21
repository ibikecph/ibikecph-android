// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.util.Config;

public class CykelPlanenApplication extends IBikeApplication {

	public static final int ALARM_REQUEST_CODE = 202837;
	
	@Override
	public void onCreate() { 
		super.onCreate();
		Config.GREEN_ROUTES_ENABLED = false; 
		Config.EXTENDED_PULL_TOUCH = false;

        IBikeApplication.APP_NAME = "CykelPlanen";
        this.primaryColor = R.color.CPActionBar;

		initializeGoogleAnalytics(R.xml.global_tracker);

		Config.generateUrls(BuildConfig.API_URL);
	}


    public static Class getTermsAcceptanceClass() {
        return com.spoiledmilk.cykelsuperstier.AcceptNewTermsActivity.class;
    }

	@Override
	public boolean isDebugging() {
		return BuildConfig.DEBUG;
	}

	@Override
	public boolean breakRouteIsEnabled() {
		return getResources().getBoolean(R.bool.breakRouteEnabled);
	}

}
