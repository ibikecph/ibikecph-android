// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.reminders;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class ResetAlarmsService extends Service {

	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, final int startId) {
		super.onStartCommand(intent, flags, startId);
		AlarmUtils.setAlarm(
				this,
				PreferenceManager.getDefaultSharedPreferences(
						getApplicationContext()).getInt("alarm_repetition", 0));
		return START_NOT_STICKY;
	}
}
