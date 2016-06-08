package com.spoiledmilk.ibikecph.introduction;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activities that inherit this can be used to present introductions to new features.
 * Created by kraen on 21-05-16.
 */
public abstract class IntroductionActivity extends Activity {

    static List<Class<? extends IntroductionActivity>> availableIntroductions = new ArrayList<>();

    static {
        // The order in which activities are added to the list is the order in which they are shown.
        availableIntroductions.add(GreenPathsIntroductionActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.introduction_activity);

        Button continueButton = (Button) findViewById(R.id.continueButton);
        continueButton.setText(IBikeApplication.getString("continue_button_text"));
    }

    public void continueClicked(View v) {
        setHasBeenIntroducedTo(getApplication(), IntroductionActivity.this.getClass(), true);
        finish();
    }

    public static String getPreferencesKey(Class<? extends IntroductionActivity> activity) {
        return String.format("%s-%s", "was-introduced-to", activity.getSimpleName());
    }

    public static boolean hasBeenIntroducedTo(Context context, Class<? extends IntroductionActivity> activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(getPreferencesKey(activity), false);
    }

    private static void setHasBeenIntroducedTo(Application application, Class<? extends IntroductionActivity> activity, boolean introduced) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        preferences.edit().putBoolean(getPreferencesKey(activity), introduced).commit();
    }

    public static void startIntroductionActivities(Context context) {
        // Let's launch all introductions that the user has not seen, in reverse order.
        List<Class<? extends IntroductionActivity>> relevantIntroductions = new ArrayList<>();

        for(Class<? extends IntroductionActivity> introduction: availableIntroductions) {
            if(!IntroductionActivity.hasBeenIntroducedTo(context, introduction)) {
                relevantIntroductions.add(introduction);
            }
        }

        // The activities started are added to a stack, so we need to add the first to be shown last
        Collections.reverse(relevantIntroductions);

        // Start all the relevant activities
        for(Class<? extends IntroductionActivity> introduction: relevantIntroductions) {
            Intent i = new Intent(context, introduction);
            context.startActivity(i);
        }
    }
}
