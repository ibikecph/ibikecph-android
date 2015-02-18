// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.reminders;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.cykelsuperstier.BreakRouteSplashActivity;
import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.favorites.FavoritesActivity;
import com.spoiledmilk.cykelsuperstier.map.MapActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;

public class RemindersSplashActivity extends Activity {

	TextView textTitle;
	TextView textReminders;
	TextView textMonday;
	TextView textTuesday;
	TextView textWednesday;
	TextView textThursday;
	TextView textFriday;
	Button btnSave;
	Button btnSkip;
	boolean switch1 = false;
	boolean switch2 = false;
	boolean switch3 = false;
	boolean switch4 = false;
	boolean switch5 = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reminders_splash);
		textTitle = (TextView) findViewById(R.id.textTitle);
		textReminders = (TextView) findViewById(R.id.textReminders);
		textMonday = (TextView) findViewById(R.id.textMonday);
		textTuesday = (TextView) findViewById(R.id.textTuesday);
		textWednesday = (TextView) findViewById(R.id.textWednesday);
		textThursday = (TextView) findViewById(R.id.textThursday);
		textFriday = (TextView) findViewById(R.id.textFriday);
		btnSave = (Button) findViewById(R.id.btnSave);
		btnSkip = (Button) findViewById(R.id.btnSkip);

		((ImageView) findViewById(R.id.toggleButton1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch1 = !switch1;
				((ImageView) findViewById(R.id.toggleButton1)).setImageResource(switch1 ? R.drawable.switch_on : R.drawable.switch_off);
			}
		});
		((ImageView) findViewById(R.id.toggleButton2)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch2 = !switch2;
				((ImageView) findViewById(R.id.toggleButton2)).setImageResource(switch2 ? R.drawable.switch_on : R.drawable.switch_off);
			}
		});
		((ImageView) findViewById(R.id.toggleButton3)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch3 = !switch3;
				((ImageView) findViewById(R.id.toggleButton3)).setImageResource(switch3 ? R.drawable.switch_on : R.drawable.switch_off);
			}
		});
		((ImageView) findViewById(R.id.toggleButton4)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch4 = !switch4;
				((ImageView) findViewById(R.id.toggleButton4)).setImageResource(switch4 ? R.drawable.switch_on : R.drawable.switch_off);
			}
		});
		((ImageView) findViewById(R.id.toggleButton5)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch5 = !switch5;
				((ImageView) findViewById(R.id.toggleButton5)).setImageResource(switch5 ? R.drawable.switch_on : R.drawable.switch_off);
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	private void initStrings() {
		textTitle.setTypeface(CykelsuperstierApplication.getBoldFont());
		textTitle.setText(CykelsuperstierApplication.getString("reminder_title"));
		textReminders.setTypeface(CykelsuperstierApplication.getNormalFont());
		textReminders.setText(CykelsuperstierApplication.getString("reminder_text"));
		textMonday.setTypeface(CykelsuperstierApplication.getNormalFont());
		textMonday.setText(CykelsuperstierApplication.getString("monday"));
		textTuesday.setTypeface(CykelsuperstierApplication.getNormalFont());
		textTuesday.setText(CykelsuperstierApplication.getString("tuesday"));
		textWednesday.setTypeface(CykelsuperstierApplication.getNormalFont());
		textWednesday.setText(CykelsuperstierApplication.getString("wednesday"));
		textThursday.setTypeface(CykelsuperstierApplication.getNormalFont());
		textThursday.setText(CykelsuperstierApplication.getString("thursday"));
		textFriday.setTypeface(CykelsuperstierApplication.getNormalFont());
		textFriday.setText(CykelsuperstierApplication.getString("friday"));
		btnSave.setTypeface(CykelsuperstierApplication.getBoldFont());
		btnSave.setText(CykelsuperstierApplication.getString("reminder_save_btn"));
		btnSkip.setTypeface(CykelsuperstierApplication.getBoldFont());
		btnSkip.setText(CykelsuperstierApplication.getString("skip"));
	}

	public void onBtnSkipClick(View v) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("reminders_shown", true).commit();
		if (CykelsuperstierApplication.isUserLogedIn())
			checkFavorites();
		else {
			Intent i = new Intent(RemindersSplashActivity.this, BreakRouteSplashActivity.class);
			RemindersSplashActivity.this.startActivity(i);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			finish();
		}
	}

	public void onBtnSaveClick(View v) {
		int repetition = 0;
		if (switch1)
			repetition = repetition | 64;
		if (switch2)
			repetition = repetition | 1;
		if (switch3)
			repetition = repetition | 2;
		if (switch4)
			repetition = repetition | 4;
		if (switch5)
			repetition = repetition | 8;
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("alarm_repetition", repetition).commit();
		AlarmUtils.setAlarm(this, repetition);
		LOG.d("repetition = " + repetition);
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("reminders_shown", true).commit();
		if (CykelsuperstierApplication.isUserLogedIn())
			checkFavorites();
		else {
			Intent i = new Intent(RemindersSplashActivity.this, BreakRouteSplashActivity.class);
			RemindersSplashActivity.this.startActivity(i);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			finish();
		}
	}

	private void checkFavorites() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				DB db = new DB(RemindersSplashActivity.this);
				ArrayList<FavoritesData> favorites = db.getFavoritesFromServer(RemindersSplashActivity.this, null);
				if (favorites == null || favorites.size() == 0) {
					RemindersSplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Intent i = new Intent(RemindersSplashActivity.this, FavoritesActivity.class);
							startActivity(i);
							overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
							finish();
						}
					});
				} else {
					RemindersSplashActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							launchMainMapActivity();
						}
					});
				}
			}
		}).start();
	}

	public void launchMainMapActivity() {
		Intent i = new Intent(RemindersSplashActivity.this, MapActivity.class);
		RemindersSplashActivity.this.startActivity(i);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		finish();
	}

}
