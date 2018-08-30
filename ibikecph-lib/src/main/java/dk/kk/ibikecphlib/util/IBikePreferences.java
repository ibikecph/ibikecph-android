// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.map.overlays.TogglableOverlay;

import java.util.Locale;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.map.overlays.TogglableOverlay;

public class IBikePreferences {
    public static final boolean DEBUGMODE = true;

    public static boolean REMEMBER_MAP_STATE = false;

    public static final String PREFS_TILE_SOURCE      = "tilesource";
    public static final String PREFS_SCROLL_X         = "scrollX";
    public static final String PREFS_SCROLL_Y         = "scrollY";
    public static final String PREFS_ZOOM_LEVEL       = "zoomLevel";
    public static final String PREFS_SHOW_LOCATION    = "showLocation";
    public static final String PREFS_SHOW_COMPASS     = "showCompass";
    public static final String PREFS_LANGUAGE         = "language";
    public static final String PREFS_OVERLAYS         = "overlays";
    public static final String PREFS_TRACKING_ENABLED = "trackingEnabled";
    public static final String PREFS_NOTIFY_MILESTONE = "notifyMilestone";
    public static final String PREFS_NOTIFY_WEEKLY    = "notifyWeekly";
    public static final String PREFS_SHARE_DATA       = "shareData";
    public static final String CRASH_REPORTING = "crashReporting";
    public static final String LENGTH_NOTIFICATION    = "lengthNotification";
    public static final String STREAK_NOTIFICATION    = "streakNotification";
    public static final String NEWEST_TERMS_ACCEPTED  = "newest_terms_accepted";
    public static final String READ_ALOUD             = "read_aloud";

    public static final int ROUTE_COLOR = Color.rgb(0, 174, 239);
    public static final float ROUTE_STROKE_WIDTH = 10.0f;
    public static final int ROUTE_ALPHA = 0xc0;
    public static final int ROUTE_DIMMED_COLOR = Color.LTGRAY;

    public static final float DEFAULT_ZOOM_LEVEL = 17.0f;
    private int newestTermsAccepted;


    public enum Language {
        UNDEFINED, ENG, DAN
    }

    public Language language = Language.UNDEFINED;

    private Context context;

    public IBikePreferences(Context context) {
        this.context = context.getApplicationContext();
    }

    public void load() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // If setting doesn't exists try to use system setting; if language is
        // not supported - use English.
        language = Language.values()[prefs.getInt(PREFS_LANGUAGE, 0)];
        if (language == Language.UNDEFINED) {
            String lng = Locale.getDefault().getISO3Language();
            if (lng.equals("dan"))
                language = Language.DAN;
            else
                language = Language.ENG;
        }
    }

    public Language getLanguage() {
        return language;
    }

    public String getLanguageName() {
        return getLanguageNames()[language.ordinal()];
    }

    public void setLanguage(Language language) {
        if (this.language != language) {
            this.language = language;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putInt(PREFS_LANGUAGE, language.ordinal()).commit();

            // for (OnSettingsChangeListener listener : listeners)
            // listener.onLanguageChange(language);
        }
    }

    public static String[] getLanguageNames() {
        String[] languageNames = new String[2];
        languageNames[0] = IBikeApplication.getString("language_eng");
        languageNames[1] = IBikeApplication.getString("language_dan");
        return languageNames;
    }

    public void setTrackingEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(PREFS_TRACKING_ENABLED, enabled).commit();
        // Check if the app is build with the tracking enabled.
        boolean trackingEnabled = context.getResources().getBoolean(R.bool.trackingEnabled);
        if (trackingEnabled) {
            // Make sure the user's choice is immediately respected.
            if (enabled) {
                Log.d("DV", "tracking sat til true");
                //IBikeApplication.getService().getActivityRecognitionClient().setTracking(true);
                IBikeApplication.getService().getActivityRecognitionClient().requestActivityUpdates();
            } else {
                Log.d("DV", "tracking released");
                IBikeApplication.getService().getActivityRecognitionClient().releaseActivityUpdates();
            }
        }
    }

    public boolean getTrackingEnabled() {
        boolean trackingEnabled = context.getResources().getBoolean(R.bool.trackingEnabled);
        return trackingEnabled && getPreferences().getBoolean(PREFS_TRACKING_ENABLED, false);
    }

    public void setNotifyMilestone(boolean notifyMilestone) {
        getPreferences().edit().putBoolean(PREFS_NOTIFY_MILESTONE, notifyMilestone).commit();
    }

    public boolean getNotifyMilestone() {
        return getPreferences().getBoolean(PREFS_NOTIFY_MILESTONE, true);
    }

    public void setNotifyWeekly(boolean notifyWeekly) {
        getPreferences().edit().putBoolean(PREFS_NOTIFY_WEEKLY, notifyWeekly).commit();

        IBikeApplication.registerWeeklyNotification();
    }

    public boolean getNotifyWeekly() {
        return getPreferences().getBoolean(PREFS_NOTIFY_WEEKLY, true);
    }

    public boolean getShareData() {
        return getPreferences().getBoolean(PREFS_SHARE_DATA, false);
    }

    public void setShareData(boolean shareData) {
        getPreferences().edit().putBoolean(PREFS_SHARE_DATA, shareData).commit();
    }

    public void setOverlay(TogglableOverlay overlay, boolean value) {
        getPreferences().edit().putBoolean(getOverlayKey(overlay), value).commit();
    }

    public boolean getOverlay(TogglableOverlay overlay) {
        return getPreferences().getBoolean(getOverlayKey(overlay), false);
    }

    public String getOverlayKey(TogglableOverlay overlay) {
        return String.format("%s_%s", PREFS_OVERLAYS, overlay.getClass().getName());
    }
    public int getLengthNotificationOrdinal() {
        return getPreferences().getInt(LENGTH_NOTIFICATION, -1);
    }

    public void setLengthNotificationOrdinal(int ordinal) {
        getPreferences().edit().putInt(LENGTH_NOTIFICATION, ordinal).commit();
    }


    public int getMaxStreakLength() {
        return getPreferences().getInt(STREAK_NOTIFICATION, 0);
    }

    public void setMaxStreakLength(int streakLength) {
        getPreferences().edit().putInt(STREAK_NOTIFICATION, streakLength).commit();
    }

    public int getNewestTermsAccepted() {
        return getPreferences().getInt(NEWEST_TERMS_ACCEPTED, 0);
    }

    public void setNewestTermsAccepted(int newestTermsAccepted) {
        getPreferences().edit().putInt(NEWEST_TERMS_ACCEPTED, newestTermsAccepted).commit();
    }

    public boolean isCrashReportingEnabled() {
        return getPreferences().getBoolean(CRASH_REPORTING, true);
    }

    public void setCrashReporting(boolean enabled) {
        getPreferences().edit().putBoolean(CRASH_REPORTING, enabled).commit();
    }

    public boolean getReadAloud() {
        return getPreferences().getBoolean(READ_ALOUD, false);
    }

    public void setReadAloud(boolean enabled) {
        getPreferences().edit().putBoolean(READ_ALOUD, enabled).commit();
    }

    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this.context);
    }
}
