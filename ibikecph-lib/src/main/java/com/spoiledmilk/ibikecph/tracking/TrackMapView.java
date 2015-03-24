package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmResults;


public class TrackMapView extends Activity {
    MapView mapView ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_map_view);

        int track_position = this.getIntent().getIntExtra("track_position", -1);

        mapView = (MapView) findViewById(R.id.mapview);


        // Get the route
        Realm realm = Realm.getInstance(this);
        RealmResults<Track> tracks = realm.allObjects(Track.class);
        Track track = tracks.get(track_position);

        PathOverlay path = new PathOverlay(Color.RED, 3);
        LatLng center = null;

        for (TrackLocation loc : track.getLocations()) {
            if (center == null) {
                center = new LatLng(loc.getLatitude(), loc.getLongitude());
            }

            path.addPoint(loc.getLatitude(), loc.getLongitude());
        }

        mapView.getOverlays().add(path);

        if (center != null) {
            mapView.setCenter(center);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_track_map_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
