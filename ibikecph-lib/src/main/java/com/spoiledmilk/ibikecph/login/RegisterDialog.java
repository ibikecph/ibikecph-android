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
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;

public class RegisterDialog {

    TextView textDialogTitle;
    EditText textName;
    EditText textEmail;
    EditText textPassword;
    EditText textPasswordConfirm;
    ImageButton imgCamera;
    String validationMessage;
    CheckBox termsAcceptanceCheckbox;
    TextView termsAcceptanceLabel;
    TextView termsAcceptanceLink;
    TexturedButton btnRegister;

    Context context;
    Handler handler;
    String base64image = "";
    UserData userData;
    ProgressBar progressBar;
    Dialog dialog;

    public boolean inProgress = false;

    public void createRegisterDialog(final Context context) {
        this.context = context;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.register_fragment);
        progressBar = (ProgressBar) dialog.findViewById(R.id.progressBar);
        textDialogTitle = (TextView) dialog.findViewById(R.id.textCreateAccount);
        textName = (EditText) dialog.findViewById(R.id.textName);
        textEmail = (EditText) dialog.findViewById(R.id.textEmail);
        textPassword = (EditText) dialog.findViewById(R.id.textPassword);
        textPasswordConfirm = (EditText) dialog.findViewById(R.id.textPasswordConfirm);
        IbikeApplication.getTracker().sendEvent("Register", "Start", textEmail.getText().toString(), (long) 0);
        Button btnBack = (Button) dialog.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                IbikeApplication.getTracker().sendEvent("Register", "Cancel", textEmail.getText().toString(), (long) 0);
                dialog.dismiss();
            }

        });


        termsAcceptanceCheckbox = (CheckBox) dialog.findViewById(R.id.termsAcceptanceCheckbox);
        termsAcceptanceLabel = (TextView) dialog.findViewById(R.id.termsAcceptanceLabel);
        termsAcceptanceLink = (TextView) dialog.findViewById(R.id.termsAcceptanceLink);


        btnRegister = (TexturedButton) dialog.findViewById(R.id.btnRegister);
        btnRegister.setBackgroundResource(R.drawable.btn_grey_selector);
        btnRegister.setEnabled(false);
        btnRegister.setTextColor(Color.WHITE);
        btnRegister.setTextureResource(R.drawable.btn_pattern_repeteable);
        btnRegister.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (validateInput(textName.getText().toString(), textEmail.getText().toString(), textPassword.getText().toString(),
                        textPasswordConfirm.getText().toString())) {
                    showProgressDialog();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!inProgress) {
                                inProgress = true;
                                Looper.myLooper();
                                Looper.prepare();
                                showProgressDialog();
                                userData = new UserData(textName.getText().toString(), textEmail.getText().toString(), textPassword.getText()
                                        .toString(), textPasswordConfirm.getText().toString(), base64image, "image.jpg");
                                Message message = HTTPAccountHandler.performRegister(userData, context);
                                Boolean success = message.getData().getBoolean("success", false);
                                if (success) {
                                    IbikeApplication.saveEmail(userData.getEmail());
                                    IbikeApplication.savePassword(userData.getPassword());
                                }
                                handler.sendMessage(message);
                                IbikeApplication.getTracker().sendEvent("Register", "Completed", userData.getEmail(), (long) 0);
                                dismissProgressDialog();
                            }
                        }
                    }).start();

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(arg0.getContext());
                    builder.setMessage(validationMessage).setTitle(IbikeApplication.getString("Error"));
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
                        dialog.dismiss();
                        ((LoginSplashActivity) context).launchRegistrationDialog(data.get("info").toString());
                    } else {
                        launchErrorDialog(data.getString("info"));
                    }
                    return true;
                }
            })

            ;
        }
        textDialogTitle.setText(IbikeApplication.getString("create_account"));
        textDialogTitle.setTypeface(IbikeApplication.getBoldFont());
        textName.setHint(IbikeApplication.getString("register_name_placeholder"));
        textName.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        textEmail.setHint(IbikeApplication.getString("register_email_placeholder"));
        textEmail.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        textPassword.setHint(IbikeApplication.getString("register_password_placeholder"));
        textPassword.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        textPasswordConfirm.setHint(IbikeApplication.getString("register_password_repeat_placeholder"));
        textPasswordConfirm.setHintTextColor(context.getResources().getColor(R.color.HintColor));
        btnBack.setText(IbikeApplication.getString("back"));
        btnBack.setTypeface(IbikeApplication.getBoldFont());
        btnRegister.setText(IbikeApplication.getString("register_save"));
        btnRegister.setTypeface(IbikeApplication.getBoldFont());
        final Activity activity = (Activity) context;

        // Pick out the "Terms of Service" part of the "Accept the ..." string
        this.termsAcceptanceLabel.setText(IbikeApplication.getString("accept_user_terms").replace(IbikeApplication.getString("accept_user_terms_link_highlight"), ""));

        // Construct a link in HTML and make it clickable
        this.termsAcceptanceLink.setText(Html.fromHtml("<a href='" + IbikeApplication.getString("accept_user_terms_link") + "'>" + IbikeApplication.getString("accept_user_terms_link_highlight") + "</a>") );
        this.termsAcceptanceLink.setMovementMethod(LinkMovementMethod.getInstance());

        this.termsAcceptanceCheckbox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean isChecked = termsAcceptanceCheckbox.isChecked();
                btnRegister.setEnabled(isChecked);
                btnRegister.setBackgroundResource(isChecked?R.drawable.btn_blue_selector:R.drawable.btn_grey_selector);

            }
        });


        imgCamera = (ImageButton) dialog.findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent pickIntent = new Intent();
                pickIntent.setType("image/*");
                pickIntent.setAction(Intent.ACTION_GET_CONTENT);
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                String pickTitle = "";
                Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takePhotoIntent });
                activity.startActivityForResult(chooserIntent, LoginSplashActivity.IMAGE_REQUEST);

            }

        });

        dialog.getWindow().setLayout(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final Context contextFinal = context;
        dialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (contextFinal != null) {
                    ((LoginSplashActivity) contextFinal).onDialogDismissed();
                }

            }
        });

        try {
            dialog.show();
        } catch (Exception e) {

        }

    }

    private boolean validateInput(String name, String email, String password, String passwordConfirm) {
        boolean ret = true;
        if (name.length() == 0) {
            validationMessage = IbikeApplication.getString("name_blank");
            ret = false;
        } else if (email.length() == 0) {
            validationMessage = IbikeApplication.getString("email_blank");
            ret = false;
        } else if (password.length() == 0) {
            validationMessage = IbikeApplication.getString("password_blank");
            ret = false;
        } else if (passwordConfirm.length() == 0) {
            validationMessage = IbikeApplication.getString("password_confirm_blank");
            ret = false;
        } else if (textPasswordConfirm.getText().toString().length() < 3) {
            validationMessage = IbikeApplication.getString("password_short");
            ret = false;
        } else if (!validEmail(email)) {
            ret = false;
        } else if (!password.equals(passwordConfirm)) {
            validationMessage = IbikeApplication.getString("register_error_passwords");
            ret = false;
        } else if (password.length() < 3) {
            validationMessage = IbikeApplication.getString("register_error_passwords_short");
            ret = false;
        }
        return ret;
    }

    private boolean validEmail(String email) {
        boolean ret = true;
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            validationMessage = IbikeApplication.getString("register_error_invalid_email");
            ret = false;
        } else if (email.length() < atIndex + 2) {
            validationMessage = IbikeApplication.getString("register_error_invalid_email");
            ret = false;
        } else if (email.indexOf(".", atIndex) < atIndex + 1) {
            validationMessage = IbikeApplication.getString("register_error_invalid_email");
            ;
            ret = false;
        }
        return ret;
    }

    private void launchErrorDialog(String info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(info).setTitle(IbikeApplication.getString("Error"));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
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

    public void onImageSet(String base64image, Drawable imgDrawable) {
        showProgressDialog();
        this.base64image = base64image;
        imgCamera.setBackgroundColor(Color.TRANSPARENT);
        imgCamera.setScaleType(ScaleType.CENTER_CROP);
        imgCamera.setImageDrawable(imgDrawable);
        dismissProgressDialog();
    }

    public boolean isDialogAlive() {
        return (dialog != null && dialog.isShowing());
    }

    public String getName() {
        String ret = "";
        if (textName.getText() != null) {
            ret = textName.getText().toString();
        }
        return ret;
    }

    public String getEmail() {
        String ret = "";
        if (textEmail.getText() != null) {
            ret = textEmail.getText().toString();
        }
        return ret;
    }

    public String getPassword() {
        String ret = "";
        if (textPassword.getText() != null) {
            ret = textPassword.getText().toString();
        }
        return ret;
    }

    public String getPasswordConfirmation() {
        String ret = "";
        if (textPasswordConfirm.getText() != null) {
            ret = textPasswordConfirm.getText().toString();
        }
        return ret;
    }

    public void setName(String string) {
        textName.setText(string);
    }

    public void setEmail(String string) {
        textEmail.setText(string);
    }

    public void setPassword(String string) {
        textPassword.setText(string);
    }

    public void setPasswordConfirmation(String string) {
        textPasswordConfirm.setText(string);
    }

}
