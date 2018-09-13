// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.

package dk.kk.ibikecphlib;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.Spanned;

import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.overlays.TogglableOverlay;
import dk.kk.ibikecphlib.map.overlays.RefreshOverlaysTask;
import dk.kk.ibikecphlib.util.IBikePreferences;
import dk.kk.ibikecphlib.util.IBikePreferences.Language;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.SMDictionary;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class IBikeApplication extends Application {
    protected static String APP_NAME = "I Bike CPH";
    private static IBikeApplication instance = null;
    public IBikePreferences prefs;
    public SMDictionary dictionary;
    private static Typeface normalFont, boldFont, italicFont;

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
    }

    protected void initializeSelectableOverlays() {
        // Let's try to load the overlays from the server - and not hang the UI thread meanwhile
        new RefreshOverlaysTask(this).execute();
    }


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

        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("auth_token", null).apply();

        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MapActivity.mapActivityContext.startActivity(intent);
    }

    public static void logoutWrongToken() {
        IBikeApplication.setIsFacebookLogin(false);

        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("loggedOut", true);
        MapActivity.mapActivityContext.startActivity(intent);

    }

    public static void logoutDeleteUser() {
        IBikeApplication.setIsFacebookLogin(false);

        Intent intent = new Intent(MapActivity.mapActivityContext, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("deleteUser", true);
        MapActivity.mapActivityContext.startActivity(intent);

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
