package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmResults;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private TextView kmText, kmtText, kmPrTripText, hoursText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

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
        } catch(NullPointerException e) {
            // There was no ActionBar. Oh well...
        }

        trackingManager = TrackingManager.getInstance();

        printTracks();
        updateStrings();
    }


    @Override
    public void onResume() {
        super.onResume();
        printTracks();
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

        printTracks();
        this.updateStrings();
    }

    public void updateStrings() {
        this.trackingStatusTextView.setText("Tracking: "+trackingManager.isTracking());

        this.kmText.setText(IbikeApplication.getString("unit_km").toUpperCase());
        this.kmtText.setText(IbikeApplication.getString("unit_km_pr_h").toUpperCase());
        this.kmPrTripText.setText(IbikeApplication.getString("unit_km_pr_trip").toUpperCase());
        this.hoursText.setText(IbikeApplication.getString("unit_h_long").toUpperCase());

        this.activityText.setText(IbikeApplication.getString("tracking_activity").toUpperCase());
        this.sinceText.setText(IbikeApplication.getString("Since").toUpperCase());

        // Get the timestamp of the first recorded TrackLocation
        Realm realm = Realm.getInstance(this);
        RealmResults<TrackLocation> results  = realm.allObjects(TrackLocation.class);
        Date firstActivity = results.first().getTimestamp();
        String formattedDate = new SimpleDateFormat(DATE_FORMAT).format(firstActivity);
        // Done

        this.activityText.setText(IbikeApplication.getString("stats_description").toUpperCase());
        this.sinceText.setText(IbikeApplication.getString("Since").toUpperCase() + " " + formattedDate.toUpperCase());
    }

    public static double getDistanceOfTrack(Track t)  {
        double result = 0;

        ArrayList<Location> locations = new ArrayList<Location>();

        for (TrackLocation l : t.getLocations()) {
            Location tmpl = new Location("TrackingActivity");
            tmpl.setLongitude(l.getLongitude());
            tmpl.setLatitude(l.getLatitude());

            locations.add(tmpl);
        }

        for (int i = 0; i < locations.size()-1; i++) {
            result += locations.get(i).distanceTo(locations.get(i + 1));
        }

        return result;
    }

    /**
     * Prints all tracks.
     */
    public void printTracks() {
        Log.d("JC", "Printing tracks:");

        Realm realm = Realm.getInstance(this);
        RealmResults<Track> results  = realm.allObjects(Track.class);

        double totalDistance = 0;
        double totalSeconds = 0;
        double speedAggregate = 0;

        for (Track t : results) {
            double curDist = getDistanceOfTrack(t);

            // We get the duration of the trip by subtracting the timestamp of the first GPS coord from the timestamp
            // of the last.
            if (t.getLocations() != null && t.getLocations().size() > 0) {
                int elapsedSeconds = (int) (t.getLocations().last().getTimestamp().getTime() - t.getLocations().first().getTimestamp().getTime()) / 1000;
                totalSeconds += elapsedSeconds;
                double speed = curDist / elapsedSeconds; // Unit: m/s
                speedAggregate += speed;
            }

            totalDistance += curDist;

            Log.d("JC", "Track " + t.hashCode() + ", distance: " + curDist + " meters");
        }

        distanceTextView.setText(String.format("%.1f", totalDistance/1000));
        if (results.size() > 0 ) {
            avgPerTrackDistanceTextView.setText(String.format("%.1f", totalDistance / 1000 / results.size()));

            // The speedAggregate is i meters/sec, we multiply with 3.6 to get km/h
            speedTextView.setText(String.format("%.1f", (speedAggregate / results.size()) * 3.6 ));
        }
        else { // Can't divide by 0
            avgPerTrackDistanceTextView.setText("0.0");
            speedTextView.setText("0.0");
        }

        timeTextView.setText(String.format("%.1f", totalSeconds / 3600));

    }

}
