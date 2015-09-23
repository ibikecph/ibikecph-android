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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.*;

import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.*;

import java.io.InputStream;
import java.net.URL;

public class ProfileActivity extends Activity implements ImagerPrefetcherListener {

    static final long API_REQUESTS_TIMEOUT = 2000;

    Button btnLogout;
    TextView textName, textEmail;
    Button btnDelete;
    ImageView pictureContainer;
    Handler handler;
    UserData userData;
    ProgressBar progressBar;
    String validationMessage;
    String base64Image = "";
    public static final int RESULT_USER_DELETED = 101;
    private static final int IMAGE_REQUEST = 1888;
    Thread tfetchUser;
    long lastAPIRequestTimestamp = 0;
    ActionBar actionbar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        actionbar = getActionBar();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        pictureContainer = (ImageView) findViewById(R.id.pictureContainer);

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setVisibility(View.VISIBLE);
        btnLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                IbikeApplication.logout();
                (new DB(ProfileActivity.this)).deleteFavorites();
                IbikeApplication.setIsFacebookLogin(false);

                // Disable tracking
                IbikeApplication.getSettings().setTrackingEnabled(false);
                IbikeApplication.getSettings().setNotifyMilestone(false);
                IbikeApplication.getSettings().setNotifyWeekly(false);

                // Set the result so the MapActivity causes the LeftMenu to reload 
                setResult(RESULT_OK);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }

        });


        textName = (TextView) findViewById(R.id.textName);
        textEmail = (TextView) findViewById(R.id.textEmail);

        btnDelete = (Button) findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchDeleteDialog();
            }
        });

        userData = new UserData(PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).getString("auth_token", ""), PreferenceManager
                .getDefaultSharedPreferences(ProfileActivity.this).getInt("id", -1));

        tfetchUser = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper();
                Looper.prepare();
                ProfileActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                Message message = HTTPAccountHandler.performGetUser(userData);
                handler.sendMessage(message);
            }
        });
        tfetchUser.start();

        if (handler == null) {
            handler = new Handler(new Handler.Callback() {

                @Override
                public boolean handleMessage(Message msg) {

                    Bundle data = msg.getData();
                    int msgType = data.getInt("type");
                    Boolean success = false;
                    switch (msgType) {
                        case HTTPAccountHandler.GET_USER:
                            success = data.getBoolean("success");
                            if (success) {
                                userData.setId(data.getInt("id"));
                                userData.setName(data.getString("name"));
                                userData.setEmail(data.getString("email"));
                                updateControls();
                                LoadImageFromWebOperations(data.getString("image_url"));
                            } else {
                                enableButtons();
                                userData = null;
                                Util.launchNoConnectionDialog(ProfileActivity.this);
                                progressBar.setVisibility(View.GONE);
                            }
                            break;
                        case HTTPAccountHandler.PUT_USER:
                            success = data.getBoolean("success");
                            if (!success) {
                                launchAlertDialog(data.getString("info"));
                            }
                            progressBar.setVisibility(View.GONE);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            enableButtons();
                            break;
                        case HTTPAccountHandler.DELETE_USER:
                            success = data.getBoolean("success");
                            if (success) {
                                Log.d("DV", "User Deleted!");
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("email").commit();
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("auth_token").commit();
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("signature").commit();
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("id").commit();
                                setResult(RESULT_USER_DELETED);
                                IbikeApplication.setIsFacebookLogin(false);
                                (new DB(ProfileActivity.this)).deleteFavorites();
                                finish();
                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            } else {
                                launchAlertDialog(data.getString("info"));
                            }
                            break;
                        case HTTPAccountHandler.ERROR:
                            enableButtons();
                            Util.launchNoConnectionDialog(ProfileActivity.this);
                            break;
                    }
                    return true;
                }
            });
        }

    }

    @Override
    public void onResume() {
        //actionbar.show();

        super.onResume();
        initStrings();
        disableButtons();

    }

    public void onImageContainerClick(View v) {
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        String pickTitle = "";
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        startActivityForResult(chooserIntent, IMAGE_REQUEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        progressBar.setVisibility(View.GONE);
        if (tfetchUser != null && tfetchUser.isAlive())
            tfetchUser.interrupt();
    }

    private void initStrings() {
        //textTitle.setText(IbikeApplication.getString("account"));
        //textTitle.setTypeface(IbikeApplication.getNormalFont());
        //textLogedIn.setText();
        btnLogout.setText(IbikeApplication.getString("logout"));
        btnDelete.setText(IbikeApplication.getString("delete_my_account"));
    }

    private void updateControls() {
        textName.setText(userData.getName());
        textEmail.setText(IbikeApplication.getString("track_token_subtitle_native") + " " + userData.getEmail());
    }

    private void launchAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setMessage(msg).setTitle(IbikeApplication.getString("Error"));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void launchDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(IbikeApplication.getString("delete_account_text")).setTitle(IbikeApplication.getString("delete_account_title"));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton(IbikeApplication.getString("Delete"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (System.currentTimeMillis() - lastAPIRequestTimestamp < API_REQUESTS_TIMEOUT) {
                    return;
                }
                IbikeApplication.getTracker().sendEvent("Account", "Delete", "", Long.valueOf(0));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.myLooper();
                        Looper.prepare();
                        ProfileActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        });
                        lastAPIRequestTimestamp = System.currentTimeMillis();
                        userData.setPassword(input.getText().toString());
                        Message message = HTTPAccountHandler.performDeleteUser(userData);
                        handler.sendMessage(message);
                        ProfileActivity.this.runOnUiThread(new Runnable() {
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            disableButtons();
            progressBar.setVisibility(View.VISIBLE);
            AsyncImageFetcher aif = new AsyncImageFetcher(this, this);
            aif.execute(data);
        }
    }

    private void LoadImageFromWebOperations(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = (InputStream) new URL(url).getContent();
                    final Drawable d = Drawable.createFromStream(is, "src name");
                    ProfileActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pictureContainer.setImageDrawable(d);
                            pictureContainer.invalidate();
                        }
                    });
                } catch (Exception e) {
                    if (e != null && e.getLocalizedMessage() != null)
                        LOG.e(e.getLocalizedMessage());
                }
                ProfileActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableButtons();
                        progressBar.setVisibility(View.GONE);
                    }
                });
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

    @Override
    public void onImagePrefetched(ImageData imageData) {
        if (imageData != null && imageData.bmp != null && imageData.base64 != null) {
            base64Image = imageData.base64;
            pictureContainer.setImageDrawable(imageData.bmp);
        } else {
            Toast.makeText(this, "Error fetching the image", Toast.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(View.GONE);
        enableButtons();
    }

    private void enableButtons() {
        btnDelete.setEnabled(true);
    }

    private void disableButtons() {
        btnDelete.setEnabled(false);
    }
}
