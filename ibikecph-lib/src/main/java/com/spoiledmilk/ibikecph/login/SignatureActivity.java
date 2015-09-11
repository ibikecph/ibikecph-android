package com.spoiledmilk.ibikecph.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import com.spoiledmilk.ibikecph.util.Util;

import java.io.InputStream;
import java.net.URL;
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
    TextView textName;
    TextView textLogedIn;
    Button savePassword;
    RoundedImageView pictureContainer;

    UserData userData;
    ProgressBar progressBar;

    boolean isRunning = true;
    boolean hasPassword = false;
    String validationMessage;
    String username;

    boolean inProgress = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signature_activity);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        pictureContainer = (RoundedImageView) findViewById(R.id.pictureContainer);
        textName = (TextView) findViewById(R.id.textName);
        textLogedIn = (TextView) findViewById(R.id.textLogedIn);
        textNewPassword = (EditText) findViewById(R.id.textNewPassword);
        textPasswordConfirm = (EditText) findViewById(R.id.textPasswordConfirm);
        lockIcon = (ImageView) findViewById(R.id.lockIcon);
        headLine = (TextView) findViewById(R.id.headLine);
        explainingText = (TextView) findViewById(R.id.explainingText);
        savePassword = (Button) findViewById(R.id.savePassword);

        Log.d("DV", "In signatureActivity!");
        //Check if the FB-user already has a signature on the server
        if (!inProgress) {
            inProgress = true;
            SignatureActivity.this.runOnUiThread(new Runnable() {
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

                    Message message = HTTPAccountHandler.performHasPassword(userData, SignatureActivity.this);
                    Bundle data = message.getData();
                    hasPassword = data.getBoolean("has_password", false);
                    if (hasPassword) {
                        Log.d("DV", "FB-user already has a signature on the server!");
                        //Save signature token?
                        //PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).edit().putString("signature", signature).commit();
                        /*IbikePreferences settings = IbikeApplication.getSettings();
                        settings.setTrackingEnabled(true);
                        settings.setNotifyMilestone(true);
                        settings.setNotifyWeekly(true);*/
                        //startActivity(new Intent(SignatureActivity.this, TrackingActivity.class));
                        //finish();
                        hasPassword();
                    } else {
                        hasNoPassword();
                    }
                    SignatureActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            inProgress = false;
                        }
                    });

                }
            }).start();
        }

        //Handle views etc. in relation to the server response
    }

    public void hasPassword() {
        Log.d("DV", "Running hasPassword stuff!");
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Message message = HTTPAccountHandler.performGetUser(new UserData(IbikeApplication.getAuthToken(), PreferenceManager
                        .getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1)));
                final Bundle data = message.getData();
                Boolean success = false;
                success = data.getBoolean("success");
                if (success) {
                    username = data.getString("name");
                    LoadImageFromWebOperations(data.getString("image_url"));
                } else
                    SignatureActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //logout();
                            Util.showSimpleMessageDlg(SignatureActivity.this, "Error fetching the user id = "
                                    + PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1) + " auth token = "
                                    + IbikeApplication.getAuthToken() + (message != null ? message.toString() : ""));
                            progressBar.setVisibility(View.GONE);
                            inProgress = false;
                        }
                    });
            }
        }).start();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textPasswordConfirm.setVisibility(View.GONE);
            }
        });
    }

    public void hasNoPassword() {
        Log.d("DV", "Running has no password stuff!");

        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Message message = HTTPAccountHandler.performGetUser(new UserData(IbikeApplication.getAuthToken(), PreferenceManager
                        .getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1)));
                final Bundle data = message.getData();
                Boolean success = false;
                success = data.getBoolean("success");
                if (success) {
                    username = data.getString("name");
                    LoadImageFromWebOperations(data.getString("image_url"));
                } else
                    SignatureActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //logout();
                            Util.showSimpleMessageDlg(SignatureActivity.this, "Error fetching the user id = "
                                    + PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1) + " auth token = "
                                    + IbikeApplication.getAuthToken() + (message != null ? message.toString() : ""));
                            progressBar.setVisibility(View.GONE);
                        }
                    });
            }
        }).start();


        //Styling/Listeners
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.app_primary_color), PorterDuff.Mode.SRC_ATOP);
            }
        });

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

    public void normalUser() {
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
        lockIcon.setBackground(getResources().getDrawable(R.drawable.tracking_from_to));
        //Load resources and text in relation to the server response
        if (hasPassword) {
            explainingText.setText(IbikeApplication.getString("track_token_description_facebook_has_token"));
            savePassword.setText(IbikeApplication.getString("use_password"));
            textLogedIn.setText(IbikeApplication.getString("you_are_logged_in_as"));
            textLogedIn.setTypeface(IbikeApplication.getItalicFont());
        } else if (!hasPassword) {
            this.getActionBar().setTitle(IbikeApplication.getString("track_token_title"));
            textLogedIn.setText(IbikeApplication.getString("you_are_logged_in_as"));
            textLogedIn.setTypeface(IbikeApplication.getItalicFont());
            headLine.setText(IbikeApplication.getString("track_token_headline"));
            explainingText.setText(IbikeApplication.getString("track_token_description_facebook_new"));
            savePassword.setText(IbikeApplication.getString("save_password"));
            textNewPassword.setHint(IbikeApplication.getString("register_password_placeholder"));
            textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
            textPasswordConfirm.setHint(IbikeApplication.getString("register_password_repeat_placeholder"));
            textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
        }

        //else if(){}normal user?

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
                    SignatureActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pictureContainer.setImageBitmap(profilePic);
                            pictureContainer.setScaleType(ImageView.ScaleType.CENTER);
                            pictureContainer.invalidate();
                            inProgress = false;
                        }
                    });
                } catch (Exception e) {
                    LOG.e(e.getLocalizedMessage());
                } finally {
                    SignatureActivity.this.runOnUiThread(new Runnable() {
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

}
