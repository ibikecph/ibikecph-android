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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.fasterxml.jackson.databind.JsonNode;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.TermsManager;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.search.SearchActivity;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.tracking.TrackHelper;
import com.spoiledmilk.ibikecph.tracking.TrackingInfoPaneFragment;
import com.spoiledmilk.ibikecph.tracking.TrackingManager;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.IbikePreferences;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;
import com.viewpagerindicator.CirclePageIndicator;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.exceptions.RealmMigrationNeededException;

/**
 * The main map view.
 * <p/>
 * TODO: Look into ways of making this class shorter.
 *
 * @author jens
 */
@SuppressLint("NewApi")
public class MapActivity extends IBCMapActivity {
    public final static int REQUEST_SEARCH_ADDRESS = 2;
    public final static int REQUEST_CHANGE_SOURCE_ADDRESS = 250;
    public final static int REQUEST_CHANGE_DESTINATION_ADDRESS = 251;
    public final static int RESULT_RETURN_FROM_NAVIGATION = 105;

    public static Context mapActivityContext;

    protected LeftMenu leftMenu;
    private DrawerLayout drawerLayout;
    private MaterialMenuIcon materialMenu;
    protected IBCMapView mapView;
    private ArrayList<InfoPaneFragment> fragments = new ArrayList<InfoPaneFragment>();
    private IbikePreferences settings;
    public static View frag;
    public static View breakFrag;
    public static CirclePageIndicator tabs;
    public static ViewPager pager;
    public static ProgressBar progressBar;
    public static FrameLayout progressBarHolder;
    public static boolean fromSearch = false;
    public static ObservableInteger obsInt;
    public int amount = 0;
    public static JsonNode breakRouteJSON = null;
    public static boolean format;
    public static boolean isBreakChosen = false;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapActivityContext = this;
        this.setContentView(R.layout.main_map_activity);
        this.settings = IbikeApplication.getSettings();
        frag = findViewById(R.id.infoPaneContainer);
        breakFrag = findViewById(R.id.breakRouteContainer);
        tabs = (CirclePageIndicator) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);

        // true if 24-hour format, false if 12-hour format
        format = DateFormat.is24HourFormat(this);

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
            public void onDrawerStateChanged(int newState) {
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }
        });


        // We need a LocationListener to have the service be able to provide GPS coords.
        // TODO: This is kind of a hack :( The service only requests GPS upstream if it has listeners. Should be able
        // to give a one-off coordinate.
        IbikeApplication.getService().addGPSListener(new DummyLocationListener());

        // When scrolling the map, make sure that the compass icon is updated.
        this.mapView.addListener(new MapListener() {
            @Override
            public void onScroll(ScrollEvent scrollEvent) {
                updateUserTrackingState();
            }

            @Override
            public void onZoom(ZoomEvent zoomEvent) {
                updateUserTrackingState();
            }

            @Override
            public void onRotate(RotateEvent rotateEvent) {
                updateUserTrackingState();
            }
        });


        // Check if the user accepts the newest terms
        // TermsManager.checkTerms(this);

        if (IbikeApplication.getService().hasValidLocation()) {
            this.mapView.setCenter(new LatLng(IbikeApplication.getService().getLastValidLocation()));
        }

        this.mapView.getUserLocationOverlay().enableFollowLocation();
        this.mapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        updateUserTrackingState();
        TrackingManager.uploadTracksToServer();
        fragmentPageAmountListener();

    }

    /**
     * Initializes the LeftMenu
     *
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
            // to avoid too many not parcelable things, just set the map back to default state
            this.mapView.changeState(IBCMapView.MapState.DEFAULT);

            Intent i = new Intent(MapActivity.this, SearchActivity.class);
            startActivityForResult(i, REQUEST_SEARCH_ADDRESS);
            overridePendingTransition(R.anim.slide_in_down, R.anim.fixed);
        } else if (id == R.id.ab_problem) {
            ((NavigationMapHandler) this.mapView.getMapHandler()).problemButtonPressed();
        }
        // Toggle the drawer when tapping the app icon.
        else if (id == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
                materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
            } else {
                drawerLayout.openDrawer(Gravity.LEFT);
                materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);

            }
        } else if (id == R.id.uploadFakeTrack) {
            TrackingManager.uploadeFakeTrack();
            //TrackingManager.printAllTracks();
        } else if (id == R.id.createFakeTrack) {
            TrackingManager.createFakeTrack();
        } else if (id == R.id.resetSignature) {
            PreferenceManager.getDefaultSharedPreferences(MapActivity.this).edit().remove("signature").commit();
        } else if (id == R.id.resetAuthToken) {
            PreferenceManager.getDefaultSharedPreferences(MapActivity.this).edit().putString("auth_token", "123").commit();
        }

        return super.onOptionsItemSelected(item);
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
//        MapActivity.breakFrag.setVisibility(View.VISIBLE);
        if (settings.getTrackingEnabled() && !fromSearch && !OverviewMapHandler.isWatchingAddress) {
            showStatisticsInfoPane();
        } else if (!fromSearch && OverviewMapHandler.isWatchingAddress) {
            MapActivity.frag.setVisibility(View.VISIBLE);
            //mapView.showAddress(OverviewMapHandler.addressBeingWatched);
        } else if (!fromSearch && !OverviewMapHandler.isWatchingAddress) {
            disableStatisticsInfoPane();
        }

        fromSearch = false;

        if (!Util.isNetworkConnected(this)) {
            Util.launchNoConnectionDialog(this);
        }
        checkForCrashes();
        getLeftMenu().onResume();

        // Check if the user accepts the newest terms
        TermsManager.checkTerms(this);

        // Check if the user was logged out/deleted and spawn a dialog
        Intent intent = getIntent();
        if (intent.hasExtra("loggedOut")) {

            if (intent.getExtras().getBoolean("loggedOut")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(IbikeApplication.getString("invalid_token_user_logged_out"));
                builder.setPositiveButton(IbikeApplication.getString("log_in"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MapActivity.this, LoginActivity.class);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(IbikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            }
            intent.removeExtra("loggedOut");
        } else if (intent.hasExtra("deleteUser")) {

            if (intent.getExtras().getBoolean("deleteUser")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setMessage(IbikeApplication.getString("account_deleted"));
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            intent.removeExtra("deleteUser");
        }
        // Ensure all tracks have been geocoded.
        try {
            TrackHelper.ensureAllTracksGeocoded();
        } catch (RealmMigrationNeededException e) {
            // If we need to migrate Realm, just delete the file
            /* FIXME: This should clearly not go into production. We should decide on a proper DB schema, and make proper
               migrations if we need to change it. */
            Log.d("JC", "Migration needed, deleting the Realm file!");
            Realm.deleteRealmFile(this);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        leftMenu = getLeftMenu();
        leftMenu.onResume();
    }

    protected void checkForCrashes() {
        CrashManager.register(this, Config.HOCKEY_APP_ID);
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

        menu.findItem(R.id.ab_problem).setVisible(this.problemButtonVisible);

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("JC", "onActivityResult, requestCode " + requestCode + " resultCode " + resultCode);

        if (requestCode == LeftMenu.LAUNCH_LOGIN) {
            Log.d("JC", "Got back from LAUNCH_LOGIN");
            if (!OverviewMapHandler.isWatchingAddress) {
                this.mapView.changeState(IBCMapView.MapState.DEFAULT);
            }
            leftMenu.populateMenu();
        } else if (resultCode == ProfileActivity.RESULT_USER_DELETED) {
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
        } else if (requestCode == REQUEST_SEARCH_ADDRESS && resultCode == RESULT_OK) {
            Log.d("JC", "Got back from address search, spawning");
            final Bundle extras = data.getExtras();
            Address address = (Address) extras.getSerializable("addressObject");
            if (address != null) {
                if (address.getAddressSource() == Address.AddressSource.FAVORITE) {
                    address.setHouseNumber("");
                }
                MapActivity.frag.setVisibility(View.VISIBLE);
                mapView.showAddress(address);
                mapView.setCenter(address.getLocation());
                fromSearch = true;
                OverviewMapHandler.isWatchingAddress = true;
            } else {
                LatLng destination = new LatLng(extras.getDouble("endLat"), extras.getDouble("endLng"));

                Geocoder.getAddressForLocation(destination, new Geocoder.GeocoderCallback() {
                    @Override
                    public void onSuccess(Address address) {
                        mapView.showAddress(address);
                    }

                    @Override
                    public void onFailure() {

                    }
                });
                // Center the map around the search result.
                this.mapView.setCenter(destination, true);
            }
        } else if (requestCode == REQUEST_SEARCH_ADDRESS && resultCode == RESULT_CANCELED) {
            Log.d("JC", "Got back from address search with RESULT_CANCELED!");
            if (!OverviewMapHandler.isWatchingAddress) {
                showStatisticsInfoPane();
            } else {
                fromSearch = true;
                MapActivity.frag.setVisibility(View.VISIBLE);
            }
        } else if (requestCode == REQUEST_CHANGE_SOURCE_ADDRESS && resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
            this.mapView.changeState(IBCMapView.MapState.DEFAULT);
            Log.d("JC", "Got back from address search, spawning");
            final Bundle extras = data.getExtras();
            Address address = (Address) extras.getSerializable("addressObject");
            if (address != null) {
                MapActivity.frag.setVisibility(View.VISIBLE);
                mapView.showAddress(address);
                mapView.setCenter(address.getLocation());
                fromSearch = true;
            } else {
                LatLng destination = new LatLng(extras.getDouble("endLat"), extras.getDouble("endLng"));

                Geocoder.getAddressForLocation(destination, new Geocoder.GeocoderCallback() {
                    @Override
                    public void onSuccess(Address address) {
                        mapView.showAddress(address);
                    }

                    @Override
                    public void onFailure() {

                    }
                });
                // Center the map around the search result.
                this.mapView.setCenter(destination, true);
            }
        } else if (requestCode == REQUEST_CHANGE_DESTINATION_ADDRESS && resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
            this.mapView.changeState(IBCMapView.MapState.DEFAULT);
            Log.d("JC", "Got back from address search, spawning");
            final Bundle extras = data.getExtras();
            Address address = (Address) extras.getSerializable("addressObject");
            if (address != null) {
                MapActivity.frag.setVisibility(View.VISIBLE);
                mapView.showAddress(address);
                mapView.setCenter(address.getLocation());
                fromSearch = true;
            } else {
                LatLng destination = new LatLng(extras.getDouble("endLat"), extras.getDouble("endLng"));

                Geocoder.getAddressForLocation(destination, new Geocoder.GeocoderCallback() {
                    @Override
                    public void onSuccess(Address address) {
                        mapView.showAddress(address);
                    }

                    @Override
                    public void onFailure() {

                    }
                });
                // Center the map around the search result.
                this.mapView.setCenter(destination, true);
            }
        }
        // We got a favorite to navigate to
        else if (requestCode == LeftMenu.LAUNCH_FAVORITE) {
            if (resultCode == RESULT_OK) {
                FavoritesData fd = data.getExtras().getParcelable("ROUTE_TO");
                mapView.showRoute(fd);
                Address a = Address.fromFavoritesData(fd);
                mapView.showAddressFromFavorite(a);
                OverviewMapHandler.isWatchingAddress = true;

            } else {
                if (!OverviewMapHandler.isWatchingAddress) {
                    this.mapView.changeState(IBCMapView.MapState.DEFAULT);
                }
            }
            // Close the LeftMenu
            drawerLayout.closeDrawer(Gravity.LEFT);
        } else if (requestCode == LeftMenu.LAUNCH_TRACKING) {
            Log.d("JC", "Got back from LAUNCH_TRACKING");
            if (!OverviewMapHandler.isWatchingAddress) {
                this.mapView.changeState(IBCMapView.MapState.DEFAULT);
            }
            leftMenu.populateMenu();
        } else if (requestCode == LeftMenu.LAUNCH_ABOUT) {
            Log.d("JC", "Got back from T");
            if (!OverviewMapHandler.isWatchingAddress) {
                this.mapView.changeState(IBCMapView.MapState.DEFAULT);
            }
            leftMenu.populateMenu();
        }
    }

    private void showStatisticsInfoPane() {
        frag.setVisibility(View.VISIBLE);
        FragmentManager fm = mapView.getParentActivity().getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.infoPaneContainer, new TrackingInfoPaneFragment());
        ft.commit();
        Log.d("DV", "Infopanefragment added!");

        OverviewMapHandler.isWatchingAddress = false;
    }

    private void disableStatisticsInfoPane() {

        frag.setVisibility(View.GONE);
        breakFrag.setVisibility(View.GONE);
        progressBarHolder.setVisibility(View.GONE);
        mapView.removeAllMarkers();

        /*Fragment fragment = mapView.getParentActivity().getFragmentManager().findFragmentByTag("infopane");
        if (fragment != null)
            fragment.getFragmentManager().beginTransaction().hide(fragment).commit();
        Log.d("DV", "Infopanefragment removed!");*/
        //OverviewMapHandler.isWatchingAddress = false;
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
            if (!OverviewMapHandler.isWatchingAddress) {
                super.onBackPressed();
            }
        }
        breakFrag.setVisibility(View.GONE);
        progressBarHolder.setVisibility(View.GONE);
        mapView.removeAllMarkers();

    }

    public void userTrackingButtonOnClick(View v) {
        UserLocationOverlay.TrackingMode curMode = this.mapView.getUserLocationTrackingMode();

        if (curMode == UserLocationOverlay.TrackingMode.NONE) {
            //this.mapView.setUserLocationEnabled(true);
            this.mapView.getUserLocationOverlay().enableFollowLocation();
            this.mapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.FOLLOW);
        } else {
            //this.mapView.setUserLocationEnabled(false);
            this.mapView.getUserLocationOverlay().disableFollowLocation();
            this.mapView.setUserLocationTrackingMode(UserLocationOverlay.TrackingMode.NONE);
        }

        updateUserTrackingState();
    }

    /**
     * Called when the user scrolls the map. Updates the compass.
     */
    public void updateUserTrackingState() {
        UserLocationOverlay.TrackingMode curMode = this.mapView.getUserLocationTrackingMode();
        ImageButton userTrackingButton = (ImageButton) this.findViewById(R.id.userTrackingButton);

        if (curMode == UserLocationOverlay.TrackingMode.NONE) {
            userTrackingButton.setImageDrawable(getResources().getDrawable(R.drawable.compass_not_tracking));
        } else {
            userTrackingButton.setImageDrawable(getResources().getDrawable(R.drawable.compass_tracking));
        }
    }

    /*
    Fragment handling section
    */

    // Listening on variable being set to the amount of breakRoute suggestions, in order to display this amount of pages in the fragmentAdapter.
    public void fragmentPageAmountListener() {

        obsInt = new ObservableInteger();

        obsInt.setOnIntegerChangeListener(new OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                amount = newValue;
                Log.d("DV", "Amount changed to " + newValue);
                if (newValue > 0) {
                    Log.d("DV", "Amount > 0, enabling fragment!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBarHolder.setVisibility(View.GONE);
                            breakFrag.setVisibility(View.VISIBLE);
                            pager.setVisibility(View.VISIBLE);
                            tabs.setVisibility(View.VISIBLE);
                            pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
                            tabs.setViewPager(pager);
                            tabs.setRadius(10);
                            tabs.setCentered(true);
                            tabs.setFillColor(Color.parseColor("#E2A500"));
                        }
                    });
                    tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                        }

                        @Override
                        public void onPageSelected(int position) {
                            NavigationMapHandler.obsInt.setPageValue(position);
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {
                        }
                    });
                }
            }
        });
    }

    class MyPagerAdapter extends FragmentStatePagerAdapter {

        @Override
        public int getCount() {
            return amount;
        }

        public MyPagerAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return BreakRouteFragment.newInstance(position);
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }


       /* @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.d("DV", "destroyItem!");
            if (position >= getCount()) {
                android.support.v4.app.FragmentManager manager = ((Fragment) object).getFragmentManager();
                android.support.v4.app.FragmentTransaction trans = manager.beginTransaction();
                trans.remove((Fragment) object);
                trans.commit();
            }
            notifyDataSetChanged();
        }*/

    }
}

            /*
            Log.d("JC", "Got coordinates to navigate to");
            if (data != null) {
                final Bundle extras = data.getExtras();
                Location start = Util.locationFromCoordinates(extras.getDouble("startLat"), extras.getDouble("startLng"));
                Location end = Util.locationFromCoordinates(extras.getDouble("endLat"), extras.getDouble("endLng"));

                // TODO: Throwing stuff around between Location and ILatLng like it ain't a thing. Drop it.
                Geocoder.getRoute(new LatLng(start), new LatLng(end), new Geocoder.RouteCallback() {
                    @Override
                    public void onSuccess(SMRoute route) {
                        Log.d("JC", "Got SMRoute");
                        route.startStationName = extras.getString("fromName");
                        route.endStationName = extras.getString("toName");
                        mapView.showRouteOverview(route);
                    }

                    @Override
                    public void onFailure() {
                        Log.e("JC", "Did not get SMRoute");
                    }
                }, null);
            }
            */