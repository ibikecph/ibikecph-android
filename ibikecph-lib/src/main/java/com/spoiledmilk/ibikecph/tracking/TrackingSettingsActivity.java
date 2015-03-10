package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

public class TrackingSettingsActivity extends Activity {

    private TextView trackingEnableText;
    private Switch trackingEnableSwitch;

    private TextView notifyMilestoneText;
    private CheckBox notifyMilestoneCheckbox;
    private TextView notifyWeeklyText;
    private CheckBox notifyWeeklyCheckbox;

    private TextView shareDataText;
    private Switch   shareDataSwitch;
    private TextView shareDataInfoText;
    private TextView shareDataUsageText;
    private TextView shareDataTermsText;

    private IbikePreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_settings);

        this.settings = IbikeApplication.getSettings();

        this.trackingEnableText      = (TextView) findViewById(R.id.tracking_enable_text);
        this.trackingEnableSwitch    = (Switch)   findViewById(R.id.tracking_enable_switch);
        this.notifyMilestoneText     = (TextView) findViewById(R.id.notify_milestone_text);
        this.notifyMilestoneCheckbox = (CheckBox) findViewById(R.id.notify_milestone_checkbox);
        this.notifyWeeklyText        = (TextView) findViewById(R.id.notify_weekly_text);
        this.notifyWeeklyCheckbox    = (CheckBox) findViewById(R.id.notify_weekly_checkbox);
        this.shareDataText           = (TextView) findViewById(R.id.share_data_text);
        this.shareDataSwitch         = (Switch)   findViewById(R.id.share_data_switch);
        this.shareDataInfoText       = (TextView) findViewById(R.id.share_data_info_text);
        this.shareDataUsageText      = (TextView) findViewById(R.id.share_data_usage_text);
        this.shareDataTermsText      = (TextView) findViewById(R.id.share_data_terms_text);

        initStrings();
    }

    public void onEnableTrackingClick(View v) {
        this.settings.setTrackingEnabled(this.trackingEnableSwitch.isChecked());
    }

    public void onNotifyMilestone(View v) {
        this.settings.setNotifyMilestone(this.notifyMilestoneCheckbox.isChecked());
    }

    public void onNotifyWeekly(View v) {
        this.settings.setNotifyWeekly(this.notifyWeeklyCheckbox.isChecked());
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
        this.trackingEnableText.setText(IbikeApplication.getString("tracking_enable"));
        this.trackingEnableSwitch.setChecked(settings.getTrackingEnabled());
        this.notifyMilestoneText.setText(IbikeApplication.getString("tracking_notify_milestone"));
        this.notifyMilestoneCheckbox.setChecked(settings.getNotifyMilestone());
        this.notifyWeeklyText.setText(IbikeApplication.getString("tracking_notify_weekly"));
        this.notifyWeeklyCheckbox.setChecked(settings.getNotifyWeekly());
        this.shareDataText.setText(IbikeApplication.getString("tracking_share_data"));
        this.shareDataSwitch.setChecked(settings.getShareData());
        this.shareDataInfoText.setText(IbikeApplication.getString("tracking_share_data_info"));
        this.shareDataUsageText.setText(IbikeApplication.getString("tracking_share_data_usage"));
        this.shareDataUsageText.setPaintFlags(this.shareDataUsageText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        this.shareDataTermsText.setText(IbikeApplication.getString("tracking_terms_link"));
        this.shareDataTermsText.setPaintFlags(this.shareDataTermsText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }
}
