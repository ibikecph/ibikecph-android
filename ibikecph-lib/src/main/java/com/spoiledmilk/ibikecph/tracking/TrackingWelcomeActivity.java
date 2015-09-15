package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.RegisterActivity;
import com.spoiledmilk.ibikecph.login.SignatureActivity;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

public class TrackingWelcomeActivity extends Activity {

    private TextView introText, enableText, termsText, termsLinkText, kmText, kmtText, kmPrTripText, hoursText;
    private Button enableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_welcome);

        this.introText = (TextView) findViewById(R.id.tracking_intro);
        this.enableText = (TextView) findViewById(R.id.tracking_enable_text);
        this.termsText = (TextView) findViewById(R.id.tracking_terms);
        this.termsLinkText = (TextView) findViewById(R.id.tracking_terms_link);
        this.enableButton = (Button) findViewById(R.id.tracking_enable);

        this.kmText = (TextView) findViewById(R.id.kmText);
        this.kmtText = (TextView) findViewById(R.id.kmtText);
        this.kmPrTripText = (TextView) findViewById(R.id.kmPrTripText);
        this.hoursText = (TextView) findViewById(R.id.hoursText);

        try {
            this.getActionBar().setTitle(IbikeApplication.getString("tracking"));
        } catch (NullPointerException e) {
            // There was no ActionBar. Oh well...
        }

        //this.enableButton.setEnabled();

        initStrings();
    }

    public void onTrackingEnableClick(View v) {
        // IF the user is not logged in, spawn a dialog saying so.
        if (!IbikeApplication.isUserLogedIn() && !IbikeApplication.isFacebookLogin()) {
            MustLogInDialogFragment loginDialog = new MustLogInDialogFragment();
            loginDialog.show(getFragmentManager(), "MustLoginDialog");

        } else {
            if (IbikeApplication.getSignature().equals("")) {
                if (IbikeApplication.isFacebookLogin()) {
                    Log.d("DV", "Prompting Facebookuser to create a password!");
                    Intent i = new Intent(TrackingWelcomeActivity.this, SignatureActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Log.d("DV", "Starting activity with resultcode = 99");
                    startActivityForResult(i, 99);
                    finish();
                } else if (IbikeApplication.isUserLogedIn()) {
                    Log.d("DV", "Prompting login for user!");
                    Intent i = new Intent(TrackingWelcomeActivity.this, SignatureActivity.class).putExtra("normalUser", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(i, 10);
                    finish();
                }
            } else {
                Log.d("DV", "We got a signature, enabling tracking!");
                IbikePreferences settings = IbikeApplication.getSettings();
                settings.setTrackingEnabled(true);
                settings.setNotifyMilestone(true);
                settings.setNotifyWeekly(true);
                startActivity(new Intent(this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        }
    }

    public void onTermsClick(View v) {
        HttpUtils.openLinkInBrowser(this, Config.TRACKING_TERMS_URL);
    }

    private void initStrings() {
        this.introText.setText(IbikeApplication.getString("enable_tracking_description"));
        this.enableText.setText(IbikeApplication.getString("enable_tracking_explanation"));

        this.termsText.setText(IbikeApplication.getString("tracking_terms"));
        this.termsLinkText.setText(IbikeApplication.getString("tracking_terms_link"));

        this.termsLinkText.setPaintFlags(this.termsLinkText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        this.enableButton.setText(IbikeApplication.getString("enable_tracking"));

        this.kmText.setText(IbikeApplication.getString("unit_km").toUpperCase());
        this.kmtText.setText(IbikeApplication.getString("unit_km_pr_h").toUpperCase());
        this.kmPrTripText.setText(IbikeApplication.getString("unit_km_pr_trip").toUpperCase());
        this.hoursText.setText(IbikeApplication.getString("unit_h_long").toUpperCase());
    }


    public static class MustLogInDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(IbikeApplication.getString("log_in_to_track_prompt"))
                    .setPositiveButton(IbikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent i = new Intent(getActivity(), LoginActivity.class);
                            getActivity().startActivityForResult(i, LeftMenu.LAUNCH_LOGIN);
                        }
                    })
                    .setNegativeButton(IbikeApplication.getString("account_cancel"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (IbikeApplication.getSettings().getTrackingEnabled()) {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // If we got back from a login box AND the used successfully logged in, go on.
        if (requestCode == LeftMenu.LAUNCH_LOGIN && resultCode == RESULT_OK) {
            // (actually, just mimic a button click)
            onTrackingEnableClick(null);
        }

        if (requestCode == 99) {
            Log.d("DV", "Result code = 99");
        }
    }
}

