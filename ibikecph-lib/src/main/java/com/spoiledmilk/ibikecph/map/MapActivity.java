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
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.iLanguageListener;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.search.SearchActivity;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;

/**
 * The main map view.
 *
 * TODO: Look into ways of making this class shorter.
 * @author jens
 *
 */
@SuppressLint("NewApi")
public class MapActivity extends Activity implements iLanguageListener {
    public final static int REQUEST_START_NAVIGATION = 2;
    public final static int RESULT_RETURN_FROM_NAVIGATION = 105;

    protected LeftMenu leftMenu;
    private DrawerLayout drawerLayout;
    private MaterialMenuIcon materialMenu;
    protected IBCMapView mapView;
    private ArrayList<InfoPaneFragment> fragments = new ArrayList<InfoPaneFragment>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main_map_activity);

        this.mapView = (IBCMapView) findViewById(R.id.mapView);
        mapView.init(IBCMapView.MapState.DEFAULT, this);

        // We want the hamburger in the ActionBar
        materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);

        // LeftMenu
        initLeftMenu(savedInstanceState);

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
            startActivityForResult(i, REQUEST_START_NAVIGATION);
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


    public void reloadStrings() {
        leftMenu.initStrings();
        leftMenu.reloadStrings();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == ProfileActivity.RESULT_USER_DELETED) {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(IbikeApplication.getString("account_deleted"));
            builder.setPositiveButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            dialog.show();

        } else if (requestCode == REQUEST_START_NAVIGATION && resultCode == RESULT_OK) {
            Log.d("JC", "Started navigation");
            if (data != null) {
                final Bundle extras = data.getExtras();
                Location start = Util.locationFromCoordinates(extras.getDouble("startLat"), extras.getDouble("startLng"));
                Location end = Util.locationFromCoordinates(extras.getDouble("endLat"), extras.getDouble("endLng"));

                Geocoder.getRoute(new LatLng(start), new LatLng(end), new Geocoder.RouteCallback() {
                    @Override
                    public void onSuccess(SMRoute route) {
                        Log.d("JC", "Got SMRoute");
                        route.startStationName = extras.getString("fromName");
                        route.endStationName = extras.getString("toName");
                        mapView.startRouting(route);
                    }

                    @Override
                    public void onFailure() {
                        Log.e("JC", "Did not get SMRoute");
                    }
                }, null);
            }

        }
    }

    public void registerFragment(InfoPaneFragment fragment) {
        fragments.add(fragment);
    }

    public void unregisterFragment(InfoPaneFragment fragment) {
        fragments.remove(fragment);
    }

    /**
     * Checks with all registered fragments if they're OK with letting back be pressed. They should return false if they
     * want to do something before letting the user continue back.
     */
    public void onBackPressed() {
        if (mapView.getMapHandler().onBackPressed()) {
            super.onBackPressed();
        }
    }
}
