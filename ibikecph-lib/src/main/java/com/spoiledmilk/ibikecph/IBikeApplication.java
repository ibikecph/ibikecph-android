// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.Spanned;
import android.util.Log;

import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.overlays.TogglableOverlay;
import com.spoiledmilk.ibikecph.map.overlays.TogglableOverlayFactory;
import com.spoiledmilk.ibikecph.map.overlays.RefreshOverlaysTask;
import com.spoiledmilk.ibikecph.tracking.MilestoneManager;
import com.spoiledmilk.ibikecph.tracking.TrackHelper;
import com.spoiledmilk.ibikecph.tracking.TrackingManager;
import com.spoiledmilk.ibikecph.util.IBikePreferences;
import com.spoiledmilk.ibikecph.util.IBikePreferences.Language;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.SMDictionary;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.realm.exceptions.RealmMigrationNeededException;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class IBikeApplication extends Application {
    protected static String APP_NAME = "I Bike CPH";
    private static IBikeApplication instance = null;
    public IBikePreferences prefs;
    public SMDictionary dictionary;
    private static Typeface normalFont, boldFont, italicFont;

    private static TrackingManager trackingManager;
    protected static int primaryColor = R.color.PrimaryColor;

    public static DateFormat getTimeFormat() {
        return android.text.format.DateFormat.getTimeFormat(instance);
    }

    /**
     * An overrideable method that returns if the app is build in debug mode
     * @return
     */
    public boolean isDebugging() {
        return BuildConfig.DEBUG;
    }

    public static boolean isDebugging(Activity activity) {
        return ((IBikeApplication) activity.getApplication()).isDebugging();
    }

    @Override
    public void onCreate() {
        LOG.d("Creating Application");
        super.onCreate();
        instance = this;
        prefs = new IBikePreferences(this);
        prefs.load();
        dictionary = new SMDictionary(this);
        dictionary.init();
        normalFont = Typeface.DEFAULT; //Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeueLTCom-Md.ttf");
        boldFont = Typeface.DEFAULT_BOLD; //Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeueLTCom-Bd.ttf");
        italicFont = Typeface.defaultFromStyle(Typeface.ITALIC); // Typeface.createFromAsset(getAssets(), "fonts/HelveticaNeueLTCom-It.ttf");

        this.startService(new Intent(this, BikeLocationService.class));

        initializeSelectableOverlays();

        trackingManager = TrackingManager.getInstance();

        // Register a weekly notification
        registerWeeklyNotification();

        //Create default realm
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder().name("IBikeCPHrealm.realm").build();
        Realm.setDefaultConfiguration(config);

        // Ensure all tracks have been geocoded.
        try {
            TrackHelper.ensureAllTracksGeocoded();
        } catch (RealmMigrationNeededException e) {
            // If we need to migrate Realm, just delete the file
            /* FIXME: This should clearly not go into production. We should decide on a proper DB schema, and make proper
               migrations if we need to change it. */
            Log.d("JC", "Migration needed, deleting the Realm file!");
            // FIXME²: This method has been removed from Realm.
            // Might be useful: https://realm.io/docs/java/latest/api/io/realm/Realm.html#deleteRealm-io.realm.RealmConfiguration-
            // Realm.deleteRealmFile(this);
        }
    }

    protected void initializeSelectableOverlays() {
        // Let's try to load the overlays from the server - and not hang the UI thread meanwhile
        new RefreshOverlaysTask(this).execute();
    }

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     * @return tracker
     */

    public static Spanned getSpanned(String key) {
        return instance.dictionary.get(key);
    }

    public static String getString(String key) {
        return instance.dictionary.get(key).toString();
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public static BikeLocationService getService() {
        return BikeLocationService.getInstance();
    }

    public static TrackingManager getTrackingManager() {
        return trackingManager;
    }

    public void changeLanguage(Language language) {
        if (prefs.getLanguage() != language) {
            LOG.d("Changing language to " + language.name());
            dictionary.changeLanguage(language);
            prefs.setLanguage(language);
        }
    }

    public static String getLanguageString() {
        return instance.prefs.language == Language.DAN ? "da" : "en";
    }

    public static Locale getLocale() {
        return instance.prefs.language == Language.DAN ?
               new Locale("da") :
               Locale.ENGLISH;
    }

    public static IBikePreferences getSettings() {
        return instance.prefs;
    }

    public static boolean isUserLogedIn() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).contains("auth_token");
    }

    public static String getAuthToken() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString("auth_token", "");
    }

    public static String getSignature() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString("signature", "");
    }

    public static void setIsFacebookLogin(boolean b) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("is_facebook_login", b).commit();
    }

    public static boolean isFacebookLogin() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("is_facebook_login", false);
    }

    public static Typeface getBoldFont() {
        return boldFont;
    }

    public static Typeface getItalicFont() {
        return italicFont;
    }

    public static void setWelcomeScreenSeen(boolean b) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putBoolean("welcone_seen", b).commit();
    }

    public static boolean isWelcomeScreenSeen() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("welcone_seen", false);
    }

    public static void saveEmail(String email) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("email", email).commit();
    }

    public static String getEmail() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString("email", "");
    }

    public static void logout() {
        IBikeApplication.setIsFacebookLogin(false);

        // Disable tracking
        IBikeApplication.getSettings().setTrackingEnabled(false);
        IBikeApplication.getSettings().setNotifyMilestone(false);
        IBikeApplication.getSettings().setNotifyWeekly(false);
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("email").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("auth_token").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("id").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("signature").commit();

        // TODO: Move this somewhere more natural
        if (BikeLocationService.getInstance().getActivityRecognitionClient() != null) {
            BikeLocationService.getInstance().getActivityRecognitionClient().releaseActivityUpdates();
        }
        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MapActivity.mapActivityContext.startActivity(intent);
    }

    public static void logoutWrongToken() {
        IBikeApplication.setIsFacebookLogin(false);

        // Disable tracking
        IBikeApplication.getSettings().setTrackingEnabled(false);
        IBikeApplication.getSettings().setNotifyMilestone(false);
        IBikeApplication.getSettings().setNotifyWeekly(false);
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("email").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("auth_token").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("id").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("signature").commit();

        // TODO: Move this somewhere more natural
        if (BikeLocationService.getInstance().getActivityRecognitionClient() != null) {
            BikeLocationService.getInstance().getActivityRecognitionClient().releaseActivityUpdates();
        }
        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("loggedOut", true);
        MapActivity.mapActivityContext.startActivity(intent);

    }

    public static void logoutDeleteUser() {
        IBikeApplication.setIsFacebookLogin(false);

        // Disable tracking
        IBikeApplication.getSettings().setTrackingEnabled(false);
        IBikeApplication.getSettings().setNotifyMilestone(false);
        IBikeApplication.getSettings().setNotifyWeekly(false);
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("email").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("auth_token").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("id").commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove("signature").commit();

        // TODO: Move this somewhere more natural
        if (BikeLocationService.getInstance().getActivityRecognitionClient() != null) {
            BikeLocationService.getInstance().getActivityRecognitionClient().releaseActivityUpdates();
        }
        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("deleteUser", true);
        MapActivity.mapActivityContext.startActivity(intent);

    }

    /**
     * Registers an intent to be delivered every Sunday at 8pm. We want to tell the user how much
     * she's been cycling the past week.
     */
    public static void registerWeeklyNotification() {
        // Let's only do this if the app was actually build with tracking
        // TODO: Consider also checking for the tracking preference to be enabled.
        boolean trackingEnabled = getContext().getResources().getBoolean(R.bool.trackingEnabled);
        if (trackingEnabled) {
            Context ctx = IBikeApplication.getContext();
            AlarmManager alarmMgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(ctx, MilestoneManager.class);
            intent.putExtra("weekly", true);
            PendingIntent alarmIntent = PendingIntent.getService(ctx, 0, intent, 0);

            Calendar nextSunday = Calendar.getInstance();

            // Without resorting to third party libraries, there's no real elegant way of doing this...
            // Add a day
            while (!(nextSunday.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && nextSunday.get(Calendar.HOUR_OF_DAY) < 18)) {
                nextSunday.add(Calendar.DAY_OF_WEEK, 1);

                // If today is Sunday but it's after 8, make sure to at least trigger on the *next* Sunday! :)
                nextSunday.set(Calendar.HOUR_OF_DAY, 12);
            }

            nextSunday.set(Calendar.HOUR_OF_DAY, 18);
            nextSunday.set(Calendar.MINUTE, 0);
            nextSunday.set(Calendar.SECOND, 0);

            // Great, nextSunday now reflects the time 18:00 on the coming Sunday, or today if called on a Sunday.

            /*
            // DEBUG CODE. Will schedule the notification ten seconds after starting, repeating every 20 secs.
            nextSunday = Calendar.getInstance();
            nextSunday.add(Calendar.SECOND, 10);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, nextSunday.getTimeInMillis(), 1000 * 20, alarmIntent);
            */

            // Run the notification next Sunday, repeating every Sunday. MilestoneManager will receive the Intent
            // regardless of the user's preference, but will only actually *make* the notification if the user wants it.
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, nextSunday.getTimeInMillis(), 1000 * 60 * 60 * 24 * 7, alarmIntent);
        }
    }

    public static String getAppName() {
        return APP_NAME;
    }

    public static Class getTermsAcceptanceClass() {
        return AcceptNewTermsActivity.class;
    }

    public List<Class<? extends TogglableOverlay>> getTogglableOverlayClasses() {
        return new ArrayList<>();
    }

    public static int getPrimaryColor() {
        return primaryColor;
    }

    public boolean breakRouteIsEnabled() {
        return getResources().getBoolean(R.bool.breakRouteEnabled);
    }

}
