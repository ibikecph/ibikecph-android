package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.login.RegisterActivity;
import com.spoiledmilk.ibikecph.login.SignatureActivity;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

public class TrackingSettingsActivity extends Activity {

    private TextView trackingEnableText;
    private Switch trackingEnableSwitch;

    private TextView notifyMilestoneText;
    private Switch notifyMilestoneCheckbox;
    private TextView notifyWeeklyText;
    private Switch notifyWeeklyCheckbox;

    private TextView shareDataText;
    private Switch shareDataSwitch;
    private TextView shareDataInfoText;
    private TextView shareDataUsageText;
    private TextView shareDataTermsText;

    private IbikePreferences settings;
    private boolean loggedIn;
    private boolean checkedFromResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_settings);

        this.settings = IbikeApplication.getSettings();
        this.trackingEnableText = (TextView) findViewById(R.id.tracking_enable_text);
        this.trackingEnableSwitch = (Switch) findViewById(R.id.tracking_enable_switch);
        this.notifyMilestoneText = (TextView) findViewById(R.id.notify_milestone_text);
        this.notifyMilestoneCheckbox = (Switch) findViewById(R.id.notify_milestone_checkbox);

        this.notifyWeeklyText = (TextView) findViewById(R.id.notify_weekly_text);
        this.notifyWeeklyCheckbox = (Switch) findViewById(R.id.notify_weekly_checkbox);
        /*
        this.shareDataText           = (TextView) findViewById(R.id.share_data_text);
        this.shareDataSwitch         = (Switch)   findViewById(R.id.share_data_switch);
        this.shareDataInfoText       = (TextView) findViewById(R.id.share_data_info_text);
        this.shareDataUsageText      = (TextView) findViewById(R.id.share_data_usage_text);
        this.shareDataTermsText      = (TextView) findViewById(R.id.share_data_terms_text);
        */

        loggedIn = IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin();

        this.trackingEnableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (loggedIn && !checkedFromResume) {

                    if (IbikeApplication.getSignature().equals("")) {
                        if (IbikeApplication.isFacebookLogin()) {
                            Log.d("DV", "Prompting Facebookuser to create a password!");
                            Intent i = new Intent(TrackingSettingsActivity.this, SignatureActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        } else if (IbikeApplication.isUserLogedIn()) {
                            Log.d("DV", "Prompting login for user!");
                            Intent i = new Intent(TrackingSettingsActivity.this, SignatureActivity.class).putExtra("normalUser", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                    } else {
                        Log.d("DV", "We got a signature, enabling tracking!");
                        onEnableTrackingClick(isChecked);
                    }
                } else {
                    spawnLoginBox();
                }
            }
        });
        this.notifyMilestoneCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (loggedIn) {
                    onNotifyMilestone(isChecked);
                } else {
                    spawnLoginBox();
                }
            }
        });
        this.notifyWeeklyCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (loggedIn) {
                    onNotifyWeekly(isChecked);
                } else {
                    spawnLoginBox();
                }
            }
        });

        initStrings();

        updateEnablednessOfMilestoneSwitches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (trackingEnableSwitch.isChecked()) {
            if (IbikeApplication.getSignature().equals("")) {
                checkedFromResume = true;
                trackingEnableSwitch.setChecked(false);
                notifyMilestoneCheckbox.setChecked(false);
                notifyWeeklyCheckbox.setChecked(false);
                finish();
            }
        }

    }

    public void onEnableTrackingClick(boolean isChecked) {
        if (!loggedIn) {
            trackingEnableSwitch.setChecked(false);
            TrackingWelcomeActivity.MustLogInDialogFragment mustLogInDialogFragment = new TrackingWelcomeActivity.MustLogInDialogFragment();
            mustLogInDialogFragment.show(getFragmentManager(), "MustLoginDialog");

        } else {
            this.settings.setTrackingEnabled(this.trackingEnableSwitch.isChecked());

            updateEnablednessOfMilestoneSwitches();
        }
    }

    private void updateEnablednessOfMilestoneSwitches() {
        // If we disabled tracking, then gray out the milestone notification
        this.notifyMilestoneCheckbox.setEnabled(this.trackingEnableSwitch.isChecked());
        this.notifyWeeklyCheckbox.setEnabled(this.trackingEnableSwitch.isChecked());

        // Also disable the milestones if the tracking isn't enabled
        if (!this.trackingEnableSwitch.isChecked()) {
            this.notifyMilestoneCheckbox.setChecked(false);
            this.notifyWeeklyCheckbox.setChecked(false);

            onNotifyWeekly(false);
            onNotifyMilestone(false);
        }
    }

    public void onNotifyMilestone(boolean isChecked) {
        this.settings.setNotifyMilestone(isChecked);
    }

    public void onNotifyWeekly(boolean isChecked) {
        this.settings.setNotifyWeekly(isChecked);
    }

    public void spawnLoginBox() {
        // TODO: This should be enumerated somehow
        trackingEnableSwitch.setChecked(false);
        notifyMilestoneCheckbox.setChecked(false);
        notifyWeeklyCheckbox.setChecked(false);

        TrackingWelcomeActivity.MustLogInDialogFragment mustLogInDialogFragment = new TrackingWelcomeActivity.MustLogInDialogFragment();
        mustLogInDialogFragment.show(getFragmentManager(), "MustLoginDialog");
    }

    public void onShareData(View v) {
        this.settings.setShareData(this.shareDataSwitch.isChecked());
    }

    public void onUsageClick(View v) {
        HttpUtils.openLinkInBrowser(this, Config.TRACKING_USAGE_URL);
    }

    public void onTermsClick(View v) {
        HttpUtils.openLinkInBrowser(this, Config.TRACKING_TERMS_URL);
    }

    private void initStrings() {
        this.trackingEnableText.setText(IbikeApplication.getString("enable_tracking"));
        this.trackingEnableSwitch.setChecked(loggedIn && settings.getTrackingEnabled());
        this.notifyMilestoneText.setText(IbikeApplication.getString("tracking_milestone_notifications"));
        this.notifyMilestoneCheckbox.setChecked(loggedIn && settings.getNotifyMilestone());

        this.notifyWeeklyText.setText(IbikeApplication.getString("tracking_weekly_status_notifications"));
        this.notifyWeeklyCheckbox.setChecked(loggedIn && settings.getNotifyWeekly());

        /*
        this.shareDataText.setText(IbikeApplication.getString("tracking_share_data"));
        this.shareDataSwitch.setChecked(settings.getShareData());
        this.shareDataInfoText.setText(IbikeApplication.getString("tracking_share_data_info"));
        this.shareDataUsageText.setText(IbikeApplication.getString("tracking_share_data_usage"));
        this.shareDataUsageText.setPaintFlags(this.shareDataUsageText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        this.shareDataTermsText.setText(IbikeApplication.getString("tracking_terms_link"));
        this.shareDataTermsText.setPaintFlags(this.shareDataTermsText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        */
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If we got back from a login box AND the used successfully logged in, go on.
        if (requestCode == LeftMenu.LAUNCH_LOGIN && resultCode == RESULT_OK) {
            this.loggedIn = IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin();
        }

        if (requestCode == 10) {
            Log.d("DV", "Vi kom fra Register via annuller!, SettingsAct. kode = 10");
            trackingEnableSwitch.setChecked(false);
            notifyMilestoneCheckbox.setChecked(false);
            notifyWeeklyCheckbox.setChecked(false);
            finish();
        }
    }
}
