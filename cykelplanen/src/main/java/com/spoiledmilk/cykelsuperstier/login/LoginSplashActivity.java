// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.login;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.util.DB;

import java.util.ArrayList;

public class LoginSplashActivity extends com.spoiledmilk.ibikecph.login.LoginSplashActivity {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // findViewById(R.id.registerContainer).setOnTouchListener(new OnTouchListener() {
        // @Override
        // public boolean onTouch(View arg0, MotionEvent event) {
        // if (event.getAction() == MotionEvent.ACTION_DOWN) {
        // findViewById(R.id.btnRegister).setBackgroundColor(getButtonPressedColor());
        // final Handler handler = new Handler();
        // handler.postDelayed(new Runnable() {
        // @Override
        // public void run() {
        // try {
        // findViewById(R.id.btnRegister).setBackgroundColor(Color.WHITE);
        // } catch (Exception e) {
        //
        // }
        // }
        // }, 500);
        // }
        // return false;
        // }
        // });
        //
        // findViewById(R.id.skipContainer).setOnTouchListener(new OnTouchListener() {
        // @Override
        // public boolean onTouch(View arg0, MotionEvent event) {
        // if (event.getAction() == MotionEvent.ACTION_DOWN) {
        // findViewById(R.id.btnSkip).setBackgroundColor(getButtonPressedColor());
        // final Handler handler = new Handler();
        // handler.postDelayed(new Runnable() {
        // @Override
        // public void run() {
        // try {
        // findViewById(R.id.btnSkip).setBackgroundColor(Color.WHITE);
        // } catch (Exception e) {
        //
        // }
        // }
        // }, 500);
        // }
        // return false;
        // }
        // });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void launchMainMapActivity(String auth_token, int id) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("auth_token", auth_token).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("id", id).commit();
        // if
        // (!PreferenceManager.getDefaultSharedPreferences(this).contains("reminders_shown"))
        // {
        // Intent i = new Intent(this, RemindersSplashActivity.class);
        // startActivity(i);
        // overridePendingTransition(R.anim.slide_in_right,
        // R.anim.slide_out_left);
        // finish();
        // } else {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DB db = new DB(LoginSplashActivity.this);
                ArrayList<FavoritesData> favorites = db.getFavoritesFromServer(LoginSplashActivity.this, null);

                launchMainMapActivity();
            }
        }).start();
        // }
    }


}
