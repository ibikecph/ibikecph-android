// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.Session;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.ReadMoreActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesActivity;
import com.spoiledmilk.ibikecph.favorites.FavoriteListItem;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.util.DB;

import java.util.ArrayList;

public class LoginSplashActivity extends Activity {

    public static final int IMAGE_REQUEST = 1888;
    Bundle savedInstanceState;
    Handler handler;

    public static final int LOGIN_REQUEST = 80;
    public static final int RESULT_FACEBOOK_REGISTERED = 104;
    private Button logInButton, enableTrackingButton;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.login_splash_activity);
        this.savedInstanceState = savedInstanceState;

        this.findViewById(R.id.rootLayout).setBackgroundColor(getResources().getColor(IBikeApplication.getPrimaryColor()));

        TextView welcomeTextView = (TextView) findViewById(R.id.welcomeTextView);
        TextView welcomeExplanationTextView = (TextView) findViewById(R.id.welcomeExplanationTextView);
        Button readMoreButton = (Button) findViewById(R.id.readMoreButton);
        Button skipButton = (Button) findViewById(R.id.skipButton);
        logInButton = (Button) findViewById(R.id.logInButton);
        enableTrackingButton = (Button) findViewById(R.id.enableTrackingButton);

        welcomeTextView.setText(IBikeApplication.getString("launch_activate_tracking_title"));
        welcomeExplanationTextView.setText(IBikeApplication.getString("launch_activate_tracking_description"));
        readMoreButton.setText(IBikeApplication.getString("launch_activate_tracking_read_more"));

        skipButton.setText(IBikeApplication.getString("no_thanks"));
        logInButton.setText(IBikeApplication.getString("log_in"));
        enableTrackingButton.setText(IBikeApplication.getString("enable"));
    }

    protected int getLayoutId() {

        return R.layout.login_splash_activity;
    }

    protected int getButtonPressedColor() {

        return 0xFF07568B;
    }

    protected int getButtonImageResource() {

        return R.drawable.btn_splash_blue_selector;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean trackingEnabled = getResources().getBoolean(R.bool.trackingEnabled);
        if (IBikeApplication.isWelcomeScreenSeen() || !trackingEnabled) {
            launchMainMapActivity();
        }
    }

    protected Class<?> getMapActivityClass() {
        return MapActivity.class;
    }

    public void onBtnSkipClick(View v) {

        launchMainMapActivity();
    }

    public void onReadMoreClick(View v) {
        Intent i = new Intent(LoginSplashActivity.this, ReadMoreActivity.class);
        LoginSplashActivity.this.startActivity(i);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void launchMainMapActivity() {
        IBikeApplication.setWelcomeScreenSeen(true);
        Intent i = new Intent(LoginSplashActivity.this, getMapActivityClass());
        LoginSplashActivity.this.startActivity(i);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Session.getActiveSession() != null && data != null && data.getExtras() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        } else if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
            // launchMainMapActivity();
            changeLoginButtonToEnableTrackingButton();
        }
    }

    private void changeLoginButtonToEnableTrackingButton() {
        logInButton.setVisibility(View.GONE);
        enableTrackingButton.setVisibility(View.VISIBLE);
    }

    public void launchMainMapActivity(String auth_token, int id) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("auth_token", auth_token).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("id", id).commit();
        new Thread(new Runnable() {
            @Override
            public void run() {
                DB db = new DB(LoginSplashActivity.this);
                ArrayList<FavoriteListItem> favorites = db.getFavoritesFromServer(null);
                if (favorites == null || favorites.size() == 0) {
                    LoginSplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            IBikeApplication.setWelcomeScreenSeen(true);
                            Intent i = new Intent(LoginSplashActivity.this, FavoritesActivity.class);
                            startActivity(i);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            finish();

                        }
                    });
                } else {
                    LoginSplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            launchMainMapActivity();
                        }
                    });
                }
            }
        }).start();
    }

    public void onBtnLogInClick(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivityForResult(i, LOGIN_REQUEST);
    }

    public void onEnableTrackingClick(View v) {

        if (IBikeApplication.getSignature().equals("")) {
            if (IBikeApplication.isFacebookLogin()) {
                Log.d("DV", "Prompting Facebookuser to create a password!");
                Intent i = new Intent(LoginSplashActivity.this, SignatureActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("fromSplashScreen", true);
                IBikeApplication.setWelcomeScreenSeen(true);
                startActivity(i);
            } else if (IBikeApplication.isUserLogedIn()) {
                Log.d("DV", "Prompting login for user!");
                Intent i = new Intent(LoginSplashActivity.this, SignatureActivity.class).putExtra("normalUser", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                IBikeApplication.setWelcomeScreenSeen(true);
                startActivity(i);
            }
        } else {
            Log.d("DV", "We got a signature, enabling tracking!");
            IBikeApplication.getSettings().setTrackingEnabled(true);
            launchMainMapActivity();
        }

    }
}
