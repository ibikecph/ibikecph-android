// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.cykelsuperstier;

import dk.kk.cykelsuperstier.BuildConfig;
import dk.kk.cykelsuperstier.R;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.util.Config;

public class CykelPlanenApplication extends IBikeApplication {

	public static final int ALARM_REQUEST_CODE = 202837;
	
	@Override
	public void onCreate() { 
		super.onCreate();
		Config.GREEN_ROUTES_ENABLED = false; 
		Config.EXTENDED_PULL_TOUCH = false;

        IBikeApplication.APP_NAME = "CykelPlanen";
        primaryColor = R.color.PrimaryColor;

		Config.generateUrls(BuildConfig.BASE_URL);
	}


    public static Class getTermsAcceptanceClass() {
        return AcceptNewTermsActivity.class;
    }

	@Override
	public boolean isDebugging() {
		return BuildConfig.DEBUG;
	}

}
