// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.*;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.*;
import com.spoiledmilk.ibikecph.favorites.AddFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.EditFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.FavoritesActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.map.SMHttpRequest.RouteInfo;
import com.spoiledmilk.ibikecph.navigation.SMRouteNavigationActivity;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.search.HistoryData;
import com.spoiledmilk.ibikecph.search.SearchActivity;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

/**
 * The main map view.
 * 
 * TODO: Look into ways of making this class shorter.
 * @author jens
 *
 */
@SuppressLint("NewApi")
public class MapActivity extends FragmentActivity implements SMHttpRequestListener, iLanguageListener {

    protected static final int SLIDE_THRESHOLD = 40;
    public static int RESULT_RETURN_FROM_NAVIGATION = 105;
    TranslateAnimation animation;
    float posX = 0;
    float touchX = 0;
    int maxSlide = 0;
    protected boolean slidden = false;
    int moveCount = 0;
    String infoLine1 = "";
    protected SMMapFragment mapFragment;
    protected LeftMenu leftMenu;
    public RelativeLayout pinInfoLayout;
    TextView pinInfoLine1;
    ProgressBar progressBar;
    Button btnStart;
    ImageButton btnTrack;
    RelativeLayout rootLayout;
    protected View mapDisabledView;
    FrameLayout mapContainer;
    ImageButton btnSearch;
    ImageButton btnSaveFavorite;
    SMHttpRequest.Address address;
    Location currentLocation;
    AlertDialog.Builder builder;
    AlertDialog dialog;
    String source, destination;
    boolean isSaveFaveoriteEnabled = true;
    FavoritesData favoritesData = null;
    boolean addFavEnabled = true;
    private DrawerLayout drawerLayout;
    private MaterialMenuIcon materialMenu;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.init(getWindowManager());
        LOG.d("Map activity onCreate");
        this.maxSlide = (int) (4 * Util.getScreenWidth() / 5);
        this.setContentView(R.layout.main_map_activity);
        
        mapFragment = new SMMapFragment();
        FragmentManager fm = this.getFragmentManager();
        fm.beginTransaction().add(R.id.map_container, mapFragment).commit();
        mapContainer = (FrameLayout) findViewById(R.id.map_container);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        pinInfoLayout = (RelativeLayout) findViewById(R.id.pinInfoLayout);
        pinInfoLine1 = (TextView) pinInfoLayout.findViewById(R.id.pinAddressLine1);
        pinInfoLine1.setTypeface(IbikeApplication.getBoldFont());
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pinInfoLayout.setClickable(false);
                LOG.d("find route");
                Location start = SMLocationManager.getInstance().getLastValidLocation();
                if (start == null) {
                    showRouteNotFoundDlg();
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    new SMHttpRequest().getRoute(start, mapFragment.getPinLocation(), null, MapActivity.this);
                }
            }
        });
        btnSaveFavorite = (ImageButton) findViewById(R.id.btnSaveFavorite);
        btnSaveFavorite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                DB db = new DB(MapActivity.this);
                try {
                    if (IbikeApplication.isUserLogedIn()) {
                        if (isSaveFaveoriteEnabled) {
                            favoritesData = new FavoritesData(address.street + " " + address.houseNumber, address.street + " " + address.houseNumber,
                                    "favorite", currentLocation.getLatitude(), currentLocation.getLongitude(), -1);
                            db.saveFavorite(favoritesData, MapActivity.this, true);
                            String st = favoritesData.getName() + " - (" + favoritesData.getLatitude() + "," + favoritesData.getLongitude() + ")";
                            IbikeApplication.getTracker().sendEvent("Favorites", "Save", st, (long) 0);
                            leftMenu.reloadFavorites();
                            btnSaveFavorite.setImageResource(R.drawable.drop_pin_add_fav_btn_active);
                        } else if (favoritesData != null) {
                            db.deleteFavorite(favoritesData, MapActivity.this);
                            leftMenu.reloadFavorites();
                            btnSaveFavorite.setImageResource(R.drawable.drop_pin_selector);
                        }

                        isSaveFaveoriteEnabled = !isSaveFaveoriteEnabled;
                    } else {
                        launchLoginDialog();
                    }
                } catch (Exception e) {

                }
            }

        });
        btnTrack = (ImageButton) findViewById(R.id.btnTrack);
        btnTrack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mapFragment.getTrackingMode()) {
                    startTrackingUser();
                }
            }
        });
        
        rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams((int) (9 * Util.getScreenWidth() / 5),
                FrameLayout.LayoutParams.MATCH_PARENT);
        rootLayout.setLayoutParams(rootParams);

        btnSearch = (ImageButton) findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(MapActivity.this, getSearchActivity());
                startActivityForResult(i, 2);
                overridePendingTransition(R.anim.slide_in_down, R.anim.fixed);
            }

        });
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth(), RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        findViewById(R.id.parent_container).setLayoutParams(params);
        params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth(), RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        findViewById(R.id.pinInfoLayout).setLayoutParams(params);
        leftMenu = getLeftMenu();
        
        // Add the menu to the Navigation Drawer
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if (savedInstanceState == null) {
            fragmentTransaction.add(R.id.leftContainerDrawer, leftMenu);
        } else {
            fragmentTransaction.replace(R.id.leftContainerDrawer, leftMenu);
        }
        fragmentTransaction.commit();
        findViewById(R.id.rootLayout).invalidate();


        // Check for crashes with Hockey
        if (Config.HOCKEY_UPDATES_ENABLED) {
            UpdateManager.register(this, Config.HOCKEY_APP_ID);
        }

        // Make sure the app icon is clickable
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);

        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            }

            @Override
            public void onDrawerStateChanged(int newState) {}

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }
        });

    }
    public boolean onOptionsItemSelected(MenuItem item) {

        // Toggle the drawer when tapping the app icon.
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(Gravity.START)) {
                drawerLayout.closeDrawer(Gravity.START);
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            } else {
                drawerLayout.openDrawer(Gravity.START);
                materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);

            }
        }

        return true;
    }

    protected Class<?> getSearchActivity() {
        return SearchActivity.class;
    }

    public void showWelcomeScreen() {
        if (!IbikeApplication.isWelcomeScreenSeen()) {
            IbikeApplication.setWelcomeScreenSeen(true);
            Intent i = new Intent(this, FavoritesActivity.class);
            startActivity(i);
        }
    }

    @Override
    public void onLowMemory() {
        try {
            if (mapFragment != null && mapFragment.mapView != null && mapFragment.mapView.getTileProvider() != null)
                mapFragment.mapView.getTileProvider().clearTileCache();
        } catch (Exception e) {

        }
    }

    float newTouchX, delta;

    protected Class<?> getSplashActivityClass() {
        return SplashActivity.class;
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
        btnStart.setText(IbikeApplication.getString("start_route"));
        btnStart.setTypeface(IbikeApplication.getBoldFont());
        pinInfoLayout.setClickable(true);
        pinInfoLayout.measure(0, 0);
        mapFragment.infoLayoutHeight = pinInfoLayout.getMeasuredHeight();
        if (!IbikeApplication.isUserLogedIn()) {
            btnSaveFavorite.setImageResource(R.drawable.drop_pin_add_fav_btn_active);
            btnSaveFavorite.setImageAlpha(100);

        } else {
            if (!isSaveFaveoriteEnabled) {
                btnSaveFavorite.setImageResource(R.drawable.drop_pin_add_fav_btn_active);
            } else {
                btnSaveFavorite.setImageResource(R.drawable.drop_pin_selector);
            }
            btnSaveFavorite.setImageAlpha(255);
        }
        
        if (!Util.isNetworkConnected(this)) {
            Util.launchNoConnectionDialog(this);
        }
        checkForCrashes();

        leftMenu.updateControls();
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

    public void startTrackingUser() {
        mapFragment.setTrackingMode(true);
        btnTrack.setImageResource(R.drawable.icon_locate_me);
    }

    public void stopTrackingUser() {
        mapFragment.setTrackingMode(false);
        btnTrack.setImageResource(R.drawable.icon_locate_no_tracking);
    }

    public void showRouteNotFoundDlg() {
        if (!MapActivity.this.isFinishing()) {
            if (dialog == null) {
                String message = IbikeApplication.getString("error_route_not_found");
                if (!SMLocationManager.getInstance().hasValidLocation())
                    message = IbikeApplication.getString("error_no_gps_location");
                builder = new AlertDialog.Builder(this);
                builder.setMessage(message).setPositiveButton(IbikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        pinInfoLayout.setClickable(true);
                    }
                });
                dialog = builder.create();
                dialog.setCancelable(false);
            }
            if (!dialog.isShowing()) {
                dialog.show();
            }
        }
    }

    @Override
    public void onResponseReceived(int requestType, Object response) {
        switch (requestType) {
            case SMHttpRequest.REQUEST_GET_ROUTE:
                RouteInfo ri = (RouteInfo) response;
                JsonNode jsonRoot = null;
                if (ri == null || (jsonRoot = ri.jsonRoot) == null || jsonRoot.path("status").asInt(-1) != 0 || ri.start == null || ri.end == null) {
                    showRouteNotFoundDlg();
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
                            mapFragment.setPinLocation(loc);

                        }
                    }, 200);
                }
                break;
            case SMHttpRequest.REQUEST_FIND_PLACES_FOR_LOC:
                if (response != null) {
                    address = (SMHttpRequest.Address) response;
                    currentLocation = new Location("");
                    currentLocation.setLatitude(address.lat);
                    currentLocation.setLongitude(address.lon);
                    Location curr = null;
                    if (SMLocationManager.getInstance().hasValidLocation())
                        curr = SMLocationManager.getInstance().getLastValidLocation();
                    if (curr != null) {
                        String st = "Start: (" + curr.getLatitude() + "," + curr.getLongitude() + ") End: (" + address.lat + "," + address.lon + ")";
                        IbikeApplication.getTracker().sendEvent("Route", "Pin", st, (long) 0);
                    }
                    infoLine1 = address.street + " " + address.houseNumber;
                    pinInfoLine1.setText(infoLine1.trim().equals("") ? String.format("%.6f", address.lat) + ",\n"
                            + String.format("%.6f", address.lon) : infoLine1);
                    destination = pinInfoLine1.getText().toString();
                    if (!IbikeApplication.isUserLogedIn())
                        btnSaveFavorite.setImageResource(R.drawable.drop_pin_add_fav_btn_active);
                    else {
                        btnSaveFavorite.setImageResource(R.drawable.drop_pin_selector);
                        isSaveFaveoriteEnabled = true;
                    }
                }
                break;

        }
        if (leftMenu != null) {
            leftMenu.favoritesEnabled = true;
        }
        progressBar.setVisibility(View.GONE);
    }

    public void startRouting(Location start, Location end, JsonNode jsonRoot, String startName, String endName) {
        Intent i = new Intent(this, getNavigationClass());
        i.putExtra("start_lat", start.getLatitude());
        i.putExtra("start_lng", start.getLongitude());
        i.putExtra("end_lat", end.getLatitude());
        i.putExtra("end_lng", end.getLongitude());
        if (jsonRoot != null)
            i.putExtra("json_root", jsonRoot.toString());
        i.putExtra("source", source);
        i.putExtra("destination", destination);
        if (jsonRoot != null && jsonRoot.has("route_summary")) {
            i.putExtra("start_name", jsonRoot.get("route_summary").get("start_point").asText());
            i.putExtra("end_name", jsonRoot.get("route_summary").get("end_point").asText());
        } else {
            i.putExtra("start_name", startName);
            i.putExtra("end_name", endName);
        }
        i.putExtra("overlays", getOverlaysShown());
        new DB(MapActivity.this).saveSearchHistory(new HistoryData(infoLine1, end.getLatitude(), end.getLongitude()), new HistoryData(
                IbikeApplication.getString("current_position"), start.getLatitude(), start.getLongitude()), MapActivity.this);
        this.startActivityForResult(i, 1);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        LOG.d("route found");

    }

    protected Class<?> getNavigationClass() {
        return SMRouteNavigationActivity.class;
    }

    public void togglePinInfoLayoutVisibility() {
        if (pinInfoLayout != null) {
            pinInfoLayout.setVisibility(pinInfoLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        }
    }

    public void showPinInfoLayout() {
        if (pinInfoLayout != null) {
            pinInfoLayout.setVisibility(View.VISIBLE);
            mapFragment.onBottomViewShown();
        }
    }

    public void updatePinInfo(String line1, String line2) {
        pinInfoLine1.setText(line1);
        destination = line1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == ProfileActivity.RESULT_USER_DELETED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(IbikeApplication.getString("account_deleted"));
            builder.setPositiveButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            dialog.show();
        } else if (resultCode == SearchActivity.RESULT_SEARCH_ROUTE) {
            if (data != null) {
                Bundle extras = data.getExtras();
                Location start = Util.locationFromCoordinates(extras.getDouble("startLat"), extras.getDouble("startLng"));
                Location endLocation = Util.locationFromCoordinates(extras.getDouble("endLat"), extras.getDouble("endLng"));
                if (extras.containsKey("fromName"))
                    source = extras.getString("fromName");
                else
                    source = IbikeApplication.getString("current_position");
                if (extras.containsKey("toName"))
                    destination = extras.getString("toName");
                else
                    destination = "";
                new SMHttpRequest().getRoute(start, endLocation, null, MapActivity.this);
            }
        } else if (resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
            try {
                ((AddFavoriteFragment) getFragmentManager().findFragmentById(R.id.leftContainerDrawer)).onActivityResult(requestCode, resultCode,
                        data);
            } catch (Exception e) {
                try {
                    ((EditFavoriteFragment) getFragmentManager().findFragmentById(R.id.leftContainerDrawer)).onActivityResult(requestCode,
                            resultCode, data);
                } catch (Exception ex) {
                }
            }
        } else if (resultCode == RESULT_RETURN_FROM_NAVIGATION) {
            btnSaveFavorite.setImageResource(R.drawable.drop_pin_selector);
            pinInfoLayout.setVisibility(View.GONE);
            mapFragment.pinView.setVisibility(View.GONE);
            if (mapFragment.pinB != null) {
                mapFragment.mapView.getOverlayManager().remove(mapFragment.pinB);
            }
            if (data != null && data.getExtras() != null && data.getExtras().containsKey("overlaysShown")) {
                refreshOverlays(data.getIntExtra("overlaysShown", 0));
            }
            
        // *** DANGER, WILL ROBINSON: I'm looking at the request code, not the return code from this point on. /jc ***
        // Take care of starting navigation once a favorite has been clicked in the FavoritesListActivity
        } else if (requestCode == LeftMenu.LAUNCH_FAVORITE && resultCode == RESULT_OK){ 
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

    float newX;

    public void reloadStrings() {
        leftMenu.initStrings();
        leftMenu.reloadStrings();
        source = IbikeApplication.getString("current_position");
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        mapFragment.mapView.getTileProvider().clearTileCache();
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    public void refreshOverlays(int overlaysShown) {

    }

    public int getOverlaysShown() {
        return 0;
    }

    public void enableAddFavourite() {
        isSaveFaveoriteEnabled = true;
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

}
