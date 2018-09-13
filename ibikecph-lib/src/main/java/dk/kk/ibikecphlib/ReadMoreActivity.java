package dk.kk.ibikecphlib;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import dk.kk.ibikecphlib.R;

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

        this.findViewById(R.id.rootLayout).setBackgroundColor(getResources().getColor(IBikeApplication.getPrimaryColor()));

        readMoreText = (TextView) findViewById(R.id.readMoreText);
        readMoreText.setText(IBikeApplication.getString("launch_activate_tracking_read_more_description"));

        this.getActionBar().setTitle(IBikeApplication.getString("launch_activate_tracking_read_more"));
    }

    @Override
    public void onResume() {
        super.onResume();
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
