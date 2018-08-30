package dk.kk.ibikecphlib.tracking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.login.SignatureActivity;
import dk.kk.ibikecphlib.persist.Track;
import dk.kk.ibikecphlib.persist.TrackLocation;
import dk.kk.ibikecphlib.util.IBikePreferences;
import dk.kk.ibikecphlib.util.Util;

import dk.kk.ibikecphlib.login.SignatureActivity;
import dk.kk.ibikecphlib.persist.Track;
import dk.kk.ibikecphlib.persist.TrackLocation;
import dk.kk.ibikecphlib.util.Util;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.RealmQuery;
import io.realm.Sort;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TrackingActivity extends Activity {

    private TextView activityText, sinceText, distanceText;
    TrackingManager trackingManager;
    private String DATE_FORMAT = "dd MMMM yyyy";
    private TextView calText;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView timeTextView;
    private TextView trackingStatusTextView;
    private StickyListHeadersListView tripListView;
    TrackListAdapter trackListAdapter;

    private TextView kmText, kmtText, calUnitText, hoursText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        this.tripListView = (StickyListHeadersListView) findViewById(R.id.tripListView);
        this.tripListView.addHeaderView(this.getLayoutInflater().inflate(R.layout.track_list_header, null, false));

        this.activityText = (TextView) findViewById(R.id.tracking_activity_text);
        this.sinceText = (TextView) findViewById(R.id.tracking_activity_since);

        this.distanceTextView = (TextView) findViewById(R.id.distanceTextView);
        this.speedTextView = (TextView) findViewById(R.id.speedTextView);
        this.calText = (TextView) findViewById(R.id.calText);
        this.timeTextView = (TextView) findViewById(R.id.timeTextView);
        this.trackingStatusTextView = (TextView) findViewById(R.id.trackingStatusTextView);

        this.kmText = (TextView) findViewById(R.id.kmText);
        this.kmtText = (TextView) findViewById(R.id.kmtText);
        this.calUnitText = (TextView) findViewById(R.id.calUnitText);
        this.hoursText = (TextView) findViewById(R.id.hoursText);

        ((Button) findViewById(R.id.reactivateButton)).setText(IBikeApplication.getString("reenable_tracking"));

        try {
            this.getActionBar().setTitle(IBikeApplication.getString("tracking"));
            this.getActionBar().setDisplayHomeAsUpEnabled(false);
        } catch (NullPointerException e) {
            // There was no ActionBar. Oh well...
        }

        trackingManager = TrackingManager.getInstance();

        updateSummaryStatistics();
        updateStrings();
        updateListOfTracks();
        updateReactivateButtonVisibility();
    }

    private void updateListOfTracks() {
        trackListAdapter = new TrackListAdapter(this);
        this.tripListView.setAdapter(trackListAdapter);;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSummaryStatistics();
        updateStrings();
        updateListOfTracks();
        if(!IBikeApplication.getSettings().getTrackingEnabled() && trackListAdapter.getCount() == 0){
            finish();
        }
        updateReactivateButtonVisibility();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracking, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.tracking_settings) {
            startActivity(new Intent(this, TrackingSettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    public void btnStartTrackingOnClick(View v) {
        trackingManager.startTracking(true);
        this.updateStrings();
    }

    public void btnStopTrackingOnClick(View v) {
        trackingManager.stopTracking(true);

        Log.d("JC", "Stopped tracking");

        updateSummaryStatistics();
        this.updateStrings();
    }

    public void updateStrings() {
        this.trackingStatusTextView.setText("Tracking: " + trackingManager.isTracking());

        this.kmText.setText(IBikeApplication.getString("unit_km"));
        this.kmtText.setText(IBikeApplication.getString("unit_km_pr_h"));
        this.calUnitText.setText(IBikeApplication.getString("unit_kcal_pr_day"));

        this.hoursText.setText(IBikeApplication.getString("unit_h_long"));

        this.activityText.setText(IBikeApplication.getString("stats_description"));

        // Get the timestamp of the first recorded TrackLocation
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<TrackLocation> query = realm.where(TrackLocation.class);
        RealmResults<TrackLocation> results = query.findAll();
        try {
            Date firstActivity = results.first().getTimestamp();
            String formattedDate = new SimpleDateFormat(DATE_FORMAT).format(firstActivity);
            this.sinceText.setText(IBikeApplication.getString("Since") + " " + formattedDate);
        } catch (ArrayIndexOutOfBoundsException e) {
            this.sinceText.setText("");
        }
        // Done

    }

    public int getNumberOfDaysSinceFirstCycled() {
        int totalDays;
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Track> query = realm.where(Track.class);
        RealmResults<Track> results = query.findAll();

        try {
            results.sort("timestamp", Sort.ASCENDING);

            Calendar firstTrip = Calendar.getInstance();
            firstTrip.setTime(results.first().getTimestamp());

            Calendar now = Calendar.getInstance();

            totalDays = (int) (now.getTimeInMillis() - firstTrip.getTimeInMillis()) / (1000 * 60 * 60 * 24);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // We have no tracks.
            return 1;
        }

         // It's counter-intuitive, but at a certain day, you want to count both today AND yesterday, even though it's
            // technically only one day ago.
        return totalDays + 1;
    }

    /**
     * Updates the overview of the bicycling activities.
     */
    public void updateSummaryStatistics() {
        Realm realm = Realm.getDefaultInstance();
        double totalDistance = 0;
        double totalSeconds = 0;
        int totalDays = getNumberOfDaysSinceFirstCycled();


        RealmQuery<Track> query = realm.where(Track.class);
        RealmResults<Track> results = query.findAll();


        Log.d("JC", "It is " + getNumberOfDaysSinceFirstCycled() + " days since first cycled.");

        for (Track t : results) {
            totalDistance += t.getLength();
            totalSeconds += t.getDuration();
        }


        distanceTextView.setText(String.format("%d", Math.round(totalDistance / 1000)));

        calText.setText(String.format("%d", (int) ((totalDistance / 1000 / totalDays) * 11)));

        if (totalSeconds > 0) {
            // The speedAggregate is in meters/sec, we multiply with 3.6 to get km/h
            speedTextView.setText(String.format("%.1f", (totalDistance / totalSeconds) * 3.6));
        } else {
            speedTextView.setText("0");
        }

        int totalHours = (int) Math.round(totalSeconds / 3600);
        timeTextView.setText(String.format("%d", totalHours));

        if (totalHours == 1) {
            this.hoursText.setText(IBikeApplication.getString("unit_h_long_singular"));
        }
    }

    public void printDebugInfo() {
        Log.d("JC", "Current max streak: " + IBikeApplication.getSettings().getMaxStreakLength());
        Log.d("JC", "Current max length ordinal: " + IBikeApplication.getSettings().getLengthNotificationOrdinal());
    }

    public void onReactivateButtonClick(View v) {
        if (!Util.isNetworkConnected(TrackingActivity.this)) {
            Util.launchNoConnectionDialog(TrackingActivity.this);
            return;
        }
        // IF the user is not logged in, spawn a dialog saying so.
        if (!IBikeApplication.isUserLogedIn() && !IBikeApplication.isFacebookLogin()) {
            TrackingWelcomeActivity.MustLogInDialogFragment loginDialog = new TrackingWelcomeActivity.MustLogInDialogFragment();
            loginDialog.show(getFragmentManager(), "MustLoginDialog");

        } else {
            if (IBikeApplication.getSignature().equals("")) {
                if (IBikeApplication.isFacebookLogin()) {
                    Log.d("DV", "Prompting Facebookuser to create a password!");
                    Intent i = new Intent(TrackingActivity.this, SignatureActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    Log.d("DV", "Starting activity with resultcode = 99");
                    startActivityForResult(i, 99);
                } else if (IBikeApplication.isUserLogedIn()) {
                    Log.d("DV", "Prompting login for user!");
                    Intent i = new Intent(TrackingActivity.this, SignatureActivity.class).putExtra("normalUser", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivityForResult(i, 10);
                }
            } else {
                Log.d("DV", "We got a signature, enabling tracking!");
                Log.d("DV", "Tracking signature = " + IBikeApplication.getSignature());
                // Remove the button and spacer
                findViewById(R.id.reactivateButton).setVisibility(View.GONE);
                findViewById(R.id.reactivateButtonSpacer).setVisibility(View.GONE);
                IBikePreferences settings = IBikeApplication.getSettings();
                settings.setTrackingEnabled(true);
                settings.setNotifyMilestone(true);
                settings.setNotifyWeekly(true);
                //startActivity(new Intent(this, TrackingActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        }

    }


    void updateReactivateButtonVisibility() {

        if (!IBikeApplication.getSettings().getTrackingEnabled()) {
            findViewById(R.id.reactivateButton).setVisibility(View.VISIBLE);
            findViewById(R.id.reactivateButtonSpacer).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.reactivateButton).setVisibility(View.GONE);
            findViewById(R.id.reactivateButtonSpacer).setVisibility(View.GONE);
        }

    }
}
