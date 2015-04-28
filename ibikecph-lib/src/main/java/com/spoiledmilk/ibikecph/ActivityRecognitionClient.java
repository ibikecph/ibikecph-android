package com.spoiledmilk.ibikecph;

import android.app.PendingIntent;
import android.content.Intent;
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
public class ActivityRecognitionClient  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

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
        mGoogleApiClient = new GoogleApiClient.Builder(IbikeApplication.getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void enableTracking() {
        // ensure the google api client is running
        if (mGoogleApiClient == null)
            buildGoogleApiClient();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {

        // Reuse the PendingIntent if we already have it.
        if (this.mActivityDetectionPendingIntent == null) {
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
        requestActivityUpdates();
    }

    public void requestActivityUpdates() {
        if (!tracking) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    10000,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(this);
        }
        this.tracking = true;
    }

    public void releaseActivityUpdates() {
        if (tracking) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());
        }
        this.tracking = false;
    }

    public void onConnectionSuspended(int i) {
        Log.d("JC", "Suspended from the Google API");
        mGoogleApiClient.connect();
    }

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
    public void onResult(Status status) {
    }
}


