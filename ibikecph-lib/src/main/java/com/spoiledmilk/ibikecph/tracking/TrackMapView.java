package com.spoiledmilk.ibikecph.tracking;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
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

        PathOverlay path = new PathOverlay(Color.RED, 5);
        LatLng center = null;

        // Loop through all of the points, adding them to the path overlay. Create a BoundingBox in the meantime
        double minLat = Double.MAX_VALUE, maxLat = Double.MIN_VALUE, minLong = Double.MAX_VALUE, maxLong = Double.MIN_VALUE;
        for (TrackLocation loc : track.getLocations()) {
            if (center == null) {
                center = new LatLng(loc.getLatitude(), loc.getLongitude());
                minLat = loc.getLatitude();
                maxLat = loc.getLatitude();
                minLong = loc.getLongitude();
                maxLong = loc.getLongitude();
            }

            path.addPoint(loc.getLatitude(), loc.getLongitude());
            Log.d("JC", Double.toString(loc.getHorizontalAccuracy()));

            if (loc.getLatitude() < minLat)
                minLat = loc.getLatitude();

            if (loc.getLatitude() > maxLat)
                maxLat = loc.getLatitude();

            if (loc.getLongitude() < minLong)
                minLong = loc.getLongitude();

            if (loc.getLongitude() > maxLong)
                maxLong = loc.getLongitude();

        }

        mapView.getOverlays().add(path);

        if (center != null) {
            mapView.setCenter(center);
        }

        Log.d("JC", "Bounding box: [" + minLat + ", " + maxLat+"], ["+minLong + ", "+maxLong+"]");

        // TODO: This doesn't work. Why?
        final BoundingBox bbox = new BoundingBox(minLat, maxLong, maxLat, minLong);
        mapView.zoomToBoundingBox(bbox, true, true);

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
