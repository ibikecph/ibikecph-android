package com.spoiledmilk.ibikecph;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.CopyOnWriteArrayList;


/**
 * A Service responsible for keeping track of GPS updates. This is done so that
 * we can still keep track of a navigation context, even if the screen is off.
 * And we can use it for recording tracks when the app is in the background.
 *
 * @author jens
 */
public class BikeLocationService extends Service implements LocationListener, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final int UPDATE_INTERVAL = 2000;
    private final IBinder binder = new BikeLocationServiceBinder();
    //LocationManager androidLocationManager;
    WakeLock wakeLock;
    //Location prevLastValidLocation;
    //Location lastValidLocation;

    public boolean locationServicesEnabledOnPhone;
    CopyOnWriteArrayList<android.location.LocationListener> gpsListeners = new CopyOnWriteArrayList<LocationListener>();
    boolean isListeningForGPS = false;
    private static BikeLocationService instance;

    // FusedLocationProvider
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location lastValidLocation;
    protected Location prevLastValidLocation;
    final static long INTERVAL = 2000;
    final static long FASTEST_INTERVAL = 2000;
    boolean isBuild = false;

    public ActivityRecognitionClient getActivityRecognitionClient() {
        return activityRecognitionClient;
    }

    private ActivityRecognitionClient activityRecognitionClient;

    /**
     * Instantiates a location manager and an (as yet unused) wake lock.
     */
    public BikeLocationService() {
        super();
        instance = this;
        Log.i("JC", "BikeLocationService instantiated.");
    }

    public void onCreate(Intent i) {
        instance = this;
    }

    // Start FusedLocationProvider

    protected synchronized void buildGoogleApiClient() {
        Log.d("DV", "buildGoogleApiClient - building!");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("DV", "innerOnConnected called!");
                        createLocationRequest();
                        startLocationUpdates();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                }).addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        isBuild = true;
        mGoogleApiClient.connect();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d("DV", "startLocationUpdates - location updates started!");
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        Log.d("DV", "stopLocationUpdates - location updates stopped!");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("DV", "outerOnConnected called!");
        prevLastValidLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    // End FusedLocationProvider

    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;

        Context context = IbikeApplication.getContext();
        //this.androidLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        /**
         * Instantiate a wake lock so we can keep tracking while the phone is off. We're not acquiring it until the
         * user starts navigation.
         */
        PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
        this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BikeLocationService");


        if (activityRecognitionClient == null) {
            Log.d("JC", "Spawning new ActivityRecognitionClient");

            activityRecognitionClient = new ActivityRecognitionClient();
            activityRecognitionClient.connect();
        }

        Log.d("JC", "BikeLocationService started.");
        return START_STICKY;
    }

    public void addGPSListener(LocationListener listener) {
        Log.d("DV", "addGPSListener called");
        // FusedLocationProvider
        if (!isBuild) {
            buildGoogleApiClient();
        } else {
            startLocationUpdates();
        }
        /*else{
            onListenersChange();
        }*/
        if (!gpsListeners.contains(listener)) {
            gpsListeners.add(listener);
        }
        // end FLP
        //onListenersChange();
    }

    public void removeGPSListener(LocationListener listener) {
        Log.d("DV", "removeGPSListener called");
        gpsListeners.remove(listener);
        stopLocationUpdates();
        //onListenersChange();
    }


    /**
     * Called whenever a GPS listener is registered or unregistered. Registers or unregisters the
     * service as a GPS listener with the Android operating system, as needed.
     * <p/>
     * This event is always called *after* adding or removing listeners from the list, so we need
     * to take care of two cases. If
     */
    public void onListenersChange() {

        // We registered a listener (and we can't be down from 2 because we weren't listening for
        // GPS in the first place)
        if (gpsListeners.size() == 1 && !isListeningForGPS) {
            Log.d("JC", "GPS listener added to the BikeLocationService, started listening for locations upstream.");
            startLocationUpdates();

            //this.androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, this);

            // Allow mock updates. Should be disabled
            // this.androidLocationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

            try {
                //this.androidLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, this);
            } catch (IllegalArgumentException exp) {
                // Would throw exception if NETWORK_PROVIDER isn't available. We don't care because it would just
                // add to the precision, but it's not strictly needed since we're already getting GPS coords.
            }

            wakeLock.acquire();
            isListeningForGPS = true;
        }

        // We unregistered the last listener. Unregister the service.
        if (gpsListeners.size() == 0 && isListeningForGPS) {
            Log.d("JC", "No more listeners in BikeLocationSerivce, unregistering for upstream locations.");
            //this.androidLocationManager.removeUpdates(this);
            stopLocationUpdates();
            wakeLock.release();
            isListeningForGPS = false;
        }
    }

    public static BikeLocationService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("JC", "BikeLocationService bound.");

        return binder;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Tell all listeners about the new location.
        for (LocationListener l : gpsListeners) {
            l.onLocationChanged(location);
        }

        // Update the local cache
        prevLastValidLocation = lastValidLocation;
        lastValidLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onProviderEnabled(String provider) {
        //locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onProviderDisabled(String provider) {
        //locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public Location getPrevLastValidLocation() {
        return prevLastValidLocation;
    }

    public Location getLastValidLocation() {
        return lastValidLocation;
    }

    public Location getLastKnownLocation() {

        /*Location locGPS = this.androidLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locNetwork = this.androidLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location ret;

        if (locGPS != null && locNetwork != null) {
            if (locGPS.getTime() < locNetwork.getTime()) {
                return locNetwork;
            } else {
                return locGPS;
            }
        } else if (locGPS == null && locNetwork == null) {
            return null;
        } else if (locGPS == null && locNetwork != null) {
            return locNetwork;
        } else {
            return locGPS;
        }*/
        return null;
    }

    public boolean hasValidLocation() {
        return lastValidLocation != null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class BikeLocationServiceBinder extends Binder {
        BikeLocationService getService() {
            return BikeLocationService.this;
        }
    }
}
