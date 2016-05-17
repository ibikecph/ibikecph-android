package com.spoiledmilk.ibikecph.tracking;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.PathOverlay;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.BaseMapActivity;
import com.spoiledmilk.ibikecph.map.IBCMapView;
import com.spoiledmilk.ibikecph.map.handlers.TrackDisplayHandler;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import java.util.ArrayList;


public class TrackMapView extends BaseMapActivity {
    IBCMapView mapView;
    BoundingBox bbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_map_view);
        Log.d("DV", "Viewing track!");

        int track_position = this.getIntent().getIntExtra("track_position", -1);

        mapView = (IBCMapView) findViewById(R.id.mapview);
        mapView.init(this);
        mapView.setMapViewListener(new TrackDisplayHandler(mapView));
        //mapView.setMaxZoomLevel(19f);

        // Get the route
        Realm realm = Realm.getInstance(this);
        RealmResults<Track> tracks = realm.allObjects(Track.class);

        // We want to see the newest track first
        tracks.sort("timestamp", Sort.DESCENDING);

        Track track = tracks.get(track_position);
        PathOverlay path = new PathOverlay(Color.RED, 10);

        /**
         * Convert the list of points to a list of LatLng objects. This is used for drawing the path and creating the
         * bounding box.
         */

        ArrayList<LatLng> route = new ArrayList<LatLng>();
        for (TrackLocation loc : track.getLocations()) {
            LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
            route.add(ll);
        }


        /*ArrayList<LatLng> route = new ArrayList<LatLng>();
        LatLng a = new LatLng(55.6774085,12.5696799);
        route.add(a);
        LatLng b = new LatLng(55.6574319,12.6121303);
        route.add(b);
        LatLng c = new LatLng(55.7079739,12.5692061);
        route.add(c);
        LatLng d = new LatLng(55.7180839,12.5692061);
        route.add(d);
        LatLng f = new LatLng(55.7281939,12.5692061);
        route.add(f);*/

        //float xx = a.distanceTo(b);
        //Log.d("DV", "XX DISTANCE = " + xx);


        // Add the points to a route
        path.addPoints(route);
        mapView.getOverlays().add(path);

        final BoundingBox bbox = BoundingBox.fromLatLngs(route);
        this.bbox = bbox;

        ////

        //Add 20% padding
        double north = this.bbox.getLatNorth();
        double east = this.bbox.getLonEast();
        double west = this.bbox.getLonWest();
        double south = this.bbox.getLatSouth();

        double latitudeDiff = Math.abs(north - south) * 0.2;

        double longitudeDiff = Math.abs(east - west) * 0.2;



        ArrayList<LatLng> paddedWaypoints = new ArrayList<LatLng>();
        LatLng ne = new LatLng(north + latitudeDiff, east + longitudeDiff);
        LatLng sw = new LatLng(south - latitudeDiff, west - longitudeDiff);
        paddedWaypoints.add(ne);
        paddedWaypoints.add(sw);

        ////

        /**
         * Center and zoom the map according to the bounding box of the route.
         */
        mapView.zoomToBoundingBox(BoundingBox.fromLatLngs(paddedWaypoints), true, true, false, true);//(bbox, true, false, true, false);

        // If the route is really short, it doesn't look good to fit that into the window. Zoom out a bit.
        if (mapView.getZoomLevel() > 18.0f) {
            mapView.setZoom(18.0f);
        }

        // Set the ActionBar
        try {
            this.getActionBar().setTitle(IbikeApplication.getString("tracking"));
        } catch (NullPointerException e) {
            // There was no ActionBar. Oh well...
        }
    }

    public void btnZoomCenterClick(View v) {
        Log.d("JC", "Bounding box center: " + this.bbox.getCenter().toString());
        Log.d("JC", "Mapbox bounding box:" + this.mapView.getBoundingBox().toString());
        mapView.zoomToBoundingBox(this.bbox);

    }

    public void onResume(Bundle savedInstanceState) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_track_map_view, menu);
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
