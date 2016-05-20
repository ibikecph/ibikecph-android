// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The about view of the application.
 * @author jens
 *
 */
public class AboutActivity extends Activity {

	TextView textAboutText;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about_activity);

		textAboutText = (TextView) findViewById(R.id.textAboutText);

		textAboutText.setMovementMethod(LinkMovementMethod.getInstance());
		textAboutText.setClickable(true);

        this.getActionBar().setTitle(IBikeApplication.getString("about_app_ibc"));


		//getBuildInfo();
	}

	@Override
	public void onResume() {
		super.onResume();

        // Tell Google Analytics that the user has resumed on this screen.
        IBikeApplication.sendGoogleAnalyticsActivityEvent(this);

		IBikePreferences preferences = IBikeApplication.getSettings();
		CheckBox crashReportingCheckBox = (CheckBox) findViewById(R.id.crashReportingCheckBox);
		crashReportingCheckBox.setChecked(preferences.isCrashReportingEnabled());

		initStrings();
	}

	private void initStrings() {
		String text = IBikeApplication.getString("about_text_ibc");
		textAboutText.setText(text);
	}

	protected void getBuildInfo() {

		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			zf.close();
			String s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			//((TextView) findViewById(R.id.textBuild)).setText("Build: " + s);
		} catch (Exception e) {
		}

	}

}
