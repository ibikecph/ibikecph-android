package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.ReadMoreActivity;
//import com.spoiledmilk.ibikecph.login.LoginActivity;
//import com.spoiledmilk.ibikecph.login.SignatureActivity;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

public class TrackingWelcomeActivity extends Activity {

    private TextView welcomeExplanationTextView, welcomeTextView;
    private Button enableButton, readMoreButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_welcome);

        enableButton = (Button) findViewById(R.id.tracking_enable);
        readMoreButton = (Button) findViewById(R.id.readMoreButton);
        welcomeExplanationTextView = (TextView) findViewById(R.id.welcomeExplanationTextView);
        welcomeTextView = (TextView) findViewById(R.id.welcomeTextView);

        this.getActionBar().hide();
        initStrings();
    }

   /* public void onTrackingEnableClick(View v) {
        // IF the user is not logged in, spawn a dialog saying so.
        if (!IBikeApplication.isUserLogedIn() && !IBikeApplication.isFacebookLogin()) {
            MustLogInDialogFragment loginDialog = new MustLogInDialogFragment();
            loginDialog.show(getFragmentManager(), "MustLoginDialog");

        } else {
            if (IBikeApplication.getSignature().equals("")) {
                if (IBikeApplication.isFacebookLogin()) {
                    Log.d("DV", "Prompting Facebookuser to create a password!");
                    Intent i = new Intent(TrackingWelcomeActivity.this, SignatureActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Log.d("DV", "Starting activity with resultcode = 99");
                    startActivityForResult(i, 99);
                    finish();
                } else if (IBikeApplication.isUserLogedIn()) {
                    Log.d("DV", "Prompting login for user!");
                    Intent i = new Intent(TrackingWelcomeActivity.this, SignatureActivity.class).putExtra("normalUser", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(i, 10);
                    finish();
                }
            } else {
                Log.d("DV", "We got a signature, enabling tracking!");
                IBikePreferences settings = IBikeApplication.getSettings();
                settings.setTrackingEnabled(true);
                settings.setNotifyMilestone(true);
                settings.setNotifyWeekly(true);
                startActivity(new Intent(this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        }
    }*/

    public void onReadMoreClick(View v) {
        Intent i = new Intent(TrackingWelcomeActivity.this, ReadMoreActivity.class);
        TrackingWelcomeActivity.this.startActivity(i);
    }

    private void initStrings() {
        welcomeTextView.setText(IBikeApplication.getString("launch_activate_tracking_title_no_welcome"));
        welcomeExplanationTextView.setText(IBikeApplication.getString("launch_activate_tracking_description"));
        readMoreButton.setText(IBikeApplication.getString("launch_activate_tracking_read_more"));
        enableButton.setText(IBikeApplication.getString("enable_tracking"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tell Google Analytics that the user has resumed on this screen.
        IBikeApplication.sendGoogleAnalyticsActivityEvent(this);

        if (IBikeApplication.getSettings().getTrackingEnabled()) {
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

    /*public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // If we got back from a login box AND the used successfully logged in, go on.
        if (requestCode == LeftMenu.LAUNCH_LOGIN && resultCode == RESULT_OK) {
            // (actually, just mimic a button click)
            onTrackingEnableClick(null);
        }

        if (requestCode == 99) {
            Log.d("DV", "Result code = 99");
        }
    }*/


    /*public static class MustLogInDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(IBikeApplication.getString("log_in_to_track_prompt"))
                    .setPositiveButton(IBikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent i = new Intent(getActivity(), LoginActivity.class);
                            getActivity().startActivityForResult(i, LeftMenu.LAUNCH_LOGIN);
                        }
                    })
                    .setNegativeButton(IBikeApplication.getString("account_cancel"), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }*/
}

