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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.Session;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.util.DB;

import java.util.ArrayList;

public class LoginSplashActivity extends Activity {

    public static final int IMAGE_REQUEST = 1888;
    Bundle savedInstanceState;
    Handler handler;

    private static boolean DEBUG = true;
    public static final int LOGIN_REQUEST = 80;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(getLayoutId());
        this.savedInstanceState = savedInstanceState;

        // If the user has already seen the welcome screen, don't bother her again.
        if (IbikeApplication.isWelcomeScreenSeen() && !DEBUG) {
            launchMainMapActivity();
        }

        TextView welcomeTextView = (TextView) findViewById(R.id.welcomeTextView);
        TextView welcomeExplanationTextView = (TextView) findViewById(R.id.welcomeExplanationTextView);
        Button readMoreButton = (Button) findViewById(R.id.readMoreButton);
        Button skipButton = (Button) findViewById(R.id.skipButton);
        Button logInButton = (Button) findViewById(R.id.logInButton);

        welcomeTextView.setText(IbikeApplication.getString("startup_welcome"));
        welcomeExplanationTextView.setText(IbikeApplication.getString("startup_explanation"));
        readMoreButton.setText(IbikeApplication.getString("startup_readmore"));

        skipButton.setText(IbikeApplication.getString("no_thanks"));
        logInButton.setText(IbikeApplication.getString("log_in"));


        /*
        if (handler == null) {
            handler = new Handler(new Handler.Callback() {

                @Override
                public boolean handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    Boolean success = data.getBoolean("success");
                    dismissProgressDialog();
                    if (rd != null) {
                        rd.inProgress = false;
                    }
                    if (success) {
                        String auth_token = data.getString("auth_token");
                        int id = data.getInt("id");
                        if (id < 0) {
                            launchErrorDialog("", "Login failed : " + data.toString());
                        } else {
                            if (auth_token == null || auth_token.equals("") || auth_token.equals("null")) {
                                auth_token = "";
                            }
                            launchMainMapActivity(auth_token, id);
                        }
                    } else {
                        String title = "";
                        if (data.containsKey("info_title")) {
                            title = data.getString("info_title");
                        }
                        launchErrorDialog(title, data.getString("info"));
                    }
                    return true;
                }
            });
        }

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        */
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
    }

/*
    public void performFBLogin() {
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, statusCallback, savedInstanceState);
            }
            if (session == null) {
                session = new Session(this);
            }
        }
        Session.setActiveSession(session);
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED) || (!session.isOpened() && !session.isClosed())) {
            session.openForRead(new Session.OpenRequest(this).setCallback(statusCallback).setPermissions(Arrays.asList("email")));
        } else if (session.isOpened() && !session.getPermissions().contains("email")) {
            session.requestNewPublishPermissions(new NewPermissionsRequest(this, Arrays.asList("email")).setCallback(statusCallback));
        } else {
            Session.openActiveSession(this, true, statusCallback);
        }
    }
*/
    public void onBtnSkipClick(View v) {
        launchMainMapActivity();
    }

    /*
    private void login(final String accessToken) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper();
                Looper.prepare();
                LOG.d("facebook login fb token = " + accessToken);
                Message message = HTTPAccountHandler.performFacebookLogin(accessToken);
                handler.sendMessage(message);

            }
        }).start();
    }
    */
    @Override
    public void onPause() {
        super.onPause();
    }
    public void launchMainMapActivity() {
        IbikeApplication.setWelcomeScreenSeen(true);
        Intent i = new Intent(LoginSplashActivity.this, MapActivity.class);
        LoginSplashActivity.this.startActivity(i);
        finish();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Session.getActiveSession() != null && data != null && data.getExtras() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        } else if (requestCode == LOGIN_REQUEST && resultCode == RESULT_OK) {
            launchMainMapActivity();
        }

    }

    public void launchMainMapActivity(String auth_token, int id) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("auth_token", auth_token).commit();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("id", id).commit();
        new Thread(new Runnable() {
            @Override
            public void run() {
                DB db = new DB(LoginSplashActivity.this);
                ArrayList<FavoritesData> favorites = db.getFavoritesFromServer(LoginSplashActivity.this, null);
                if (favorites == null || favorites.size() == 0) {
                    LoginSplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            IbikeApplication.setWelcomeScreenSeen(true);
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


}
