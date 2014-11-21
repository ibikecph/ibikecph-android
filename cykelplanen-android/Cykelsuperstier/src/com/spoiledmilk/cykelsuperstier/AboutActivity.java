// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier;

import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * The "About" window. Mostly inherits the functionality from the lib, but 
 * replaces the title, so it says Supercykelstier.
 * @author jens
 *
 */
public class AboutActivity extends com.spoiledmilk.ibikecph.AboutActivity {

	@Override
	public void onResume() {
		super.onResume();
		if (Util.getDensity() >= 2.0)
			((TextView) findViewById(R.id.textAboutTitle)).setPadding(0, Util.dp2px(14), 0, 0);
		((TextView) findViewById(R.id.textAboutTitle)).setText(IbikeApplication.getString("about_cykelsuperstier_title"));
	}

	@Override
	protected void getBuildInfo() {

	}
}
