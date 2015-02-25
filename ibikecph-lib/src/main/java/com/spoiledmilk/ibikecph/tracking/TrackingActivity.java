package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.content.Intent;
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
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.HttpUtils;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import io.realm.Realm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TrackingActivity extends Activity {

    private TextView activityText, sinceText;
    TrackingManager trackingManager;
    private String DATE_FORMAT = "dd MMMM yyyy";
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        this.activityText = (TextView) findViewById(R.id.tracking_activity_text);
        this.sinceText    = (TextView) findViewById(R.id.tracking_activity_since);

        this.activityText.setText(IbikeApplication.getString("tracking_activity"));
        // TODO: get date of last activity
        Date lastActivity = new Date();
        String formattedDate = new SimpleDateFormat(DATE_FORMAT).format(lastActivity);
        this.sinceText.setText(IbikeApplication.getString("tracking_since") + " " + formattedDate);

        trackingManager = TrackingManager.getInstance();
        realm = Realm.getInstance(this);
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
    }
}
