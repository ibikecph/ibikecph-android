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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.android.Facebook;
import com.facebook.widget.ProfilePictureView;
import com.google.analytics.tracking.android.EasyTracker;
import com.makeramen.roundedimageview.RoundedImageView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.InputStream;
import java.net.URL;

public class FacebookProfileActivity extends Activity {

    private Button btnLogout;
    private ActionBar actionBar;
    private TextView textLogedIn;
    private TextView textName;
    private TextView textLinked;
    private Button btnDelete;
    private Handler handler;
    private ProgressBar progressBar;
    private RoundedImageView pictureContainer;
    private String username = "";
    public static final int RESULT_USER_DELETED = 101;
    private boolean inProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_profile);

        actionBar = getActionBar();
        pictureContainer = (RoundedImageView) findViewById(R.id.pictureContainer);
        textLogedIn = (TextView) findViewById(R.id.textLogedIn);
        textName = (TextView) findViewById(R.id.textName);
        textLinked = (TextView) findViewById(R.id.textLinked);
        btnDelete = (Button) findViewById(R.id.btnDelete);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();

                setResult(RESULT_OK);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }

        });

        if (handler == null) {
            handler = new Handler(new Handler.Callback() {

                @Override
                public boolean handleMessage(Message msg) {

                    Bundle data = msg.getData();
                    int msgType = data.getInt("type");
                    Boolean success = false;
                    switch (msgType) {
                        case HTTPAccountHandler.DELETE_USER:
                            success = data.getBoolean("success");
                            if (success) {
                                PreferenceManager.getDefaultSharedPreferences(FacebookProfileActivity.this).edit().remove("email").commit();
                                PreferenceManager.getDefaultSharedPreferences(FacebookProfileActivity.this).edit().remove("auth_token").commit();
                                PreferenceManager.getDefaultSharedPreferences(FacebookProfileActivity.this).edit().remove("id").commit();
                                PreferenceManager.getDefaultSharedPreferences(FacebookProfileActivity.this).edit().remove("signature").commit();
                                IbikeApplication.setIsFacebookLogin(false);
                                setResult(RESULT_USER_DELETED);
                                (new DB(FacebookProfileActivity.this)).deleteFavorites();
                                finish();
                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            } else {
                                launchAlertDialog(data.getString("info"));
                            }
                            break;
                        case HTTPAccountHandler.ERROR:
                            logout();
                            Util.launchNoConnectionDialog(FacebookProfileActivity.this);
                            break;
                    }
                    return true;
                }
            });
        }
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Message message = HTTPAccountHandler.performGetUser(new UserData(IbikeApplication.getAuthToken(), PreferenceManager
                        .getDefaultSharedPreferences(FacebookProfileActivity.this).getInt("id", -1)));
                final Bundle data = message.getData();
                Boolean success = false;
                success = data.getBoolean("success");
                if (success) {
                    username = data.getString("name");
                    LoadImageFromWebOperations(data.getString("image_url"));
                } else
                    FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logout();
                            /*Util.showSimpleMessageDlg(FacebookProfileActivity.this, "Error fetching the user id = "
                                    + PreferenceManager.getDefaultSharedPreferences(FacebookProfileActivity.this).getInt("id", -1) + " auth token = "
                                    + IbikeApplication.getAuthToken() + (message != null ? message.toString() : ""));*/
                            progressBar.setVisibility(View.GONE);
                        }
                    });
            }
        }).start();

    }

    private void logout() {
        IbikeApplication.logout();
        (new DB(FacebookProfileActivity.this)).deleteFavorites();
        IbikeApplication.setIsFacebookLogin(false);

        // Disable tracking
        IbikeApplication.getSettings().setTrackingEnabled(false);
        IbikeApplication.getSettings().setNotifyMilestone(false);
        IbikeApplication.getSettings().setNotifyWeekly(false);

        Session session = Session.getActiveSession();
        if (session != null) { // && !session.isClosed()
            session.closeAndClearTokenInformation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    private void initStrings() {
        actionBar.setTitle(IbikeApplication.getString("account"));
        btnLogout.setText(IbikeApplication.getString("logout"));
        textLogedIn.setText(IbikeApplication.getString("track_token_subtitle_facebook"));
        textLogedIn.setTypeface(IbikeApplication.getItalicFont());
        textLinked.setText(IbikeApplication.getString("account_is_linked_to_facebook"));
        textLinked.setTypeface(IbikeApplication.getItalicFont());
        btnDelete.setText(IbikeApplication.getString("delete_my_account"));
    }

    public void onBtnDelete(View v) {
        if (!Util.isNetworkConnected(FacebookProfileActivity.this)) {
            Util.launchNoConnectionDialog(FacebookProfileActivity.this);
            return;
        }
        /*
        Check wether the Facebook-user has a created a password or not.
        If yes, prompt for it, else don't.
        */
        if (!inProgress) {
            inProgress = true;
            FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.myLooper();
                    Looper.prepare();
                    Message message = HTTPAccountHandler.performHasPassword();
                    final Bundle data = message.getData();
                    FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            inProgress = false;
                            if (data.getBoolean("has_password", false)) {
                                launchDeleteDialogWithPassword();
                            } else {
                                launchDeleteDialogWithoutPassword();
                            }
                        }
                    });

                }
            }).start();
        }
    }

    private void launchDeleteDialogWithoutPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FacebookProfileActivity.this);
        builder.setMessage(IbikeApplication.getString("delete_account_text")).setTitle(IbikeApplication.getString("delete_account_title"));
        builder.setPositiveButton(IbikeApplication.getString("Delete"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (!Util.isNetworkConnected(FacebookProfileActivity.this)) {
                    Util.launchNoConnectionDialog(FacebookProfileActivity.this);
                    return;
                }
                IbikeApplication.getTracker().sendEvent("Account", "Delete", "", Long.valueOf(0));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.myLooper();
                        Looper.prepare();

                        FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });

                        Message message = HTTPAccountHandler.performDeleteUser(new UserData(IbikeApplication.getAuthToken(), PreferenceManager
                                .getDefaultSharedPreferences(FacebookProfileActivity.this).getInt("id", 0)));
                        handler.sendMessage(message);

                        FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                }).start();

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void launchDeleteDialogWithPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(IbikeApplication.getString("delete_account_text")).setTitle(IbikeApplication.getString("delete_account_title"));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(IbikeApplication.getString("Delete"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                IbikeApplication.getTracker().sendEvent("Account", "Delete", "", Long.valueOf(0));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.myLooper();
                        Looper.prepare();
                        FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });

                        Message message = HTTPAccountHandler.performDeleteUser(new UserData(IbikeApplication.getAuthToken(), PreferenceManager
                                .getDefaultSharedPreferences(FacebookProfileActivity.this).getInt("id", 0), input.getText().toString()));
                        handler.sendMessage(message);
                        FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                }).start();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void launchAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(FacebookProfileActivity.this);
        builder.setMessage(msg).setTitle(IbikeApplication.getString("Error"));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void LoadImageFromWebOperations(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = (InputStream) new URL(url).getContent();
                    final Drawable d = Drawable.createFromStream(is, "src name");
                    String httpsUrl = url.replace("http", "https");
                    URL img_value = new URL(httpsUrl);
                    final Bitmap profilePic = BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
                    FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pictureContainer.setImageBitmap(profilePic);
                            pictureContainer.setScaleType(ImageView.ScaleType.CENTER);
                            pictureContainer.invalidate();
                        }
                    });
                } catch (Exception e) {
                    LOG.e(e.getLocalizedMessage());
                } finally {
                    FacebookProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            textName.setTypeface(IbikeApplication.getItalicFont());
                            textName.setText(username);
                        }
                    });
                }
            }
        }).start();

    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

}
