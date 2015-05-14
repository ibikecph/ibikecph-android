package com.spoiledmilk.ibikecph;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.util.ArrayList;


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
	LocationManager androidLocationManager;
	WakeLock wakeLock;

    boolean locationServicesEnabledOnPhone;
    ArrayList<LocationListener> gpsListeners = new ArrayList<LocationListener>();
    boolean isListeningForGPS = false;
    private static BikeLocationService instance;

    public ActivityRecognitionClient getActivityRecognitionClient() {
        return activityRecognitionClient;
    }

    private ActivityRecognitionClient activityRecognitionClient;

    /**
     * Instantiates a location manager and an (as yet unused) wake lock.
     */
	public BikeLocationService( ) {
        super();


		Log.i("JC", "BikeLocationService instantiated.");
	}


	public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;

        Context context = IbikeApplication.getContext();
        this.androidLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

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
		gpsListeners.add(listener);
		onListenersChange();
	}

	public void removeGPSListener(LocationListener listener) {
		gpsListeners.remove(listener);
		onListenersChange();
	}


	/**
	 * Called whenever a GPS listener is registered or unregistered. Registers or unregisters the
	 * service as a GPS listener with the Android operating system, as needed.
	 *
	 * This event is always called *after* adding or removing listeners from the list, so we need
	 * to take care of two cases. If
	 */
	public void onListenersChange() {

		// We registered a listener (and we can't be down from 2 because we weren't listening for
		// GPS in the first place)
		if (gpsListeners.size() == 1 && !isListeningForGPS) {
			Log.d("JC", "GPS listener added to the BikeLocationService, started listening for locations upstream.");

			this.androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, this);
			try {
				this.androidLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, this);
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
			this.androidLocationManager.removeUpdates(this);
            wakeLock.release();
			isListeningForGPS = false;
		}
	}

    public void onCreate(Intent i) {
        instance = this;
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
        Log.d("JC", "BikeLocationService new GPS coord");

		// Tell all listeners about the new location.
		for (LocationListener l : gpsListeners) {
			l.onLocationChanged(location);
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
        locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public void onProviderEnabled(String provider) {
		locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public void onProviderDisabled(String provider) {
		locationServicesEnabledOnPhone = androidLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || androidLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}


    public class BikeLocationServiceBinder extends Binder {
		BikeLocationService getService() {
			return BikeLocationService.this;
		}
	}
}
