package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
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

        this.introText     = (TextView) findViewById(R.id.tracking_intro);
        this.enableText    = (TextView) findViewById(R.id.tracking_enable_text);
        this.termsText     = (TextView) findViewById(R.id.tracking_terms);
        this.termsLinkText = (TextView) findViewById(R.id.tracking_terms_link);
        this.enableButton  = (Button) findViewById(R.id.tracking_enable);

        this.kmText        = (TextView) findViewById(R.id.kmText);
        this.kmtText       = (TextView) findViewById(R.id.kmtText);
        this.kmPrTripText  = (TextView) findViewById(R.id.kmPrTripText);
        this.hoursText     = (TextView) findViewById(R.id.hoursText);

        try {
            this.getActionBar().setTitle(IbikeApplication.getString("tracking"));
        } catch(NullPointerException e) {
            // There was no ActionBar. Oh well...
        }

        this.enableButton.setEnabled(IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin());

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
}
