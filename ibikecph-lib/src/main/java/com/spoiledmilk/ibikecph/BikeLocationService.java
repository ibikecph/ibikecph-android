package com.spoiledmilk.ibikecph;

import android.app.Activity;
import android.app.PendingIntent;
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
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.*;

import java.util.ArrayList;


/**
 * A Service responsible for keeping track of GPS updates. This is done so that
 * we can still keep track of a navigation context, even if the screen is off.
 * And we can use it for recording tracks when the app is in the background.
 *
 * @author jens
 */
public class BikeLocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
	static final int UPDATE_INTERVAL = 2000;
	private final IBinder binder = new BikeLocationServiceBinder();
	LocationManager androidLocationManager;
	WakeLock wakeLock;

    boolean locationServicesEnabledOnPhone;
    ArrayList<LocationListener> gpsListeners = new ArrayList<LocationListener>();
    boolean isListeningForGPS = false;
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mActivityDetectionPendingIntent;

    /**
     * Instantiates a location manager and an (as yet unused) wake lock.
     */
	public BikeLocationService() {
		Context context = IbikeApplication.getContext();
		this.androidLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		/**
		 * Instantiate a wake lock so we can keep tracking while the phone is off. We're not acquiring it until the
		 * user starts navigation.
		 */
		PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
		this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BikeLocationService");

		Log.i("JC", "BikeLocationService instantiated.");

        buildGoogleApiClient();
	}

    /**
     * Establishes a connection to the Google API, and registers as listener for
     * location updates (including activity recognition updates).
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(IbikeApplication.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        mGoogleApiClient.connect();

        Log.i("JC", "Registered for Google Activity Recognition API");
        Toast.makeText(IbikeApplication.getContext(), "Got Google Activity Recognition API", Toast.LENGTH_LONG).show();
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("JC", "BikeLocationService started");

        // ensure the google api client is running
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }

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
     * Intent handler used for the Activity recognition
     * @param intent
     */
    public void onHandleIntent(Intent intent) {
        Log.i("JC", "BikeLocationService got an intent");

        if (ActivityRecognitionResult.hasResult(intent)) {
            Log.i("JC", "BikeLocationService got activity update");
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();
            mostProbableActivity.getVersionCode();

            Log.d("JC", "BikeLocationService: Activity: " + getNameFromType(activityType) + "confidence: "+confidence);
            Toast.makeText(IbikeApplication.getContext(), getNameFromType(activityType), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Map detected activity types to strings
     * Copied from: http://stackoverflow.com/questions/24818517/activity-recognition-api /jc
     *
     * @param activityType The detected activity type
     * @return A user-readable name for the type
     */
    private String getNameFromType(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.WALKING:
                return "walking";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            default:
                return "unknown";
        }
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

			isListeningForGPS = true;
		}

		// We unregistered the last listener. Unregister the service.
		if (gpsListeners.size() == 0 && isListeningForGPS) {
			Log.d("JC", "No more listeners in BikeLocationSerivce, unregistering for upstream locations.");
			this.androidLocationManager.removeUpdates(this);
			isListeningForGPS = false;
		}
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

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mActivityDetectionPendingIntent != null) {
            return mActivityDetectionPendingIntent;
        }
        Intent intent = new Intent(IbikeApplication.getContext(), BikeLocationService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(IbikeApplication.getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * When connected to the Google Play API
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d("JC", "Connected to the Google API");

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                5000,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);

        Log.d("JC", "Asked for activity recognition updates");
    }



    @Override
    public void onConnectionSuspended(int i) {
        Log.d("JC", "Suspended from the Google API");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("JC", "Failed to connect to the Google API");

    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    @Override
    public void onResult(Status status) {
        if(status.isSuccess()) {
            Log.d("JC", "BikeLocationService.onResult success");

        } else {
            Log.d("JC", "BikeLocationService.onResult fail");
        }
    }

    public class BikeLocationServiceBinder extends Binder {
		BikeLocationService getService() {
			return BikeLocationService.this;
		}
	}
}
