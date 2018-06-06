// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.balysv.materialmenu.MaterialMenuIcon;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.google.android.gms.location.LocationListener;
import com.mapbox.mapboxsdk.events.MapListener;
import com.mapbox.mapboxsdk.events.RotateEvent;
import com.mapbox.mapboxsdk.events.ScrollEvent;
import com.mapbox.mapboxsdk.events.ZoomEvent;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.overlay.Overlay;
import com.mapbox.mapboxsdk.overlay.UserLocationOverlay;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.TermsManager;
import com.spoiledmilk.ibikecph.favorites.FavoriteListItem;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.map.fragments.BreakRouteSelectionFragment;
import com.spoiledmilk.ibikecph.map.fragments.RouteSelectionFragment;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.map.overlays.TogglableOverlay;
import com.spoiledmilk.ibikecph.map.overlays.TogglableOverlayFactory;
import com.spoiledmilk.ibikecph.map.states.BrowsingState;
import com.spoiledmilk.ibikecph.map.states.DestinationPreviewState;
import com.spoiledmilk.ibikecph.map.states.MapState;
import com.spoiledmilk.ibikecph.map.states.NavigatingState;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.search.SearchActivity;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.tracking.TrackHelper;
import com.spoiledmilk.ibikecph.tracking.TrackingManager;
import com.spoiledmilk.ibikecph.tracking.TrackingStatisticsFragment;
import com.spoiledmilk.ibikecph.util.Util;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.List;

import io.realm.exceptions.RealmMigrationNeededException;

/**
 * The main map view.
 * <p/>
 * TODO: Look into ways of making this class shorter.
 *
 * @author jens
 */
@SuppressLint("NewApi")
public class MapActivity extends BaseMapActivity {

    public final static int REQUEST_SEARCH_ADDRESS = 2;
    public final static int REQUEST_CHANGE_SOURCE_ADDRESS = 250;
    public final static int REQUEST_CHANGE_DESTINATION_ADDRESS = 251;
    public final static int RESULT_RETURN_FROM_NAVIGATION = 105;

    public final static int PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    protected MapState state;

    public static Context mapActivityContext;
    // This is used to throttle calls to setting image resource on the compas
    protected UserLocationOverlay.TrackingMode previousTrackingMode;

    protected LeftMenu leftMenu;
    private DrawerLayout drawerLayout;
    private MaterialMenuIcon materialMenu;
    protected IBCMapView mapView;

    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static View topFragment;
    /**
     * @deprecated Let's phase out the use of static members like this.
     */
    public static boolean fromSearch = false;
    /**
     * @deprecated Let's phase out the use of static booleans like this.
     */
    public static boolean isBreakChosen = false;

    private static final String TAG = "MapActivity";

    protected LocationListener locationListener;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Link the activity to the map activity layout.
        setContentView(R.layout.map_activity);

        // Make sure the app icon is clickable
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowCustomEnabled(true);

        // Create the drawer menu to the left.
        createMenu();

        // TODO: Remove this after reimplementing the logout* methods of the IBikeApplication class
        mapActivityContext = this;

        // Finding the sub-components of the activity's view, consider if these need to be static
        // or if we could pass a reference to this activity to the components that needs access
        topFragment = findViewById(R.id.topFragment);

        // Initialize the map view
        mapView = (IBCMapView) findViewById(R.id.mapView);
        mapView.init(this);

        changeState(BrowsingState.class);

        initializeSelectableOverlays();

        // Check for HockeyApp updates
        try {
            UpdateManager.register(this);
        } catch (IllegalArgumentException e) {
            Log.i("HockeyApp", "No HockeyApp app identifier provided - HockeyApp is disabled");
        }

        // Check if the user accepts the newest terms
        // TermsManager.checkTerms(this);

        // When scrolling the map, make sure that the compass icon is updated.
        this.mapView.addListener(new MapListener() {
            @Override
            public void onScroll(ScrollEvent scrollEvent) {
                updateCompassIcon();
            }

            @Override
            public void onZoom(ZoomEvent zoomEvent) {
                updateCompassIcon();
            }

            @Override
            public void onRotate(RotateEvent rotateEvent) {
                updateCompassIcon();
            }
        });

        // Uploads tracks to the server - TODO: consider removing this call and class entirely.
        TrackingManager.uploadTracksToServer();
    }

    protected void initializeSelectableOverlays() {
        final TogglableOverlayFactory factory = TogglableOverlayFactory.getInstance();
        factory.addOnOverlaysLoadedListener(new TogglableOverlayFactory.OnOverlaysLoadedListener() {
            @Override
            public void onOverlaysLoaded(List<TogglableOverlay> togglableOverlays) {
                // Add all the overlays to the map view
                for (TogglableOverlay togglableOverlay : factory.getTogglableOverlays()) {
                    for (Overlay overlay : togglableOverlay.getOverlays()) {
                        mapView.addOverlay(overlay);
                    }
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();

        attemptToRegisterLocationListener();

        /*
        LOG.d("Map activity onResume");
        if (IBikeApplication.getSettings().getTrackingEnabled() && !fromSearch && !OverviewMapHandler.isWatchingAddress) {
            showStatisticsInfoPane();
        } else if (!fromSearch && OverviewMapHandler.isWatchingAddress) {
            MapActivity.topFragment.setVisibility(View.VISIBLE);
            //mapView.showAddress(OverviewMapHandler.addressBeingWatched);
        } else if (!fromSearch && !OverviewMapHandler.isWatchingAddress) {
            disableStatisticsInfoPane();
        }
        */

        fromSearch = false;

        if (!Util.isNetworkConnected(this)) {
            Util.launchNoConnectionDialog(this);
        }
        checkForCrashes();
        // TODO: Check if this is even needed as the menu has been added using the fragment manager.
        leftMenu.onResume();

        // Check if the user accepts the newest terms
        TermsManager.checkTerms(this);

        // Check if the user was logged out/deleted and spawn a dialog
        Intent intent = getIntent();
        if (intent.hasExtra("loggedOut")) {

            if (intent.getExtras().getBoolean("loggedOut")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(IBikeApplication.getString("invalid_token_user_logged_out"));
                builder.setPositiveButton(IBikeApplication.getString("log_in"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MapActivity.this, LoginActivity.class);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(IBikeApplication.getString("close"), new DialogInterface.OnClickListener() {
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
                builder.setMessage(IBikeApplication.getString("account_deleted"));
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
            // FIXME²: This method has been removed from Realm.
            // Might be useful: https://realm.io/docs/java/latest/api/io/realm/Realm.html#deleteRealm-io.realm.RealmConfiguration-
            // Realm.deleteRealmFile(this);
        }
    }

    /**
     * Transition the map activity to another state.
     *
     * @param toState the new state
     */
    public void changeState(MapState toState) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        MapState fromState = state;
        String logMessage = "Changed state ";

        // Give the state a reference back to this activity.
        toState.setMapActivity(this);

        // Transition away from the current state.
        if (fromState != null) {
            logMessage += String.format("from %s ", fromState);
            fromState.transitionAway(toState, fragmentTransaction);
        }

        // Transition to the new state
        state = toState;
        logMessage += String.format("to %s", toState);
        toState.transitionTowards(fromState, fragmentTransaction);

        fragmentTransaction.commit();

        // Insert this as info in the log
        Log.i(TAG, logMessage);
    }

    /**
     * Transitions state to some state of the class provided.
     *
     * @param stateClass
     * @return the existing or new state, useful when chaining.
     */
    public <MS extends MapState> MS changeState(Class<? extends MapState> stateClass) {
        try {
            MS newState = (MS) stateClass.newInstance();
            changeState(newState);
            return newState;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the current state of the MapActivity.
     *
     * @return
     */
    public MapState getState() {
        return state;
    }

    public IBCMapView getMapView() {
        if (mapView == null) {
            throw new RuntimeException("The mapView has not yet been initialized.");
        }
        return mapView;
    }

    /**
     * Instantiate the left menu - this needs to be a method, so it can be overwritten.
     *
     * @return
     */
    protected LeftMenu createLeftMenu() {
        return new LeftMenu();
    }

    /**
     * Creates the material menu icon that animates when the drawer changes state.
     */
    protected void createMenu() {
        // Creating the left menu fragment
        leftMenu = createLeftMenu();

        // Find the drawer layout view using it's id, we'll attach the menu to that.
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Add the menu to the drawer layout
        getFragmentManager()
                .beginTransaction()
                .add(R.id.leftContainerDrawer, leftMenu)
                .commit();

        // We want the hamburger in the ActionBar
        materialMenu = new MaterialMenuIcon(this, Color.WHITE, MaterialMenuDrawable.Stroke.THIN);
        materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);

        // When the drawer opens or closes, we want the icon to animate between "burger" and "arrow"
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
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
    }

    /**
     * Checks if the app has sufficient permissions and updates the center of the map to the
     * current location upon receiving this.
     */
    private void attemptToRegisterLocationListener() {
        boolean hasLocationPermissions = checkAndRequestLocationPermissions();
        if (hasLocationPermissions && locationListener == null) {
            // We can register a location listener and start receiving location updates right away.
            locationListener = new LocationListener() {

                protected boolean hasUpdatedMap = false;

                @Override
                public void onLocationChanged(Location location) {
                    // Let's update the map only once - we use the hasUpdatedMap for this
                    // We cannot simply deregister the listener as this would stop updating the
                    // user's location on the map.
                    if (!hasUpdatedMap && state instanceof BrowsingState) {
                        Log.d("MapActivity", "Location changed and we center the map");
                        mapView.setCenter(new LatLng(location));
                        hasUpdatedMap = true;
                    }
                }
            };

            Log.d("MapActivity", "Adding map's locationListener to the LocationService.");
            // We need a LocationListener to have the service be able to provide GPS coordinates.
            IBikeApplication.getService().addLocationListener(locationListener);
        }
    }

    /**
     * Stop listening for locations by removing the location listener.
     */
    private void deregisterLocationListener() {
        Log.d("MapActivity", "deregisterLocationListener called");
        if (locationListener != null) {
            IBikeApplication.getService().removeLocationListener(locationListener);
            // Null this - as this is how we know if we've already added it to the location service.
            locationListener = null;
        }
    }

    /**
     * Checks if the app has permissions to the device's physical location and requests it if not.
     * The result of a permission request ends up calling the onRequestPermissionsResult method.
     */
    private boolean checkAndRequestLocationPermissions() {
        final String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        // Let's check if we have permissions to get file locations.
        int hasPermission = ContextCompat.checkSelfPermission(this.getApplicationContext(), permission);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSIONS_REQUEST_FINE_LOCATION);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Result is back from a request for permissions.
     * If this request was for the device location, we attempt to register the location listener.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MapActivity", "Got the permission to receive fine locations.");
                    attemptToRegisterLocationListener();
                }
                return;
            }
            default: {
                throw new RuntimeException("Request for permission was not handled");
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.ab_search) {
            this.mapView.clear();
            Intent i = new Intent(MapActivity.this, SearchActivity.class);
            startActivityForResult(i, REQUEST_SEARCH_ADDRESS);
            overridePendingTransition(R.anim.slide_in_down, R.anim.fixed);
        } else if (id == R.id.ab_problem) {
            if(state instanceof NavigatingState) {
                NavigatingState navigatingState = (NavigatingState) state;
                navigatingState.reportProblem();
            }
        }
        // Toggle the drawer when tapping the app icon.
        else if (id == android.R.id.home) {
            toggleDrawer();
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

    protected void toggleDrawer() {
        if (drawerLayout == null) {
            throw new RuntimeException("toggleDrawer called too soon, drawerLayout was null");
        }

        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
            // Start the animation right away, instead of waiting for the drawer to settle.
            materialMenu.animateState(MaterialMenuDrawable.IconState.BURGER);
        } else {
            drawerLayout.openDrawer(Gravity.LEFT);
            // Start the animation right away, instead of waiting for the drawer to settle.
            materialMenu.animateState(MaterialMenuDrawable.IconState.ARROW);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        leftMenu.onResume();
    }

    /**
     * Uses hockey app (if enabled) to check for crashes that can be reported back to Hockey App.
     */
    protected void checkForCrashes() {
        try {
            if (IBikeApplication.getSettings().isCrashReportingEnabled()) {
                CrashManager.register(this);
            } else {
                Log.i("HockeyApp", "User turned off crash reporting - HockeyApp is disabled");
            }
        } catch (IllegalArgumentException e) {
            Log.i("HockeyApp", "No HockeyApp app identifier provided - HockeyApp is disabled");
        }
    }

    AlertDialog loginDlg;

    private void launchLoginDialog() {
        if (loginDlg == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(IBikeApplication.getString("login"));
            builder.setMessage(IBikeApplication.getString("error_not_logged_in"));

            builder.setPositiveButton(IBikeApplication.getString("login"), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent i = new Intent(MapActivity.this, LoginActivity.class);
                    startActivity(i);
                }
            });
            builder.setNegativeButton(IBikeApplication.getString("close"), new DialogInterface.OnClickListener() {

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
        deregisterLocationListener();
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

        // Show the button to report problems, when in the navigating state.
        boolean navigating = state instanceof NavigatingState;
        menu.findItem(R.id.ab_problem).setVisible(navigating);

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult, requestCode " + requestCode + " resultCode " + resultCode);

        if (requestCode == LeftMenu.LAUNCH_LOGIN) {
            Log.d(TAG, "Got back from the user login");
            /*
            Log.d("JC", "Got back from LAUNCH_LOGIN");
            if (!OverviewMapHandler.isWatchingAddress) {
                this.mapView.changeState(IBCMapView.MapViewState.DEFAULT);
            }
            leftMenu.populateMenu();
            */
        }
        else if (resultCode == ProfileActivity.RESULT_USER_DELETED) {
            Log.d(TAG, "Got back from deleting the user");
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(IBikeApplication.getString("account_deleted"));
            builder.setPositiveButton(IBikeApplication.getString("close"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            dialog.show();
        }
        else if (requestCode == REQUEST_SEARCH_ADDRESS && resultCode == RESULT_OK) {
            Log.d(TAG, "Got back from address search, with an OK result");
            // Change state right away
            DestinationPreviewState state = this.changeState(DestinationPreviewState.class);
            // What address was selected?
            final Bundle extras = data.getExtras();
            Address address = (Address) extras.getSerializable("addressObject");

            if (address != null) {
                if (address.getSource() == Address.Source.FAVORITE) {
                    address.setHouseNumber("");
                }
                state.setDestination(address);
            } else {
                throw new RuntimeException("Expected an address");
            }
        } else if (requestCode == REQUEST_SEARCH_ADDRESS && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Got back from address search were the user canceled!");
            // throw new UnsupportedOperationException("Canceling the search address has not been implemented.");
        } else if (requestCode == REQUEST_CHANGE_SOURCE_ADDRESS && resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
            Log.d(TAG, "Got back from setting the source");
            if (state instanceof RouteSelectionState) {
                final Bundle extras = data.getExtras();
                Address address = (Address) extras.getSerializable("addressObject");
                if (address != null) {
                    ((RouteSelectionState) state).setSource(address);
                } else {
                    throw new RuntimeException("Expected an address");
                }
            }
        } else if (requestCode == REQUEST_CHANGE_DESTINATION_ADDRESS && resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
            Log.d(TAG, "Got back from setting the destination");
            if (state instanceof RouteSelectionState) {
                final Bundle extras = data.getExtras();
                Address address = (Address) extras.getSerializable("addressObject");
                if (address != null) {
                    ((RouteSelectionState) state).setDestination(address);
                } else {
                    throw new RuntimeException("Expected an address");
                }
            }
        } else if (requestCode == LeftMenu.LAUNCH_FAVORITE) {
            Log.d(TAG, "Got back from the favorite screen.");
            // We got a favorite to navigate to
            if (resultCode == RESULT_OK) {
                // TODO: Re-implement launching of favorites
                // throw new UnsupportedOperationException("Launching favorites not yet implemented using MapStates");
                FavoriteListItem fd = data.getExtras().getParcelable("ROUTE_TO");
                Address address = fd.getAddress();
                RouteSelectionState state = changeState(RouteSelectionState.class);
                state.setDestination(address);
                // mapView.showRoute(fd);
                //mapView.showAddressFromFavorite(a);
            }
            // Close the LeftMenu
            drawerLayout.closeDrawer(Gravity.LEFT);
        } else if (requestCode == LeftMenu.LAUNCH_TRACKING) {
            Log.d(TAG, "Got back from the tracking screen.");
        } else if (requestCode == LeftMenu.LAUNCH_ABOUT) {
            Log.d(TAG, "Got back from the about screen.");
        }
    }

    private void showStatisticsInfoPane() {
        boolean trackingEnabled = getResources().getBoolean(R.bool.trackingEnabled);
        if (trackingEnabled) {
            topFragment.setVisibility(View.VISIBLE);
            FragmentManager fm = mapView.getParentActivity().getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.topFragment, new TrackingStatisticsFragment());
            ft.commit();
            Log.d("DV", "Infopanefragment added!");

            OverviewMapHandler.isWatchingAddress = false;
        } else {
            Log.i(TAG, "showStatisticsInfoPane was called, but tracking is disabled.");
        }
    }

    /**
     * Checks with all registered fragments if they're OK with letting back be pressed.
     * They should return false if they want to do something before letting the user continue back.
     */
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        } else if (state != null) {
            if (state.onBackPressed() == MapState.BackPressBehaviour.PROPAGATE) {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void readAloudClicked(View v) {
        if (getState() instanceof NavigatingState) {
            NavigatingState state = (NavigatingState) getState();
            state.toggleReadAloud();
        }
    }

    public void compassClicked(View v) {
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
    }

    /**
     * Called when the user scrolls the map. Updates the compass.
     */
    public void updateCompassIcon() {
        UserLocationOverlay.TrackingMode currentTrackingMode = mapView.getUserLocationTrackingMode();
        // Without follow location enabled, we assume a NONE tracking mode.
        if (!mapView.getUserLocationOverlay().isFollowLocationEnabled()) {
            currentTrackingMode = UserLocationOverlay.TrackingMode.NONE;
        }

        if (previousTrackingMode != currentTrackingMode) {
            ImageButton userTrackingButton = (ImageButton) this.findViewById(R.id.userTrackingButton);

            if (currentTrackingMode == UserLocationOverlay.TrackingMode.NONE) {
                userTrackingButton.setImageDrawable(getResources().getDrawable(R.drawable.compass_not_tracking));
            } else {
                userTrackingButton.setImageDrawable(getResources().getDrawable(R.drawable.compass_tracking));
            }
            previousTrackingMode = currentTrackingMode;
        }
    }

    /**
     * Create the fragment used when the user selects a route, this is implemented as a function to
     * allow for implementations to override this behaviour.
     * @return
     */
    public RouteSelectionFragment createRouteSelectionFragment() {
        IBikeApplication application = (IBikeApplication) getApplication();
        if(application.breakRouteIsEnabled()) {
            return new BreakRouteSelectionFragment();
        } else {
            return new RouteSelectionFragment();
        }
    }

}
