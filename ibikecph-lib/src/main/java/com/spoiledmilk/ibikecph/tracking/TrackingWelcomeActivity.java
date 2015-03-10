package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;

public class TrackingWelcomeActivity extends Activity {

    private TextView introText, enableText, termsText, termsLinkText;
    private Button enableButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_welcome);

        this.introText     = (TextView) findViewById(R.id.tracking_intro);
        this.enableText    = (TextView) findViewById(R.id.tracking_enable_text);
        this.termsText     = (TextView) findViewById(R.id.tracking_terms);
        this.termsLinkText = (TextView) findViewById(R.id.tracking_terms_link);
        this.enableButton  = (Button) findViewById(R.id.tracking_enable);

        initStrings();
    }

    public void onTrackingEnableClick(View v) {
        IbikePreferences settings = IbikeApplication.getSettings();
        settings.setTrackingEnabled(true);
        startActivity(new Intent(this, TrackingActivity.class));
    }

    public void onTermsClick(View v) {
        HttpUtils.openLinkInBrowser(this, Config.TRACKING_TERMS_URL);
    }

    private void initStrings() {
        this.introText.setText(IbikeApplication.getString("tracking_intro"));
        this.enableText.setText(IbikeApplication.getString("tracking_enable"));
        this.termsText.setText(IbikeApplication.getString("tracking_terms"));
        this.termsLinkText.setText(IbikeApplication.getString("tracking_terms_link"));
        this.termsLinkText.setPaintFlags(this.termsLinkText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        this.enableButton.setText(IbikeApplication.getString("tracking_enable"));
    }
}
