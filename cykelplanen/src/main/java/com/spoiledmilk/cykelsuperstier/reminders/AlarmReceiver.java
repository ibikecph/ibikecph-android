// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.reminders;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.spoiledmilk.cykelsuperstier.CykelPlanenApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.SplashActivity;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		if (getAbortBroadcast()) {
			return;
		}
		int repetition = intent.getExtras().getInt("repetition");
		if (repetition >= 0) {
			// Find next alarm time.
			// If repetition is EVERY_DAY no need to set again, reminder was
			// initially set repetitive.+
			Calendar nextAlarmTime = AlarmUtils.calculateNextAlarmTime(repetition);
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent newIntent = new Intent(AlarmUtils.ALARM_ACTION);
			newIntent.putExtra("repetition", repetition);
			PendingIntent sender = PendingIntent.getBroadcast(context, CykelPlanenApplication.ALARM_REQUEST_CODE + 1, newIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			am.set(AlarmManager.RTC_WAKEUP, nextAlarmTime.getTimeInMillis(), sender);
		}

		createNotification(context, repetition);
	}

	private void createNotification(Context context, int repetition) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent intent = new Intent(context, SplashActivity.class);
		intent.setData(Uri.fromParts("cykelsuperstier", "custom", "1"));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
		String description = CykelPlanenApplication.getString("reminder_alert_text");
		builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher)
				.setTicker(context.getResources().getString(R.string.app_name) + " " + description).setWhen(System.currentTimeMillis())
				.setAutoCancel(true).setContentText(description).setContentTitle(context.getResources().getString(R.string.app_name));
		builder.setStyle(new NotificationCompat.InboxStyle());
		Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (alarmSound == null) {
			alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		}
		builder.setSound(alarmSound);
		Notification n = builder.build();
		notificationManager.notify(1, n);
	}

}
