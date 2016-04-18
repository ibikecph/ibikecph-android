// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.spoiledmilk.ibikecph.login.LoginSplashActivity;
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

		// new CopyTilesThread().start();

	}

	@Override
	public void onResume() {
		super.onResume();

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent i;
				if (IbikeApplication.isUserLogedIn()) {
					i = new Intent(SplashActivity.this, MapActivity.class);
				} else {
					i = new Intent(SplashActivity.this, getLoginActivityClass());
				}
				startActivity(i);
				finish();
			}
		}, timeout);

	}

	protected Class<?> getLoginActivityClass() {
		return LoginSplashActivity.class;
	}


	class CopyTilesThread extends Thread {

		@Override
		public void run() {
			AssetManager assetManager = getAssets();
			String[] files = null;
			try {
				files = assetManager.list("iBikeTiles/12");
			} catch (IOException e) {
				Log.e("tag", "Failed to get asset file list.", e);
			}
			for (String filename : files) {
				InputStream in = null;
				OutputStream out = null;
				try {
					in = assetManager.open("iBikeTiles/12/" + filename);
					final File osmDir = new File(Environment.getExternalStorageDirectory(), "osmdroid");
					if (!osmDir.exists()) {
						osmDir.mkdir();
					}
					final File tilesDir = new File(osmDir, "tiles");
					if (!tilesDir.exists()) {
						tilesDir.mkdir();
					}
					final File iBikeDir = new File(tilesDir, "IBikeCPH");
					if (!iBikeDir.exists()) {
						iBikeDir.mkdir();
					}
					final File dir = new File(iBikeDir, "12");
					if (!dir.exists()) {
						dir.mkdir();
					}
					String dirName = filename.split(" ")[0];
					final File dir1 = new File(dir, dirName);
					if (!dir1.exists()) {
						dir1.mkdir();
					}
					String name = filename.split(" ")[1] + ".tile";
					File outFile = new File(dir1, name);
					if (!outFile.exists()) {
						outFile.createNewFile();
						out = new FileOutputStream(outFile);
						copyFile(in, out);
						in.close();
						in = null;
						out.flush();
						out.close();
						out = null;
					}
				} catch (IOException e) {
					Log.e("tag", "Failed to copy asset file: " + filename, e);
				}
			}
		}

		private void copyFile(InputStream in, OutputStream out) throws IOException {
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}

}
