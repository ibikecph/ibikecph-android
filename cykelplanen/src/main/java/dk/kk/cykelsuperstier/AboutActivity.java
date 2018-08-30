// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.cykelsuperstier;

import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;

/**
 * The "About" window. Mostly inherits the functionality from the lib, but 
 * replaces the title, so it says Supercykelstier.
 * @author jens
 *
 */
public class AboutActivity extends dk.kk.ibikecphlib.AboutActivity {

	@Override
	public void onResume() {
		super.onResume();

        this.getActionBar().setTitle(IBikeApplication.getString("about_app_cp"));
        ((TextView) findViewById(R.id.textAboutText)).setText(IBikeApplication.getString("about_text_cp"));

	}

	@Override
	protected void getBuildInfo() {

	}
}
