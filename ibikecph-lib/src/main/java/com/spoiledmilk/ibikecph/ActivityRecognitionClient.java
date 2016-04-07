package com.spoiledmilk.ibikecph;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;

/**
 * A class for keeping track of the ActivityRecognitionApi subscription. The actual data handling is taken care of by
 * BikeActivityService.
 */
public class ActivityRecognitionClient implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mActivityDetectionPendingIntent;
    private boolean tracking = false;

    public ActivityRecognitionClient() {
        Log.d("JC", "ActivityRecognitionClient instantiated");

    }

    /**
     * Establishes a connection to the Google API, and registers as listener for
     * location updates (including activity recognition updates).
     */
    protected synchronized void buildGoogleApiClient() {
        Log.d("JC", "Building Google API Client");
        mGoogleApiClient = new GoogleApiClient.Builder(IbikeApplication.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void connect() {
        //Log.d("JC", "Attempting to connect to the Activity Recognition Service");

        // ensure the google api client is running
        if (mGoogleApiClient == null)
            buildGoogleApiClient();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            Log.d("JC", "Connecting to the Activity Recognition Service");
            mGoogleApiClient.connect();
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {

        Context context = IbikeApplication.getContext();
        boolean trackingEnabled = context.getResources().getBoolean(R.bool.trackingEnabled);

        // Reuse the PendingIntent if we already have it.
        if (trackingEnabled && this.mActivityDetectionPendingIntent == null) {
            Intent intent = new Intent(IbikeApplication.getContext(), BikeActivityService.class);

            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // requestActivityUpdates() and removeActivityUpdates().
            this.mActivityDetectionPendingIntent =
                    PendingIntent.getService(IbikeApplication.getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return mActivityDetectionPendingIntent;
    }

    /**
     * When connected to the Google Play API
     *
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d("JC", "Connected to Google API");

        if (IbikeApplication.getSettings().getTrackingEnabled()) {
            requestActivityUpdates();
        }
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public void requestActivityUpdates() {
        Log.d("JC", "Requesting activity updates");
        if (!tracking && mGoogleApiClient.isConnected()) {
            Log.d("JC", "requesting!");
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    7500,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(this);
        } else if (!tracking && !mGoogleApiClient.isConnected()) {
            Log.d("JC", "setting apiclient!");
            this.buildGoogleApiClient();
        }
        this.tracking = true;
    }

    public void releaseActivityUpdates() {
        Log.d("JC", "Releasing activity updates");
        if (tracking) {
            try {
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());
            } catch (Exception ex) {
                Log.d("DV", "releaseActivityUpdates-exception = " + ex.getMessage());
            }
        }
        this.tracking = false;
    }

    public void onConnectionSuspended(int i) {
        Log.d("JC", "Suspended from the Google API");
        mGoogleApiClient.connect();
    }

    public void onConnectionFailed(ConnectionResult connectionResult) {
        // http://developer.android.com/reference/com/google/android/gms/common/ConnectionResult.html
        Log.d("JC", "Failed to connect to the Google API: " + connectionResult.toString());

        // Check if the connection failed because of Google Play Services being too old
        if (connectionResult.getErrorCode() == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            showPlayServiceVersionDialog();
        }

        disableTracking();
    }

    public static void showPlayServiceVersionDialog() {
        //Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms"));
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms&hl=en"));
        PendingIntent googlePlayStoreIntent = PendingIntent.getActivity(IbikeApplication.getContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(IbikeApplication.getContext());
        builder.setContentText(IbikeApplication.getString("play_services_version_error")).setContentTitle(IbikeApplication.getAppName());
        builder.setContentIntent(googlePlayStoreIntent);
        builder.setSmallIcon(R.drawable.logo);

        NotificationManager notificationManager = (NotificationManager) IbikeApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(3, builder.build());
    }

    public void disableTracking() {
        this.releaseActivityUpdates();
        IbikeApplication.getSettings().setTrackingEnabled(false);
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
    }
}


