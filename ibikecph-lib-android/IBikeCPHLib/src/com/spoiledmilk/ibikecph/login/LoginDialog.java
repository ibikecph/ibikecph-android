// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;

public class LoginDialog {

    Context context;

    TextView textLoginTitle;
    EditText textEmail;
    EditText textPassword;
    Button btnBack;
    TexturedButton btnLogin;
    TextView textCreateAccount;

    UserData userData;

    Dialog dialog;
    ProgressBar progressBar;
    Handler handler;

    public void createLoginDialog(Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.login_dialog);
        progressBar = (ProgressBar) dialog.findViewById(R.id.progressBar);
        textLoginTitle = (TextView) dialog.findViewById(R.id.textLoginTitle);
        textLoginTitle.setText(IbikeApplication.getString("log_in"));
        textLoginTitle.setTypeface(IbikeApplication.getBoldFont());
        textEmail = (EditText) dialog.findViewById(R.id.textEmail);
        textEmail.setHint(IbikeApplication.getString("register_email_placeholder"));
        textEmail.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        textEmail.setTypeface(IbikeApplication.getNormalFont());
        textPassword = (EditText) dialog.findViewById(R.id.textPassword);
        textPassword.setHint(IbikeApplication.getString("register_password_placeholder"));
        textPassword.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        textPassword.setTypeface(IbikeApplication.getNormalFont());
        btnBack = (Button) dialog.findViewById(R.id.btnBack);
        btnBack.setText(IbikeApplication.getString("back"));
        btnBack.setTypeface(IbikeApplication.getBoldFont());
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.GONE);
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
            }

        });
        btnLogin = (TexturedButton) dialog.findViewById(R.id.btnLogin);
        btnLogin.setBackgroundResource(R.drawable.btn_blue_selector);
        btnLogin.setTextureResource(R.drawable.btn_pattern_repeteable);
        btnLogin.setTextColor(Color.WHITE);
        btnLogin.setText(IbikeApplication.getString("login"));
        btnLogin.setTypeface(IbikeApplication.getBoldFont());
        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (textEmail.getText() == null || ("" + textEmail.getText().toString().trim()).equals("") || textPassword.getText() == null
                        || ("" + textPassword.getText().toString()).trim().equals("")) {
                    launchErrorDialog("", IbikeApplication.getString("login_error_fields"));
                } else {
                    showProgressDialog();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.myLooper();
                            Looper.prepare();
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
                    Bundle data = msg.getData();
                    Boolean success = data.getBoolean("success");
                    if (success) {
                        IbikeApplication.savePassword(textPassword.getText().toString());
                        String auth_token = data.getString("auth_token");
                        int id = data.getInt("id");
                        launchMainMapActivity(auth_token, id);
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    } else {
                        final String message = data.containsKey("errors") ? data.getString("errors") : data.getString("info");
                        Log.d("", data.toString());
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
        textCreateAccount = (TextView) dialog.findViewById(R.id.textCreateAccount);
        textCreateAccount.setText(IbikeApplication.getString("login_new_account"));
        textCreateAccount.setTypeface(IbikeApplication.getBoldFont());
        textCreateAccount.setClickable(true);
        final Context contextFinal = context;
        textCreateAccount.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                RegisterDialog rd = new RegisterDialog();
                rd.createRegisterDialog(contextFinal);
                ((LoginSplashActivity) contextFinal).setRegisterDialog(rd);
            }
        });

        dialog.getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (contextFinal != null) {
                    ((LoginSplashActivity) contextFinal).onDialogDismissed();
                }

            }
        });
        textEmail.setText(IbikeApplication.getEmail());
        textPassword.setText(IbikeApplication.getPassword());
        dialog.show();
    }

    private void launchErrorDialog(String title, String info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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

    private void launchMainMapActivity(String auth_token, int id) {
        dialog.dismiss();
        progressBar.setVisibility(View.GONE);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("auth_token", auth_token).commit();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("id", id).commit();
        ((LoginSplashActivity) context).launchMainMapActivity(auth_token, id);
    }

    public void showProgressDialog() {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void dismissProgressDialog() {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

}
