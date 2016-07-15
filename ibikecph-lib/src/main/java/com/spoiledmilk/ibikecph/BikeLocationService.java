package com.spoiledmilk.ibikecph;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
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
public class BikeLocationService extends Service implements LocationListener {
    static final int UPDATE_INTERVAL = 2000;
    private final IBinder binder = new BikeLocationServiceBinder();
    WakeLock wakeLock;

    public boolean locationServicesEnabledOnPhone;
    CopyOnWriteArrayList<LocationListener> locationListeners = new CopyOnWriteArrayList<LocationListener>();

    // TODO: Consider if there is a more Android way of achieving a singleton service.
    private static BikeLocationService instance;

    // FusedLocationProvider
    protected GoogleApiClient locationApiClient;
    protected Location lastValidLocation;
    final static long INTERVAL = 5000;
    final static long FASTEST_INTERVAL = 2000;

    public ActivityRecognitionClient getActivityRecognitionClient() {
        return activityRecognitionClient;
    }

    private ActivityRecognitionClient activityRecognitionClient;

    /**
     * Instantiates a location manager and an (as yet unused) wake lock.
     */
    public BikeLocationService() {
        super();
        Log.i("JC", "BikeLocationService instantiated.");
        instance = this;
    }

    protected boolean startLocationUpdates() {
        Log.d("BikeLocationService", "Starting to receive location updates.");
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(locationApiClient, locationRequest, this);
            // Fire a location change event, right away.
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(locationApiClient);
            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }
            return true;
        } catch(SecurityException e) {
            Log.e("BikeLocationService", e.getLocalizedMessage());
            return false;
        }
    }

    protected void stopLocationUpdates() {
        Log.d("BikeLocationService", "Stopping to receive location updates.");
        LocationServices.FusedLocationApi.removeLocationUpdates(locationApiClient, this);
    }

    // End FusedLocationProvider

    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;

        Context context = IBikeApplication.getContext();
        //this.androidLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Instantiate a wake lock so we can keep tracking while the phone is off.
        // We're not acquiring it until the user starts navigation.
        PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
        this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BikeLocationService");

        boolean trackingEnabled = getResources().getBoolean(R.bool.trackingEnabled);
        if (trackingEnabled && activityRecognitionClient == null) {
            Log.d("JC", "Spawning new ActivityRecognitionClient");

            activityRecognitionClient = new ActivityRecognitionClient();
            activityRecognitionClient.connect();
        }

        // Create a Google API client for the location API.
        locationApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    startLocationUpdates();
                }

                @Override
                public void onConnectionSuspended(int i) {
                    stopLocationUpdates();
                }
            })
            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    String msg = "Could not connect to the location API";
                    if(connectionResult.getErrorMessage() != null) {
                        msg += ": " + connectionResult.getErrorMessage();
                    }
                    Log.e("BikeLocationService", msg);
                    Toast.makeText(BikeLocationService.this, msg, Toast.LENGTH_SHORT).show();
                }
            })
            .addApi(LocationServices.API)
            .build();

        Log.d("JC", "BikeLocationService started.");
        return START_STICKY;
    }

    public void addLocationListener(LocationListener listener) {
        // Let's add this to the list of listeners.
        if (!locationListeners.contains(listener)) {
            locationListeners.add(listener);
        }
        onListenersChange();
    }

    public void removeLocationListener(LocationListener listener) {
        locationListeners.remove(listener);
        onListenersChange();
    }


    /**
     * Called whenever a GPS listener is registered or unregistered. Registers or unregisters the
     * service as a GPS listener with the Android operating system, as needed.
     * <p/>
     * This event is always called *after* adding or removing listeners from the list, so we need
     * to take care of two cases. If
     */
    public void onListenersChange() {
        /*
        Log.d("BikeLocationService", "Listeners changed, the listeners are now:");
        for(LocationListener listener: locationListeners) {
            Log.d("BikeLocationService", "\t" + listener);
        }
        */
        // We registered a listener
        // (and we can't be down from 2 because we weren't listening for GPS in the first place)
        if (locationListeners.size() == 1 && !locationApiClient.isConnected()) {
            Log.d("BikeLocationService", "GPS listener added to the BikeLocationService, started listening for locations upstream.");
            // Start listning for locations by connecting to the location API.
            locationApiClient.connect();
            wakeLock.acquire();
        }

        // We unregistered the last listener. Unregister the service.
        if (locationListeners.size() == 0 && locationApiClient.isConnected()) {
            Log.d("BikeLocationService", "No more listeners in BikeLocationService, disconnecting for upstream locations.");
            // Disconnect from the location API.
            locationApiClient.disconnect();
            wakeLock.release();
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
        for (LocationListener l : locationListeners) {
            l.onLocationChanged(location);
        }
        lastValidLocation = location;
    }

    public Location getLastValidLocation() {
        return lastValidLocation;
    }

    public boolean hasValidLocation() {
        return lastValidLocation != null;
    }

    public class BikeLocationServiceBinder extends Binder {
        BikeLocationService getService() {
            return BikeLocationService.this;
        }
    }
}
