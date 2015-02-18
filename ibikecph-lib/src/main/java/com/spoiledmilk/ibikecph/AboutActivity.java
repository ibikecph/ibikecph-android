// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.ibikecph;

import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * The about view of the application.
 * @author jens
 *
 */
public class AboutActivity extends Activity {

	TextView textAboutTitle;
	TextView textAboutText;
	ImageButton btnBack;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about_activity);

		textAboutTitle = (TextView) findViewById(R.id.textAboutTitle);
		textAboutText = (TextView) findViewById(R.id.textAboutText);
		// textAboutText.setMovementMethod(new ScrollingMovementMethod());
		textAboutText.setMovementMethod(LinkMovementMethod.getInstance());
		textAboutText.setClickable(true);

		getBuildInfo();
	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		textAboutTitle.setText(IbikeApplication.getString("about_ibikecph_title"));
		textAboutTitle.setTypeface(IbikeApplication.getBoldFont());
		String text = IbikeApplication.getString("about_text");
		textAboutText.setText(text);
		// textAboutText.setTypeface(IbikeApplication.getNormalFont());
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	protected void getBuildInfo() {

		try {
			ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			zf.close();
			String s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			((TextView) findViewById(R.id.textBuild)).setText("Build: " + s);
		} catch (Exception e) {
		}

	}

}
