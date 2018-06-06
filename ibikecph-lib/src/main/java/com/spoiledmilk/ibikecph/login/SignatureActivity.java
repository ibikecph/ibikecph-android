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
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingActivity;
import com.spoiledmilk.ibikecph.util.IBikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.io.InputStream;
import java.net.URL;

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
    boolean normalUser = false;
    String validationMessage;
    String username;
    boolean fromSplashScreen = false;

    boolean inProgress = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signature_activity);
        Intent intentExtra = getIntent();

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
        normalUser = intentExtra.getBooleanExtra("normalUser", false);

        // Check if the user came from the splashActivity.
        if (intentExtra.hasExtra("fromSplashScreen")) {
            if (intentExtra.getExtras().getBoolean("fromSplashScreen")) {
                fromSplashScreen = true;
            }
            intentExtra.removeExtra("fromSplashScreen");
        }

        if (normalUser) {
            Log.d("DV", "In signatureActivity as a normal user");

            userData = new UserData(PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).getString("auth_token", ""), PreferenceManager
                    .getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1));


            new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.myLooper();
                    Looper.prepare();
                    showProgressDialog();

                    Message message = HTTPAccountHandler.performGetUser(userData);
                    Bundle data = message.getData();
                    userData.setEmail(data.getString("email"));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textName.setTypeface(IBikeApplication.getItalicFont());
                            textName.setText(userData.getEmail());
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lockIcon.setVisibility(View.VISIBLE);
                            headLine.setVisibility(View.VISIBLE);
                            explainingText.setVisibility(View.VISIBLE);
                            textLogedIn.setVisibility(View.VISIBLE);
                            textName.setVisibility(View.VISIBLE);
                            textNewPassword.setVisibility(View.VISIBLE);
                            savePassword.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });
                    dismissProgressDialog();
                }
            }).start();

            savePassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Util.isNetworkConnected(SignatureActivity.this)) {
                        Util.launchNoConnectionDialog(SignatureActivity.this);
                        return;
                    }
                    if (validatePasswords() && !inProgress) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.myLooper();
                                Looper.prepare();
                                showProgressDialog();
                                userData = new UserData(userData.getEmail(), textNewPassword.getText().toString());
                                Message message = HTTPAccountHandler.performLogin(userData, false);
                                Bundle data = message.getData();
                                boolean success = data.getBoolean("success");
                                if (success) {
                                    String signature = data.getString("signature");
                                    if (signature == null || signature.equals("") || signature.equals("null")) {
                                        signature = "";
                                    }
                                    Log.d("DV", "Vi har modtaget signature = " + signature);
                                    PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).edit().putString("signature", signature).commit();
                                    IBikePreferences settings = IBikeApplication.getSettings();
                                    settings.setTrackingEnabled(true);
                                    settings.setNotifyMilestone(true);
                                    settings.setNotifyWeekly(true);
                                    dismissProgressDialog();
                                    startActivity(new Intent(SignatureActivity.this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                } else {
                                    dismissProgressDialog();
                                    final String msg = data.getString("info");//data.containsKey("errors") ? data.getString("errors") : data.getString("info");
                                    String title = "";
                                    if (data.containsKey("info_title")) {
                                        title = data.getString("info_title");
                                    }
                                    final String finalTitle = title;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            launchErrorDialog(finalTitle, msg);
                                        }
                                    });

                                }

                            }
                        }).start();
                    } else {
                        launchAlertDialog(IBikeApplication.getString("register_error_fields"));
                    }
                }
            });
        } else {
            Log.d("DV", "In signatureActivity as a Facebook user");
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

                        Message message = HTTPAccountHandler.performHasPassword();
                        Bundle data = message.getData();
                        hasPassword = data.getBoolean("has_password", false);
                        if (hasPassword) {
                            Log.d("DV", "FB-user already has a signature on the server!");
                            hasPassword();
                        } else {
                            Log.d("DV", "FB-user deson't have a signature on the server!");
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
        }
        initStrings();
    }

    public void hasPassword() {
        Log.d("DV", "Running hasPassword stuff!");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lockIcon.setVisibility(View.VISIBLE);
                headLine.setVisibility(View.VISIBLE);
                explainingText.setVisibility(View.VISIBLE);
                textLogedIn.setVisibility(View.VISIBLE);
                pictureContainer.setVisibility(View.VISIBLE);
                textName.setVisibility(View.VISIBLE);
                textNewPassword.setVisibility(View.VISIBLE);
                savePassword.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Message message = HTTPAccountHandler.performGetUser(new UserData(IBikeApplication.getAuthToken(), PreferenceManager
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

                            /*Util.showSimpleMessageDlg(SignatureActivity.this, "Error fetching the user id = "
                                    + PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1) + " auth token = "
                                    + IBikeApplication.getAuthToken() + (message != null ? message.toString() : ""));*/

                            progressBar.setVisibility(View.GONE);
                            inProgress = false;
                        }
                    });
            }
        }).start();


        savePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Util.isNetworkConnected(SignatureActivity.this)) {
                    Util.launchNoConnectionDialog(SignatureActivity.this);
                    return;
                }
                if (validatePassword() && !inProgress) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.myLooper();
                            Looper.prepare();
                            showProgressDialog();
                            userData = new UserData(IBikeApplication.getEmail(), textNewPassword.getText().toString());
                            Message message = HTTPAccountHandler.performLogin(userData, true);
                            Bundle data = message.getData();
                            boolean success = data.getBoolean("success");
                            if (success) {
                                String signature = data.getString("signature");
                                if (signature == null || signature.equals("") || signature.equals("null")) {
                                    signature = "";
                                }
                                Log.d("DV", "Vi har modtaget signature = " + signature);
                                PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).edit().putString("signature", signature).commit();
                                IBikePreferences settings = IBikeApplication.getSettings();
                                settings.setTrackingEnabled(true);
                                settings.setNotifyMilestone(true);
                                settings.setNotifyWeekly(true);
                                dismissProgressDialog();
                                if (fromSplashScreen) {
                                    startActivity(new Intent(SignatureActivity.this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                } else {
                                    startActivity(new Intent(SignatureActivity.this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                }
                                finish();
                            } else {
                                dismissProgressDialog();
                                final String msg = data.getString("info");//data.containsKey("errors") ? data.getString("errors") : data.getString("info");
                                String title = "";
                                if (data.containsKey("info_title")) {
                                    title = data.getString("info_title");
                                }
                                final String finalTitle = title;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        launchErrorDialog(finalTitle, msg);
                                    }
                                });

                            }

                        }
                    }).start();
                } else if (!inProgress) {
                    launchAlertDialog(validationMessage);
                }
            }
        });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initStrings();
            }
        });
    }

    public void hasNoPassword() {
        Log.d("DV", "Running has no password stuff!");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);
                lockIcon.setVisibility(View.VISIBLE);
                headLine.setVisibility(View.VISIBLE);
                explainingText.setVisibility(View.VISIBLE);
                textLogedIn.setVisibility(View.VISIBLE);
                pictureContainer.setVisibility(View.VISIBLE);
                textName.setVisibility(View.VISIBLE);
                textNewPassword.setVisibility(View.VISIBLE);
                textPasswordConfirm.setVisibility(View.VISIBLE);
                savePassword.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Message message = HTTPAccountHandler.performGetUser(new UserData(IBikeApplication.getAuthToken(), PreferenceManager
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
                            /*
                            Util.showSimpleMessageDlg(SignatureActivity.this, "Error fetching the user id = "
                                    + PreferenceManager.getDefaultSharedPreferences(SignatureActivity.this).getInt("id", -1) + " auth token = "
                                    + IBikeApplication.getAuthToken() + (message != null ? message.toString() : ""));*/
                            progressBar.setVisibility(View.GONE);
                        }
                    });
            }
        }).start();


        //Styling/Listeners
        textNewPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);
                } else {
                    textNewPassword.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        textPasswordConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    textPasswordConfirm.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);
                } else {
                    textPasswordConfirm.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        savePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Util.isNetworkConnected(SignatureActivity.this)) {
                    Util.launchNoConnectionDialog(SignatureActivity.this);
                    return;
                }
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
                                IBikePreferences settings = IBikeApplication.getSettings();
                                settings.setTrackingEnabled(true);
                                settings.setNotifyMilestone(true);
                                settings.setNotifyWeekly(true);
                                if (fromSplashScreen) {
                                    startActivity(new Intent(SignatureActivity.this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));

                                } else {
                                    startActivity(new Intent(SignatureActivity.this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                }
                                finish();
                            } else {
                                dismissProgressDialog();
                                final String msg = data.getString("info");//data.containsKey("errors") ? data.getString("errors") : data.getString("info");
                                String title = "";
                                if (data.containsKey("info_title")) {
                                    title = data.getString("info_title");
                                }
                                final String finalTitle = title;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        launchErrorDialog(finalTitle, msg);
                                    }
                                });

                            }
                            SignatureActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                        }
                    }).start();
                    inProgress = false;
                } else if (!inProgress) {
                    launchAlertDialog(validationMessage);
                    inProgress = false;
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        progressBar.setVisibility(View.GONE);
    }

    private void initStrings() {
        lockIcon.setBackground(getResources().getDrawable(R.drawable.hanglock));
        //Load resources and text in relation to the server response
        if (hasPassword) {
            headLine.setText(IBikeApplication.getString("track_token_headline"));
            explainingText.setText(IBikeApplication.getString("track_token_description_facebook_has_token"));
            savePassword.setText(IBikeApplication.getString("use_password"));
            textLogedIn.setText(IBikeApplication.getString("track_token_subtitle_facebook"));
            textLogedIn.setTypeface(IBikeApplication.getItalicFont());
            textNewPassword.setHint(IBikeApplication.getString("register_password_placeholder"));
            textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
        } else if (!hasPassword && !normalUser) {
            this.getActionBar().setTitle(IBikeApplication.getString("track_token_title"));
            textLogedIn.setText(IBikeApplication.getString("track_token_subtitle_facebook"));
            textLogedIn.setTypeface(IBikeApplication.getItalicFont());
            headLine.setText(IBikeApplication.getString("track_token_headline"));
            explainingText.setText(IBikeApplication.getString("track_token_description_facebook_new"));
            savePassword.setText(IBikeApplication.getString("save_password"));
            textNewPassword.setHint(IBikeApplication.getString("register_password_placeholder"));
            textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
            textPasswordConfirm.setHint(IBikeApplication.getString("register_password_repeat_placeholder"));
            textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
        } else if (normalUser) {
            headLine.setText(IBikeApplication.getString("track_token_headline"));
            textLogedIn.setText(IBikeApplication.getString("track_token_subtitle_native"));
            textLogedIn.setTypeface(IBikeApplication.getItalicFont());
            explainingText.setText(IBikeApplication.getString("track_token_description_native"));
            textNewPassword.setHint(IBikeApplication.getString("register_password_placeholder"));
            textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
            savePassword.setText(IBikeApplication.getString("use_password"));
        }

    }

    private void launchAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignatureActivity.this);
        builder.setMessage(msg).setTitle(IBikeApplication.getString("Error"));
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
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (!textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
            validationMessage = IBikeApplication.getString("register_error_passwords");
            ret = false;
        }
        userData = new UserData(textNewPassword.getText().toString(), textPasswordConfirm.getText().toString());
        return ret;
    }

    private boolean validatePassword() {
        boolean ret = true;
        if (textNewPassword.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
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
                builder.setTitle(IBikeApplication.getString("Error"));
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
                            textName.setTypeface(IBikeApplication.getItalicFont());
                            textName.setText(username);
                        }
                    });
                }
            }
        }).start();
    }

}
