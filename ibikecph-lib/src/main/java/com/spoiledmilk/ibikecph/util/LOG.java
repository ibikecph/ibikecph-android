// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import android.util.Log;

public class LOG {

	private static String tag = "I Bike CPH";

	public static void turnLogOn() {
		Config.LOG_ENABLED = true;
	}

	public static void turnLogOff() {
		Config.LOG_ENABLED = false;
	}

	public static int d(String msg) {
		if (Config.LOG_ENABLED) {
			return Log.d(tag, msg);
		} else {
			return -1;
		}
	}

	public static int fd(String msg) {
		return Log.d(tag, msg);
	}

	public static int e(String msg) {
		if (msg != null) {
			return Log.e(tag, msg);
		} else {
			return Log.e(tag, "");
		}
	}

	public static int e(String msg, Throwable t) {
		return Log.e(tag, msg, t);
	}

	public static int i(String msg) {
		return Log.i(tag, msg);
	}

	public static int w(String msg) {
		return Log.w(tag, msg);
	}

	public static int w(String msg, Throwable tr) {
		return Log.w(tag, msg, tr);
	}
}
