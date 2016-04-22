// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.reminders;

import java.util.Calendar;

import com.spoiledmilk.cykelsuperstier.CykelPlanenApplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AlarmUtils {

	public static final String ALARM_ACTION = "com.spoiledmilk.cykelsuperstier.ALARM_ACTION";

	public static void setAlarm(Context ctx, int repetition) {
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(AlarmUtils.ALARM_ACTION);
		intent.putExtra("repetition", repetition);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, CykelPlanenApplication.ALARM_REQUEST_CODE + 1, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (repetition == 0)
			am.cancel(sender);
		else {
			Calendar now = Calendar.getInstance();
			now.set(Calendar.SECOND, 0);
			int day_of_week = now.get(Calendar.DAY_OF_WEEK) - 2;
			if (day_of_week == -1)
				day_of_week = 6;
			Calendar alarmTime;
			if ((repetition & (1 << day_of_week)) == 0)
				alarmTime = calculateNextAlarmTime(repetition);
			alarmTime = Calendar.getInstance();
			alarmTime.set(Calendar.HOUR_OF_DAY, 20);
			alarmTime.set(Calendar.MINUTE, 0);
			alarmTime.set(Calendar.SECOND, 0);
			if (!alarmTime.after(now))
				alarmTime = calculateNextAlarmTime(repetition);
			am.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), sender);
		}
	}

	public static void cancelAlarm(Context ctx, int repetition) {
		// cancel alarm
		AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(ALARM_ACTION);
		intent.putExtra("repetition", repetition);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, CykelPlanenApplication.ALARM_REQUEST_CODE + 1, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(sender);
	}

	public static boolean isAlarmActive(Context ctx, long id) {
		Intent intent = new Intent(ALARM_ACTION);
		PendingIntent sender = PendingIntent.getBroadcast(ctx, CykelPlanenApplication.ALARM_REQUEST_CODE + (int) id, intent,
				PendingIntent.FLAG_NO_CREATE);

		return sender != null;
	}

	public static Calendar calculateNextAlarmTime(int repetition) {
		if (repetition == 0)
			return null;
		Calendar alarmTime = Calendar.getInstance();
		alarmTime.set(Calendar.HOUR_OF_DAY, 20);
		alarmTime.set(Calendar.MINUTE, 0);
		alarmTime.set(Calendar.SECOND, 0);

		int day_of_week = alarmTime.get(Calendar.DAY_OF_WEEK) - 2;
		if (day_of_week == -1)
			day_of_week = 6;// sunday
		repetition &= 0x7f;
		int rot = (repetition >> (day_of_week + 1)) | (repetition << (7 - day_of_week - 1));
		rot &= 0x7f;
		int ndays = 0;
		for (ndays = 0; ndays < 7; ndays++) {
			if ((rot & (1 << ndays)) != 0)
				break;
		}
		alarmTime.add(Calendar.DATE, ndays + 1);

		return alarmTime;
	}
}
