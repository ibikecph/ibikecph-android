// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.analytics.tracking.android.EasyTracker;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.tileprovider.tilesource.WebSourceTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.iLanguageListener;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.search.SearchActivity;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import net.hockeyapp.android.CrashManager;

/**
 * The main map view.
 * 
 * TODO: Look into ways of making this class shorter.
 * @author jens
 *
 */
@SuppressLint("NewApi")
public class MapActivity extends Activity implements SMHttpRequestListener, iLanguageListener {
    public static int RESULT_RETURN_FROM_NAVIGATION = 105;

    protected LeftMenu leftMenu;
    private DrawerLayout drawerLayout;
    private MaterialMenuIcon materialMenu;
    private MapView mapView;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main_map_activity);

        this.mapView = (MapView) findViewById(R.id.mapView);

        // We want the hamburger in the ActionBar
        materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);

        // LeftMenu
        initLeftMenu(savedInstanceState);

        initMapView();

    }

    /**
     * Initializes the map, sets the tile source and centers around Copenhagen.
     */
    private void initMapView() {
        WebSourceTileLayer ws = new WebSourceTileLayer("openstreetmap", "http://tiles.ibikecph.dk/tiles/{z}/{x}/{y}.png");
        ws.setName("OpenStreetMap")
                .setAttribution("© OpenStreetMap Contributors")
                .setMinimumZoomLevel(1)
                .setMaximumZoomLevel(17);

        this.mapView.setTileSource(ws);
        this.mapView.setCenter(new LatLng(Util.COPENHAGEN));
        this.mapView.setZoom(15);
    }

    /**
     * Initializes the LeftMenu
     * @param savedInstanceState
     */
    private void initLeftMenu(final Bundle savedInstanceState) {
        leftMenu = getLeftMenu();

        // Add the menu to the Navigation Drawer
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (savedInstanceState == null) {
            fragmentTransaction.add(R.id.leftContainerDrawer, leftMenu);
        } else {
            fragmentTransaction.replace(R.id.leftContainerDrawer, leftMenu);
        }
        fragmentTransaction.commit();
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.ab_search) {
            Intent i = new Intent(MapActivity.this, getSearchActivity());
            startActivityForResult(i, 2);
            overridePendingTransition(R.anim.slide_in_down, R.anim.fixed);
        }
        // Toggle the drawer when tapping the app icon.
        else if (id == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(Gravity.START)) {
                drawerLayout.closeDrawer(Gravity.START);
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            } else {
                drawerLayout.openDrawer(Gravity.START);
                materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);

            }
        }

        return super.onOptionsItemSelected(item);
    }

    protected Class<?> getSearchActivity() {
        return SearchActivity.class;
    }

    protected LeftMenu getLeftMenu() {
        if (leftMenu == null) {
            return leftMenu = new LeftMenu();
        } else {
            return leftMenu;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        LOG.d("Map activity onResume");

        if (!Util.isNetworkConnected(this)) {
            Util.launchNoConnectionDialog(this);
        }
        checkForCrashes();
        getLeftMenu().updateControls();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        leftMenu = getLeftMenu();
        leftMenu.updateControls();
    }

    protected void checkForCrashes() {
        CrashManager.register(this, Config.HOCKEY_APP_ID);
    }


    @Override
    public void onResponseReceived(int requestType, Object response) {
        switch (requestType) {
            case SMHttpRequest.REQUEST_GET_ROUTE:
                RouteInfo ri = (RouteInfo) response;
                JsonNode jsonRoot = null;
                if (ri == null || (jsonRoot = ri.jsonRoot) == null || jsonRoot.path("status").asInt(-1) != 0 || ri.start == null || ri.end == null) {
                    //showRouteNotFoundDlg();
                } else {
                    startRouting(ri.start, ri.end, ri.jsonRoot, "", "");
                }
                break;
            case SMHttpRequest.REQUEST_FIND_NEAREST_LOC:
                if (response != null) {
                    final Location loc = (Location) response;

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                         //mapFragment.setPinLocation(loc);

                        }
                    }, 200);
                }
                break;
            case SMHttpRequest.REQUEST_FIND_PLACES_FOR_LOC:
                if (response != null) {
                    SMHttpRequest.Address address = (SMHttpRequest.Address) response;

                }
                break;

        }

        if (leftMenu != null) {
            leftMenu.favoritesEnabled = true;
        }
    }

    public void startRouting(Location start, Location end, JsonNode jsonRoot, String startName, String endName) {
        Intent i = new Intent(this, MapActivity.class); // FIXME: This needs to be the navigation class.
        i.putExtra("start_lat", start.getLatitude());
        i.putExtra("start_lng", start.getLongitude());
        i.putExtra("end_lat", end.getLatitude());
        i.putExtra("end_lng", end.getLongitude());
        if (jsonRoot != null)
            i.putExtra("json_root", jsonRoot.toString());

        // TODO: These are strings
        //i.putExtra("source", source);
        //i.putExtra("destination", destination);

        if (jsonRoot != null && jsonRoot.has("route_summary")) {
            i.putExtra("start_name", jsonRoot.get("route_summary").get("start_point").asText());
            i.putExtra("end_name", jsonRoot.get("route_summary").get("end_point").asText());
        } else {
            i.putExtra("start_name", startName);
            i.putExtra("end_name", endName);
        }
        i.putExtra("overlays", getOverlaysShown());

        /*
        new DB(MapActivity.this).saveSearchHistory(new HistoryData(infoLine1, end.getLatitude(), end.getLongitude()), new HistoryData(
                IbikeApplication.getString("current_position"), start.getLatitude(), start.getLongitude()), MapActivity.this);
        */
        this.startActivityForResult(i, 1);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        LOG.d("route found");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        

        // *** DANGER, WILL ROBINSON: I'm looking at the request code, not the return code from this point on. /jc ***
        // Take care of starting navigation once a favorite has been clicked in the FavoritesListActivity
        if (requestCode == LeftMenu.LAUNCH_FAVORITE && resultCode == RESULT_OK){
        	FavoritesData fd = (FavoritesData) data.getExtras().getParcelable("ROUTE_TO");

        	Location start = SMLocationManager.getInstance().getLastValidLocation();
			IbikeApplication.getTracker().sendEvent("Route", "Menu", "Favorites", (long) 0);
			new SMHttpRequest().getRoute(start, Util.locationFromCoordinates(fd.getLatitude(), fd.getLongitude()), null, this);
			
			Log.i("JC", "Fav coordinates: "+ fd.getLatitude() + ", " + fd.getLongitude());
			
        	Log.i("JC", "Launching favorite " + fd.getName());
        	
        } else if (requestCode == LeftMenu.LAUNCH_LOGIN && resultCode == RESULT_OK) {
        	leftMenu.populateMenu();
        	
        } else if (resultCode == RESULT_CANCELED) {
        	Log.i("JC", "Canceled sub activity");
        } else {
        	Log.e("JC", "MapActivity: Didn't have an activity handler for " + requestCode);
        }
    }

    public void reloadStrings() {
        leftMenu.initStrings();
        leftMenu.reloadStrings();
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    public void refreshOverlays(int overlaysShown) {

    }

    public int getOverlaysShown() {
        return 0;
    }

    AlertDialog loginDlg;

    private void launchLoginDialog() {
        if (loginDlg == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(IbikeApplication.getString("login"));
            builder.setMessage(IbikeApplication.getString("error_not_logged_in"));
            builder.setPositiveButton(IbikeApplication.getString("login"), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(MapActivity.this, LoginActivity.class);
                    startActivity(i);
                }
            });
            builder.setNegativeButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            loginDlg = builder.create();
        }
        loginDlg.show();
    }

    @Override
    public void onPause() {
    	/*
    	if (loginDlg != null && loginDlg.isShowing()) {
            loginDlg.dismiss();
        }
        */
        super.onPause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        materialMenu.syncState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        materialMenu.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_map_activity, menu);
        return true;
    }
}
