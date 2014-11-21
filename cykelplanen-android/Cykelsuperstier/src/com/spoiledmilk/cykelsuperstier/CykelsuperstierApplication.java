// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.util.Config;

public class CykelsuperstierApplication extends IbikeApplication {

	public static final int ALARM_REQUEST_CODE = 202837;
	
	@Override
	public void onCreate() { 
		super.onCreate();
		Config.GREEN_ROUTES_ENABLED = false; 
		Config.EXTENDED_PULL_TOUCH = false;
	}

}
