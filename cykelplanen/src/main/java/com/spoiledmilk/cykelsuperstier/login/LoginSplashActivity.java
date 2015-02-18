// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.login;

import java.util.ArrayList;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.map.MapActivity;
import com.spoiledmilk.cykelsuperstier.reminders.RemindersSplashActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.util.DB;

public class LoginSplashActivity extends com.spoiledmilk.ibikecph.login.LoginSplashActivity {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// findViewById(R.id.registerContainer).setOnTouchListener(new OnTouchListener() {
		// @Override
		// public boolean onTouch(View arg0, MotionEvent event) {
		// if (event.getAction() == MotionEvent.ACTION_DOWN) {
		// findViewById(R.id.btnRegister).setBackgroundColor(getButtonPressedColor());
		// final Handler handler = new Handler();
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// try {
		// findViewById(R.id.btnRegister).setBackgroundColor(Color.WHITE);
		// } catch (Exception e) {
		//
		// }
		// }
		// }, 500);
		// }
		// return false;
		// }
		// });
		//
		// findViewById(R.id.skipContainer).setOnTouchListener(new OnTouchListener() {
		// @Override
		// public boolean onTouch(View arg0, MotionEvent event) {
		// if (event.getAction() == MotionEvent.ACTION_DOWN) {
		// findViewById(R.id.btnSkip).setBackgroundColor(getButtonPressedColor());
		// final Handler handler = new Handler();
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// try {
		// findViewById(R.id.btnSkip).setBackgroundColor(Color.WHITE);
		// } catch (Exception e) {
		//
		// }
		// }
		// }, 500);
		// }
		// return false;
		// }
		// });
	}

	@Override
	protected int getLayoutId() {
		return R.layout.login_splash_activity;
	}

	protected int getButtonPressedColor() {
		return Color.LTGRAY;
	}

	@Override
	public void onResume() {
		super.onResume();
		((TextView) findViewById(R.id.textSkip)).setTextColor(Color.rgb(243, 109, 0));
		((TextView) findViewById(R.id.textRegister)).setTextColor(Color.rgb(243, 109, 0));
		// ((TextView) findViewById(R.id.textHeadline)).setTextColor(Color.rgb(67, 67, 67));
		// ((TextView) findViewById(R.id.textHeadline)).setTypeface(CykelsuperstierApplication.getBoldFont());
		// ((TextView) findViewById(R.id.textHeadline)).setText(CykelsuperstierApplication.getString("quickly"));
	}

	@Override
	public void launchMainMapActivity(String auth_token, int id) {
		findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		PreferenceManager.getDefaultSharedPreferences(this).edit().putString("auth_token", auth_token).commit();
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("id", id).commit();
		// if
		// (!PreferenceManager.getDefaultSharedPreferences(this).contains("reminders_shown"))
		// {
		// Intent i = new Intent(this, RemindersSplashActivity.class);
		// startActivity(i);
		// overridePendingTransition(R.anim.slide_in_right,
		// R.anim.slide_out_left);
		// finish();
		// } else {
		new Thread(new Runnable() {
			@Override
			public void run() {
				DB db = new DB(LoginSplashActivity.this);
				ArrayList<FavoritesData> favorites = db.getFavoritesFromServer(LoginSplashActivity.this, null);
				if (favorites == null || favorites.size() == 0) {
					LoginSplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!PreferenceManager.getDefaultSharedPreferences(LoginSplashActivity.this).contains("reminders_shown")) {
								Intent i = new Intent(LoginSplashActivity.this, RemindersSplashActivity.class);
								startActivity(i);
								overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
								finish();
							} else {
								Intent i = new Intent(LoginSplashActivity.this, MapActivity.class);
								LoginSplashActivity.this.startActivity(i);
								overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
								finish();
							}
						}
					});
				} else {
					LoginSplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							PreferenceManager.getDefaultSharedPreferences(LoginSplashActivity.this).edit()
									.putBoolean("reminders_shown", true).commit();
							findViewById(R.id.progressBar).setVisibility(View.GONE);
							launchMainMapActivity();
						}
					});
				}
			}
		}).start();
		// }
	}

	@Override
	public void launchMainMapActivity() {
		if (!PreferenceManager.getDefaultSharedPreferences(this).contains("reminders_shown")) {
			Intent i = new Intent(this, RemindersSplashActivity.class);
			startActivity(i);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			finish();
		} else {
			Intent i = new Intent(LoginSplashActivity.this, MapActivity.class);
			LoginSplashActivity.this.startActivity(i);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			finish();
		}
	}

}
