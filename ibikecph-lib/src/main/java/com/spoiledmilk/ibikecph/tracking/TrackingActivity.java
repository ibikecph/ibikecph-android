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

import com.facebook.android.Util;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TrackingActivity extends Activity {

    private TextView activityText, sinceText, distanceText;
    TrackingManager trackingManager;
    private String DATE_FORMAT = "dd MMMM yyyy";
    private Realm realm;
    private TextView avgPerTrackDistanceTextView;
    private TextView distanceTextView;
    private TextView speedTextView;
    private TextView timeTextView;
    private TextView trackingStatusTextView;

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

        this.activityText.setText(IbikeApplication.getString("tracking_activity"));
        // TODO: get date of last activity
        Date lastActivity = new Date();
        String formattedDate = new SimpleDateFormat(DATE_FORMAT).format(lastActivity);
        this.sinceText.setText(IbikeApplication.getString("tracking_since") + " " + formattedDate);

        trackingManager = TrackingManager.getInstance();
        realm = Realm.getInstance(this);

        getTracks();
        updateStrings();
    }


    @Override
    public void onResume() {
        super.onResume();
        getTracks();
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
        trackingManager.startTracking();
        this.updateStrings();
    }

    public void btnStopTrackingOnClick(View v) {
        trackingManager.stopTracking();

        // This is a hack. We shouldn't put the locations into a Track before we have a Realm instance.
        // Refactor TrackingManager.
        Track t = trackingManager.getLocationsAsTrack();

        Log.d("JC", "Stopped tracking, got number of points: " + t.getLocations().size());

        realm.beginTransaction();
        Track track = realm.createObject(Track.class);
        track.setLocations(t.getLocations());
        realm.commitTransaction();

        getTracks();
        this.updateStrings();
    }

    public void updateStrings() {
        this.trackingStatusTextView.setText("Tracking: "+trackingManager.isTracking());
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
    public void getTracks() {
        Log.d("JC", "Printing tracks:");
        RealmResults<Track> results  = realm.allObjects(Track.class);
        ArrayList<Location> locations;

        double totalDistance = 0;

        for (Track t : results) {
            double curDist = getDistanceOfTrack(t);
            totalDistance += curDist;
            Log.d("JC", "Track " + t.hashCode() + ", distance: " + curDist + " meters");
        }

        distanceTextView.setText(String.format("%.2f", totalDistance/1000));


    }

}
