// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import java.io.InputStream;
import java.net.URL;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.util.AsyncImageFetcher;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.ImageData;
import com.spoiledmilk.ibikecph.util.ImagerPrefetcherListener;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

public class ProfileActivity extends Activity implements ImagerPrefetcherListener {

    static final long API_REQUESTS_TIMEOUT = 2000;

    Button btnLogout;
    EditText textName, textEmail, textOldPassword, textNewPassword, textPasswordConfirm;
    TexturedButton btnSave;
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
                
                // Set the result so the MapActivity causes the LeftMenu to reload 
                setResult(RESULT_OK);
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }

        });
        
        
        textName = (EditText) findViewById(R.id.textName);
        textEmail = (EditText) findViewById(R.id.textEmail);
        textNewPassword = (EditText) findViewById(R.id.textNewPassword);
        textPasswordConfirm = (EditText) findViewById(R.id.textPasswordConfirm);
        textOldPassword = (EditText) findViewById(R.id.textOldPassword);
        btnSave = (TexturedButton) findViewById(R.id.btnSave);
        btnSave.setTextureResource(R.drawable.btn_pattern_repeteable);
        btnSave.setBackgroundResource(R.drawable.btn_blue_selector);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - lastAPIRequestTimestamp < API_REQUESTS_TIMEOUT) {
                    return;
                }
                if (userData != null && validateInput()) {
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
                            Message message = HTTPAccountHandler.performPutUser(userData);
                            handler.sendMessage(message);

                        }
                    }).start();
                } else {
                    launchAlertDialog(validationMessage);
                }
            }
        });

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
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("email").commit();
                                PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this).edit().remove("auth_token").commit();
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
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takePhotoIntent });
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
        btnLogout.setText(IbikeApplication.getString("logout"));
        btnLogout.setTypeface(IbikeApplication.getNormalFont());
        textOldPassword.setHint(IbikeApplication.getString("old_password"));
        textOldPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
        textOldPassword.setTypeface(IbikeApplication.getNormalFont());
        textNewPassword.setHint(IbikeApplication.getString("account_password_placeholder"));
        textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
        textNewPassword.setTypeface(IbikeApplication.getNormalFont());
        textPasswordConfirm.setHint(IbikeApplication.getString("account_repeat_placeholder"));
        textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
        textPasswordConfirm.setTypeface(IbikeApplication.getNormalFont());
        btnSave.setText(IbikeApplication.getString("save_changes"));
        btnSave.setTypeface(IbikeApplication.getNormalFont());
        btnDelete.setText(IbikeApplication.getString("delete_my_account"));
        btnDelete.setTypeface(IbikeApplication.getNormalFont());
    }

    private void updateControls() {
        textName.setText(userData.getName());
        textEmail.setText(userData.getEmail());
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

    private boolean validateInput() {
        boolean ret = true;
        if (textName.getText().toString().length() == 0 || textEmail.getText().toString().length() == 0) {
            validationMessage = IbikeApplication.getString("register_error_fields");
            ret = false;
        } else if (!textNewPassword.getText().toString().trim().equals("")
                && !textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
            validationMessage = IbikeApplication.getString("register_error_passwords");
            ret = false;
        } else if (textNewPassword.getText().toString().length() < 3 && textNewPassword.getText().toString().length() > 0) {
            validationMessage = IbikeApplication.getString("register_error_passwords_short");
            ret = false;
        } else {
            int atIndex = textEmail.getText().toString().indexOf('@');
            if (atIndex < 1) {
                validationMessage = IbikeApplication.getString("register_error_invalid_email");
                ret = false;
            }
            int pointIndex = textEmail.getText().toString().indexOf('.', atIndex);
            if (pointIndex < atIndex || pointIndex == textEmail.getText().toString().length() - 1) {
                validationMessage = IbikeApplication.getString("register_error_invalid_email");
                ret = false;
            }
        }
        if (textOldPassword.getText() == null) {
            validationMessage = IbikeApplication.getString("register_error_passwords");
            ret = false;
        }
        if (textOldPassword.getText() != null && !IbikeApplication.getPassword().equals(textOldPassword.getText().toString())
                && !IbikeApplication.getPassword().equals(textOldPassword.getText().toString())) {
            validationMessage = IbikeApplication.getString("register_error_passwords");
            ret = false;
        }
        userData.setName(textName.getText().toString());
        userData.setEmail(textEmail.getText().toString());
        if (!textNewPassword.getText().toString().trim().equals("")) {
            userData.setPassword(textNewPassword.getText().toString());
            userData.setPasswordConfirmed(textPasswordConfirm.getText().toString());
        }
        userData.setBase64Image(base64Image);
        userData.setImageName("image.png");

        IbikeApplication.getTracker().sendEvent("Account", "Save", "Data", Long.valueOf(0));
        if (textNewPassword.getText().toString().length() != 0)
            IbikeApplication.getTracker().sendEvent("Account", "Save", "Password", Long.valueOf(0));
        return ret;
    }

    private void launchDeleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setMessage(IbikeApplication.getString("delete_account_text")).setTitle(IbikeApplication.getString("delete_account_title"));
        builder.setPositiveButton(IbikeApplication.getString("Delete"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void launchBackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setMessage(IbikeApplication.getString("account_not_saved"));
        builder.setPositiveButton(IbikeApplication.getString("account_dont_save"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });
        builder.setNegativeButton(IbikeApplication.getString("account_cancel"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.show();
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
        btnSave.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void disableButtons() {
        btnSave.setEnabled(false);
        btnDelete.setEnabled(false);
    }
}
