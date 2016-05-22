package com.spoiledmilk.ibikecph.introduction;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.overlays.GreenPathsOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Activities that inherit this can be used to present introductions to new features.
 * Created by kraen on 21-05-16.
 */
public abstract class IntroductionActivity extends Activity {

    static List<Class<? extends IntroductionActivity>> availableIntroductions = new ArrayList<>();

    static {
        availableIntroductions.add(GreenPathsIntroductionActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.introduction_activity);
        Log.d("IntroductionActivity", "Created from " + getParent());

        findViewById(R.id.introductionLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHasBeenIntroducedTo(getApplication(), IntroductionActivity.this.getClass(), true);
                finish();
            }
        });
    }

    public static String getPreferencesKey(Class<? extends IntroductionActivity> activity) {
        return String.format("%s-%s", "was-introduced-to", activity.getSimpleName());
    }

    public static boolean hasBeenIntroducedTo(Application application, Class<? extends IntroductionActivity> activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        return preferences.getBoolean(getPreferencesKey(activity), false);
    }

    private static void setHasBeenIntroducedTo(Application application, Class<? extends IntroductionActivity> activity, boolean introduced) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.edit().putBoolean(getPreferencesKey(activity), introduced).commit();
    }

    public static Class<? extends IntroductionActivity> nextIntroduction(Application application) {
        for(Class<? extends IntroductionActivity> introduction: availableIntroductions) {
            if(!IntroductionActivity.hasBeenIntroducedTo(application, introduction)) {
                return introduction;
            }
        }
        return null;
    }

}
