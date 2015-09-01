// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.*;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.model.GraphUser;
import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.LOG;

import java.util.Arrays;

public class LoginActivity extends Activity implements FBLoginListener {

    ActionBar actionBar;
    TextView textLoginTitle;
    EditText textEmail;
    EditText textPassword;
    Button btnLogin;
    TextView textOr;
    Button btnFacebookLogin;
    Button btnRegister;

    ProgressBar progressBar;
    Handler handler;
    UserData userData;
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    String fbToken;
    boolean isRunning = true;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        actionBar = getActionBar();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textLoginTitle = (TextView) findViewById(R.id.textLoginTitle);
        textOr = (TextView) findViewById(R.id.textOr);
        textEmail = (EditText) findViewById(R.id.textEmail);
        textPassword = (EditText) findViewById(R.id.textPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setTextColor(Color.WHITE);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (textEmail.getText() == null || ("" + textEmail.getText().toString().trim()).equals("") || textPassword.getText() == null
                        || ("" + textPassword.getText().toString()).trim().equals("")) {
                    launchErrorDialog("", IbikeApplication.getString("login_error_fields"));
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.myLooper();
                            Looper.prepare();
                            showProgressDialog();
                            userData = new UserData(textEmail.getText().toString(), textPassword.getText().toString());
                            Message message = HTTPAccountHandler.performLogin(userData);
                            handler.sendMessage(message);

                            dismissProgressDialog();

                        }
                    }).start();
                }
            }

        });

        if (handler == null) {
            handler = new Handler(new Handler.Callback() {

                @Override
                public boolean handleMessage(Message msg) {
                    Log.d("JC", "Received Facebook handler callback");

                    Bundle data = msg.getData();
                    Boolean success = data.getBoolean("success");
                    if (success) {
                        IbikeApplication.savePassword(textPassword.getText().toString());
                        LOG.d("fbdebug apitoken = " + data.getString("auth_token"));
                        String auth_token = data.getString("auth_token");
                        int id = data.getInt("id");
                        progressBar.setVisibility(View.GONE);
                        if (id < 0) {
                            launchErrorDialog("", "Login failed : " + data.toString());
                        } else {
                            if (auth_token == null || auth_token.equals("") || auth_token.equals("null")) {
                                auth_token = "";
                            }
                            PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString("auth_token", auth_token).commit();
                            PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putInt("id", id).commit();
                            LOG.d("Loged in token = " + auth_token + ", id = " + id);

                            setResult(RESULT_OK);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        }
                    } else {
                        final String message = data.containsKey("errors") ? data.getString("errors") : data.getString("info");
                        String title = "";
                        if (data.containsKey("info_title")) {
                            title = data.getString("info_title");
                        }
                        launchErrorDialog(title, message);
                    }
                    return true;
                }
            });
        }

        btnFacebookLogin = (Button) findViewById(R.id.btnFacebookLogin);
        btnFacebookLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.d("DV", "facebook btn clicked!");
                performFBLogin(savedInstanceState);
            }
        });

        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivityForResult(i, 1);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
        textEmail.setText(IbikeApplication.getEmail());
        textPassword.setText(IbikeApplication.getPassword());
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        textEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    textEmail.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
                }
                else{
                    textEmail.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        textPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    textPassword.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
                }
                else{
                    textPassword.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });
    }

    private void performFBLogin(Bundle savedInstanceState) {
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(LoginActivity.this, null, statusCallback, savedInstanceState);
            }
            if (session == null) {
                session = new Session(LoginActivity.this);
            }
        }
        Session.setActiveSession(session);
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED) || (!session.isOpened() && !session.isClosed())) {
            session.openForRead(new Session.OpenRequest(LoginActivity.this).setCallback(statusCallback).setPermissions(Arrays.asList("email")));
        } else if (session.isOpened() && !session.getPermissions().contains("email")) {
            session.requestNewPublishPermissions(new NewPermissionsRequest(LoginActivity.this, Arrays.asList("email")).setCallback(statusCallback));
        } else {
            Session.openActiveSession(LoginActivity.this, true, statusCallback);
        }
    }

    private void login() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper();
                Looper.prepare();
                showProgressDialog();
                LOG.d("fbdebug fbtoken = " + Session.getActiveSession().getAccessToken());
                Message message = HTTPAccountHandler.performFacebookLogin(Session.getActiveSession().getAccessToken());
                handler.sendMessage(message);
                dismissProgressDialog();

            }
        }).start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    private void initStrings() {
        actionBar.setTitle(IbikeApplication.getString("log_in"));
        textLoginTitle.setText(IbikeApplication.getString("please_login"));
        textLoginTitle.setTypeface(IbikeApplication.getItalicFont());
        textOr.setText(IbikeApplication.getString("or"));
        textEmail.setHint(IbikeApplication.getString("register_email_placeholder"));
        textEmail.setHintTextColor(getResources().getColor(R.color.HintColor));
        textPassword.setHint(IbikeApplication.getString("register_password_placeholder"));
        textPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
        btnLogin.setText(IbikeApplication.getString("log_in"));
        btnFacebookLogin.setText(IbikeApplication.getString("login_with_fb"));
        btnRegister.setText(IbikeApplication.getString("register_with_mail"));
    }

    public void showProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void dismissProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void launchErrorDialog(String title, String info) {
        if (!isFinishing() && isRunning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (!title.equals("")) {
                builder.setTitle(title);
            } else {
                builder.setTitle(IbikeApplication.getString("Error"));
            }
            builder.setMessage(info);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RegisterActivity.RESULT_ACCOUNT_REGISTERED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(IbikeApplication.getString("register_successful"));
            builder.setPositiveButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (resultCode == RegisterActivity.RESULT_FACEBOOK_REGISTERED) {
            performFBLogin(null);
            //finish();
        } else if (resultCode == RegisterActivity.RESULT_NO_ACTION) {
            // do nothing
        } else if (Session.getActiveSession() != null && data != null && data.getExtras() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        isRunning = true;
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().addCallback(statusCallback);
        }
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().removeCallback(statusCallback);
        }
        EasyTracker.getInstance().activityStop(this);
    }

    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened()) {
                fbToken = session.getAccessToken();
                if (!session.isOpened())
                    session = Session.getActiveSession();
                // make request to the /me API
                Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
                    // callback after Graph API
                    // response with user object
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user != null) {
                            HTTPAccountHandler.checkIsFbTokenValid(Session.getActiveSession().getAccessToken(), LoginActivity.this);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onFBLoginSuccess(final String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IbikeApplication.setIsFacebookLogin(true);
                login();
            }
        });
    }

    int numOfRetries = 0;

    @Override
    public void onFBLoginError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (numOfRetries == 0) {
                    Session session = Session.getActiveSession();
                    if (session != null) {
                        session.closeAndClearTokenInformation();
                    }
                    performFBLogin(null);
                    numOfRetries++;
                } else {
                    launchErrorDialog("", "Facebook login failed");
                }
            }
        });
    }

}
