// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.util;

import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import com.spoiledmilk.ibikecph.IbikeApplication;

public class IbikePreferences {
    public static final boolean DEBUGMODE = true;

    public static boolean REMEMBER_MAP_STATE = false;

    public static final String PREFS_TILE_SOURCE = "tilesource";
    public static final String PREFS_SCROLL_X = "scrollX";
    public static final String PREFS_SCROLL_Y = "scrollY";
    public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    public static final String PREFS_SHOW_LOCATION = "showLocation";
    public static final String PREFS_SHOW_COMPASS = "showCompass";

    public static final String PREFS_LANGUAGE = "language";

    public static final int ROUTE_COLOR = Color.rgb(0, 174, 239);
    public static final float ROUTE_STROKE_WIDTH = 10.0f;
    public static final int ROUTE_ALPHA = 0xc0;
    public static final int ROUTE_DIMMED_COLOR = Color.LTGRAY;

    public static final float DEFAULT_ZOOM_LEVEL = 17.0f;

    public enum Language {
        UNDEFINED, ENG, DAN
    }

    public Language language = Language.UNDEFINED;

    private Context context;

    public IbikePreferences(Context context) {
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
        languageNames[0] = IbikeApplication.getString("language_eng");
        languageNames[1] = IbikeApplication.getString("language_dan");
        return languageNames;
    }
}
