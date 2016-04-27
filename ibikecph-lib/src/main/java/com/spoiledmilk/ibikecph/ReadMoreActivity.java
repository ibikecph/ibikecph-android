package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * The about view of the application.
 *
 * @author Daniel
 */
public class ReadMoreActivity extends Activity {

    TextView readMoreText;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_read_more);

        this.findViewById(R.id.rootLayout).setBackgroundColor(getResources().getColor(IbikeApplication.getPrimaryColor()));

        readMoreText = (TextView) findViewById(R.id.readMoreText);
        readMoreText.setText(IbikeApplication.getString("launch_activate_tracking_read_more_description"));

        this.getActionBar().setTitle(IbikeApplication.getString("launch_activate_tracking_read_more"));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tell Google Analytics that the user has resumed on this screen.
        IbikeApplication.sendGoogleAnalyticsActivityEvent(this);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
