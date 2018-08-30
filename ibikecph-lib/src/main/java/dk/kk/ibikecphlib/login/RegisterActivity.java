// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.login;

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
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.*;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.model.GraphUser;
import com.makeramen.roundedimageview.RoundedImageView;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.util.AsyncImageFetcher;
import dk.kk.ibikecphlib.util.ImageData;
import dk.kk.ibikecphlib.util.ImagerPrefetcherListener;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import java.util.Arrays;

import dk.kk.ibikecphlib.util.Util;

public class RegisterActivity extends Activity implements ImagerPrefetcherListener, FBLoginListener {
    EditText textName;
    EditText textEmail;
    EditText textNewPassword;
    EditText textPasswordConfirm;
    Button btnRegister;
    Button btnFacebookLogin;
    CheckBox termsAcceptanceCheckbox;
    TextView termsAcceptanceLabel;
    TextView termsAcceptanceLink;
    TextView textOr;

    Handler handler, facebookHandler;

    UserData userData;

    ProgressBar progressBar;
    private Session.StatusCallback statusCallback = new SessionStatusCallback();

    boolean isRunning = true;
    String fbToken;
    String validationMessage;
    String base64Image = "";
    private static final int IMAGE_REQUEST = 1888;

    public static final int RESULT_USER_DELETED = 101;
    public static final int RESULT_ACCOUNT_REGISTERED = 102;
    public static final int RESULT_NO_ACTION = 103;
    public static final int RESULT_FACEBOOK_REGISTERED = 104;

    boolean inProgress = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.register_activity);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        textOr = (TextView) findViewById(R.id.textOr);
        textName = (EditText) findViewById(R.id.textName);
        textEmail = (EditText) findViewById(R.id.textEmail);
        textNewPassword = (EditText) findViewById(R.id.textNewPassword);
        textPasswordConfirm = (EditText) findViewById(R.id.textPasswordConfirm);

        termsAcceptanceCheckbox = (CheckBox) findViewById(R.id.termsAcceptanceCheckbox);
        termsAcceptanceLabel = (TextView) findViewById(R.id.termsAcceptanceLabel);
        termsAcceptanceLink = (TextView) findViewById(R.id.termsAcceptanceLink);

        btnFacebookLogin = (Button) findViewById(R.id.btnFacebookLogin);
        btnRegister = (Button) findViewById(R.id.btnRegister);

        btnRegister.setEnabled(false);
        btnRegister.setBackground(getResources().getDrawable(R.drawable.stroke_button_inverted_ltgray));

        btnRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Util.isNetworkConnected(RegisterActivity.this)) {
                    Util.launchNoConnectionDialog(RegisterActivity.this);
                    return;
                }
                if (validateInput() && !inProgress) {
                    inProgress = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.myLooper();
                            Looper.prepare();
                            RegisterActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.VISIBLE);
                                }
                            });
                            Message message = HTTPAccountHandler.performRegister(userData, RegisterActivity.this);
                            Boolean success = message.getData().getBoolean("success", false);
                            if (success) {
                                IBikeApplication.saveEmail(userData.getEmail());
                                //IBikeApplication.savePassword(userData.getPassword());
                            }
                            handler.sendMessage(message);

                            // TODO: Change this to the implementation described here
                            // https://developers.google.com/analytics/devguides/collection/android/v4/#send-an-event
                            // IBikeApplication.getTracker().sendEvent("Register", "Completed", userData.getEmail(), (long) 0);

                            RegisterActivity.this.runOnUiThread(new Runnable() {
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


        if (handler == null) {
            handler = new Handler(new Handler.Callback() {

                @Override
                public boolean handleMessage(Message msg) {

                    Bundle data = msg.getData();
                    int msgType = data.getInt("type");
                    Boolean success = false;
                    inProgress = false;
                    switch (msgType) {
                        case HTTPAccountHandler.REGISTER_USER:
                            success = data.getBoolean("success");
                            if (!success) {
                                launchAlertDialog(data.getString("info"));
                            } else {
                                setResult(RESULT_ACCOUNT_REGISTERED);
                                finish();
                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            }
                            break;
                        case HTTPAccountHandler.ERROR:
                            launchAlertDialog(IBikeApplication.getString("Error"));
                            break;
                    }
                    return true;
                }
            });
        }

        if (facebookHandler == null) {
            facebookHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    Log.d("JC", "Received Facebook handler callback");

                    Bundle data = msg.getData();
                    Boolean success = data.getBoolean("success");
                    if (success) {
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
                            PreferenceManager.getDefaultSharedPreferences(RegisterActivity.this).edit().putString("auth_token", auth_token).commit();
                            PreferenceManager.getDefaultSharedPreferences(RegisterActivity.this).edit().putInt("id", id).commit();
                            LOG.d("Loged in token = " + auth_token + ", id = " + id);

                            setResult(RESULT_FACEBOOK_REGISTERED);
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

        btnFacebookLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!Util.isNetworkConnected(RegisterActivity.this)) {
                    Util.launchNoConnectionDialog(RegisterActivity.this);
                    return;
                }
                Log.d("DV", "facebook btn clicked!");
                performFBLogin(savedInstanceState);
            }
        });

        textName.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);

        textName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    textName.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);
                } else {
                    textName.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        textEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    textEmail.getBackground().setColorFilter(getResources().getColor(R.color.PrimaryColor), PorterDuff.Mode.SRC_ATOP);
                } else {
                    textEmail.getBackground().setColorFilter(getResources().getColor(R.color.Grey), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

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

    private void performFBLogin(Bundle savedInstanceState) {
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(RegisterActivity.this, null, statusCallback, savedInstanceState);
            }
            if (session == null) {
                session = new Session(RegisterActivity.this);
            }
        }
        Session.setActiveSession(session);
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED) || (!session.isOpened() && !session.isClosed())) {
            session.openForRead(new Session.OpenRequest(RegisterActivity.this).setCallback(statusCallback).setPermissions(Arrays.asList("email")));
        } else if (session.isOpened() && !session.getPermissions().contains("email")) {
            session.requestNewPublishPermissions(new NewPermissionsRequest(RegisterActivity.this, Arrays.asList("email")).setCallback(statusCallback));
        } else {
            Session.openActiveSession(RegisterActivity.this, true, statusCallback);
        }
    }

    private void initStrings() {

        this.getActionBar().setTitle(IBikeApplication.getString("create_account"));

        // Pick out the "Terms of Service" part of the "Accept the ..." string
        this.termsAcceptanceLabel.setText(IBikeApplication.getString("accept_user_terms").replace(IBikeApplication.getString("accept_user_terms_link_highlight"), ""));

        // Construct a link in HTML and make it clickable
        this.termsAcceptanceLink.setText(Html.fromHtml("<a href='" + IBikeApplication.getString("accept_user_terms_link") + "'>" + IBikeApplication.getString("accept_user_terms_link_highlight") + "</a>"));
        this.termsAcceptanceLink.setMovementMethod(LinkMovementMethod.getInstance());

        textOr.setText(IBikeApplication.getString("or"));
        textNewPassword.setHint(IBikeApplication.getString("register_password_placeholder"));
        textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
        textPasswordConfirm.setHint(IBikeApplication.getString("register_password_repeat_placeholder"));
        textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
        btnRegister.setText(IBikeApplication.getString("register_save"));
        textName.setHint(IBikeApplication.getString("register_name_placeholder"));
        textName.setHintTextColor(getResources().getColor(R.color.HintColor));
        textEmail.setHint(IBikeApplication.getString("register_email_placeholder"));
        textEmail.setHintTextColor(getResources().getColor(R.color.HintColor));
        btnFacebookLogin.setText(IBikeApplication.getString("create_with_fb"));

    }

    private void launchAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
        builder.setMessage(msg).setTitle(IBikeApplication.getString("Error"));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // "email_blank" = "Email can't be blank";
    // "name_blank" = "Name can't be blank";
    // "password_blank" = "Password can't be blank";
    // "password_confirm_blank" = "Password confirmation can't be blank";
    // "password_short" = "Password is too short (minimum is 3 characters)";

    private boolean validateInput() {
        boolean ret = true;
        if (textName.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textEmail.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textNewPassword.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (!textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
            validationMessage = IBikeApplication.getString("register_error_passwords");
            ret = false;
        } else {
            int atIndex = textEmail.getText().toString().indexOf('@');
            if (atIndex < 1) {
                validationMessage = IBikeApplication.getString("register_error_invalid_email");
                ret = false;
            }
            int pointIndex = textEmail.getText().toString().indexOf('.', atIndex);
            if (pointIndex < atIndex || pointIndex == textEmail.getText().toString().length() - 1) {
                validationMessage = IBikeApplication.getString("register_error_invalid_email");
                ret = false;
            }
        }
        userData = new UserData(textName.getText().toString(), textEmail.getText().toString(), textNewPassword.getText().toString(),
                textPasswordConfirm.getText().toString(), base64Image, "image.png");
        return ret;
    }

    private boolean validatePasswords() {
        boolean ret = true;
        if (textNewPassword.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() == 0) {
            validationMessage = IBikeApplication.getString("register_error_fields");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() < 3) {
            validationMessage = IBikeApplication.getString("register_error_passwords_short");
            ret = false;
        } else if (!textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
            validationMessage = IBikeApplication.getString("register_error_passwords");
            ret = false;
        } else if (textNewPassword.getText().toString().length() < 3) {
            validationMessage = IBikeApplication.getString("register_error_passwords_short");
            ret = false;
        }
        userData = new UserData(textNewPassword.getText().toString(), textPasswordConfirm.getText().toString());
        return ret;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            progressBar.setVisibility(View.VISIBLE);
            AsyncImageFetcher aif = new AsyncImageFetcher(this, this);
            aif.execute(data);
        }
        if (resultCode == RegisterActivity.RESULT_ACCOUNT_REGISTERED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(IBikeApplication.getString("register_successful"));
            builder.setPositiveButton(IBikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
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
    }

    @Override
    public void onStop() {
        super.onStop();
        isRunning = false;
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().removeCallback(statusCallback);
        }
    }

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
    }

    public void onTermsAcceptanceCheckboxClick(View v) {
        boolean isChecked = this.termsAcceptanceCheckbox.isChecked();
        btnRegister.setEnabled(isChecked);
        btnRegister.setBackground(isChecked ? getResources().getDrawable(R.drawable.stroke_button_inverted_red) : getResources().getDrawable(R.drawable.stroke_button_inverted_ltgray));
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

    @Override
    public void onFBLoginSuccess(String token) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IBikeApplication.setIsFacebookLogin(true);
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

    private void login() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper();
                Looper.prepare();
                showProgressDialog();
                LOG.d("fbdebug fbtoken = " + Session.getActiveSession().getAccessToken());
                Message message = HTTPAccountHandler.performFacebookLogin(Session.getActiveSession().getAccessToken());
                facebookHandler.sendMessage(message);
                dismissProgressDialog();

            }
        }).start();
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
                            HTTPAccountHandler.checkIsFbTokenValid(Session.getActiveSession().getAccessToken(), RegisterActivity.this);
                        }
                    }
                });
            }
        }
    }

}
