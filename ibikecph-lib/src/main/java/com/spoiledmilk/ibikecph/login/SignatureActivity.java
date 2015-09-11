package com.spoiledmilk.ibikecph.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.analytics.tracking.android.EasyTracker;
import com.makeramen.roundedimageview.RoundedImageView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.util.AsyncImageFetcher;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.ImageData;
import com.spoiledmilk.ibikecph.util.LOG;

import java.util.Arrays;

/**
 * Created by Udvikler on 11-09-2015.
 */
public class SignatureActivity extends Activity {
    EditText textNewPassword;
    EditText textPasswordConfirm;

    ImageView lockIcon;
    TextView headLine;
    TextView explainingText;
    Button savePassword;
    Button cancelButton;

    UserData userData;
    ProgressBar progressBar;

    boolean isRunning = true;
    boolean fromTracking = false;
    String validationMessage;
    String base64Image = "";

    boolean inProgress = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        setContentView(R.layout.signature_activity);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        textNewPassword = (EditText) findViewById(R.id.textNewPassword);
        textPasswordConfirm = (EditText) findViewById(R.id.textPasswordConfirm);

        fromTracking = intent.getBooleanExtra("fromTracking", false);
        if (fromTracking) {
            Log.d("DV", "SignatureActivity: Kommer fra tracking!");
            //Make views gone!
            //Maybe remove.
            RoundedImageView riv;
            riv = (RoundedImageView) findViewById(R.id.pictureContainer);
            riv.setVisibility(View.GONE);

            //Make views visible!
            lockIcon = (ImageView) findViewById(R.id.lockIcon);
            headLine = (TextView) findViewById(R.id.headLine);
            explainingText = (TextView) findViewById(R.id.explainingText);
            savePassword = (Button) findViewById(R.id.savePassword);
            cancelButton = (Button) findViewById(R.id.cancelButton);

            lockIcon.setVisibility(View.VISIBLE);
            headLine.setVisibility(View.VISIBLE);
            explainingText.setVisibility(View.VISIBLE);
            savePassword.setVisibility(View.VISIBLE);

            headLine.setText(IbikeApplication.getString("track_token_headline"));
            explainingText.setText(IbikeApplication.getString("track_token_description_facebook_new"));
            savePassword.setText(IbikeApplication.getString("save_password"));

            //Styling/Listeners
            textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
            textNewPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (b) {
                        textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
                    } else {
                        textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            });

            textPasswordConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (b) {
                        textPasswordConfirm.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
                    } else {
                        textPasswordConfirm.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                    }
                }
            });

            savePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (validatePasswords() && !inProgress) {
                        inProgress = true;

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.myLooper();
                                Looper.prepare();
                                SignatureActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.VISIBLE);
                                    }
                                });
                                Message message = HTTPAccountHandler.performAddPassword(userData, SignatureActivity.this);
                                Bundle data = message.getData();
                                Boolean success = data.getBoolean("success", false);
                                if (success) {
                                    //Save signature token
                                    String signature = data.getString("signature");
                                    PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).edit().putString("signature", signature).commit();
                                    Log.d("DV", "We got a signature, enabling tracking!");
                                    IbikePreferences settings = IbikeApplication.getSettings();
                                    settings.setTrackingEnabled(true);
                                    settings.setNotifyMilestone(true);
                                    settings.setNotifyWeekly(true);
                                    startActivity(new Intent(SignatureActivity.this, TrackingActivity.class));
                                    finish();
                                }
                                SignatureActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });

                            }
                        }).start();
                    } else if (!inProgress) {
                        launchAlertDialog(validationMessage);
                    }
                }
            });

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressBar.setVisibility(View.GONE);
    }

    private void initStrings() {
        if (fromTracking) {
            this.getActionBar().setTitle(IbikeApplication.getString("track_token_title"));

            //Load resources and text
            lockIcon.setBackground(getResources().getDrawable(R.drawable.tracking_from_to));
            headLine.setText("Vi går op i at dine data er anonyme!");
            explainingText.setText("For at sikre at dine data kan deles med Københavns Kommune og Region Hovedstaden, uden at vi kan kæde dem sammen med dig, har vi brug for at du opretter er kodeord kun du kender.");
            savePassword.setText("Gem kodeord");

            textNewPassword.setHint(IbikeApplication.getString("register_password_placeholder")); //ny der skriver kodeord?
            textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
            textPasswordConfirm.setHint(IbikeApplication.getString("register_password_repeat_placeholder")); //ny der skrive gentag kodeord?
            textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
        }
    }

    private void launchAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignatureActivity.this);
        builder.setMessage(msg).setTitle(IbikeApplication.getString("Error"));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean validatePasswords() {
        boolean ret = true;
        if (textNewPassword.getText().toString().length() == 0) {
            validationMessage = IbikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() == 0) {
            validationMessage = IbikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() < 3) {
            validationMessage = IbikeApplication.getString("register_error_passwords_short");
            ret = false;
        } else if (!textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
            validationMessage = IbikeApplication.getString("register_error_passwords");
            ret = false;
        } else if (textNewPassword.getText().toString().length() < 3) {
            validationMessage = IbikeApplication.getString("register_error_passwords_short");
            ret = false;
        }
        userData = new UserData(textNewPassword.getText().toString(), textPasswordConfirm.getText().toString());
        return ret;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /*
    @Override
    public void onImagePrefetched(ImageData imageData) {
        if (imageData != null && imageData.bmp != null && imageData.base64 != null) {
            base64Image = imageData.base64;
            RoundedImageView riv;
            riv = (RoundedImageView) findViewById(R.id.pictureContainer);
            riv.findViewById(R.id.pictureContainer);
            riv.setImageDrawable(imageData.bmp);
            riv.setScaleType(ImageView.ScaleType.CENTER);
            //((RoundedImageView) findViewById(R.id.pictureContainer)).setImageDrawable(imageData.bmp);
            //findViewById(R.id.frame).setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Error fetching the image", Toast.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(View.GONE);
    }*/


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

}
