package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmResults;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TrackingActivity extends Activity {

    private TextView activityText, sinceText, distanceText;
    TrackingManager trackingManager;
    private String DATE_FORMAT = "dd MMMM yyyy";
    private TextView avgPerTrackDistanceTextView;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView timeTextView;
    private TextView trackingStatusTextView;
    private ListView tripListView;

    private TextView kmText, kmtText, kmPrTripText, hoursText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        this.tripListView = (ListView) findViewById(R.id.tripListView);
        this.tripListView.addHeaderView(this.getLayoutInflater().inflate(R.layout.track_list_header, null, false));


        this.activityText = (TextView) findViewById(R.id.tracking_activity_text);
        this.sinceText    = (TextView) findViewById(R.id.tracking_activity_since);

        this.distanceTextView    = (TextView) findViewById(R.id.distanceTextView);
        this.speedTextView    = (TextView) findViewById(R.id.speedTextView);
        this.avgPerTrackDistanceTextView    = (TextView) findViewById(R.id.avgPerTrackDistanceTextView);
        this.timeTextView    = (TextView) findViewById(R.id.timeTextView);
        this.trackingStatusTextView = (TextView) findViewById(R.id.trackingStatusTextView);

        this.kmText = (TextView) findViewById(R.id.kmText);
        this.kmtText = (TextView) findViewById(R.id.kmtText);
        this.kmPrTripText = (TextView) findViewById(R.id.kmPrTripText);
        this.hoursText = (TextView) findViewById(R.id.hoursText);


        try {
            this.getActionBar().setTitle(IbikeApplication.getString("tracking"));
            this.getActionBar().setDisplayHomeAsUpEnabled(false);
        } catch(NullPointerException e) {
            // There was no ActionBar. Oh well...
        }

        trackingManager = TrackingManager.getInstance();

        updateSummaryStatistics();
        updateStrings();
        updateListOfTracks();
        printDebugInfo();
    }

    private void updateListOfTracks() {
        Realm realm = Realm.getInstance(this);
        RealmResults<Track> tracks = realm.allObjects(Track.class);

        // We want to see the newest track first
        tracks.sort("timestamp", false);

        TrackListAdapter trackListAdapter = new TrackListAdapter(this, R.layout.track_list_row_view, tracks);
        this.tripListView.setAdapter(trackListAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        updateSummaryStatistics();
        updateStrings();
        updateListOfTracks();
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
        this.trackingStatusTextView.setText("Tracking: "+trackingManager.isTracking());

        this.kmText.setText(IbikeApplication.getString("unit_km"));
        this.kmtText.setText(IbikeApplication.getString("unit_km_pr_h"));
        this.kmPrTripText.setText(IbikeApplication.getString("unit_km_pr_trip"));
        this.hoursText.setText(IbikeApplication.getString("unit_h_long"));

        this.activityText.setText(IbikeApplication.getString("stats_description"));

        // Get the timestamp of the first recorded TrackLocation
        Realm realm = Realm.getInstance(this);
        RealmResults<TrackLocation> results  = realm.allObjects(TrackLocation.class);

        try {
            Date firstActivity = results.first().getTimestamp();
            String formattedDate = new SimpleDateFormat(DATE_FORMAT).format(firstActivity);
            this.sinceText.setText(IbikeApplication.getString("Since") + " " + formattedDate);
        } catch(ArrayIndexOutOfBoundsException e) {
            this.sinceText.setText("");
        }
        // Done

    }



    /**
     * Updates the overview of the bicycling activities.
     */
    public void updateSummaryStatistics() {
        Realm realm = Realm.getInstance(this);
        RealmResults<Track> results  = realm.allObjects(Track.class);

        double totalDistance = 0;
        double totalSeconds = 0;

        for (Track t : results) {
            totalDistance += t.getLength();
            totalSeconds += t.getDuration();
        }

        distanceTextView.setText(String.format("%.1f", totalDistance/1000));

        if (results.size() > 0 ) {
            avgPerTrackDistanceTextView.setText(String.format("%.1f", totalDistance / 1000 / results.size()));
        }
        else { // Can't divide by 0
            avgPerTrackDistanceTextView.setText("0.0");
        }

        if (totalSeconds > 0 ) {
            // The speedAggregate is in meters/sec, we multiply with 3.6 to get km/h
            speedTextView.setText(String.format("%.1f", (totalDistance / totalSeconds) * 3.6));
        } else {
            speedTextView.setText("0.0");
        }

        timeTextView.setText(String.format("%.1f", totalSeconds / 3600));
    }

    public void printDebugInfo() {
        Log.d("JC", "Current max streak: " + IbikeApplication.getSettings().getMaxStreakLength());
        Log.d("JC", "Current max length ordinal: " + IbikeApplication.getSettings().getLengthNotificationOrdinal());

    }

}
