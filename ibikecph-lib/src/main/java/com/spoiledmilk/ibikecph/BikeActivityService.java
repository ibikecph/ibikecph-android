package com.spoiledmilk.ibikecph;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.spoiledmilk.ibikecph.tracking.TrackingManager;

/**
 * Created by jens on 3/6/15.
 */
public class BikeActivityService extends WakefulIntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {


    private static final int CONFIDENCE_THRESHOLD = 0 ;
    private GoogleApiClient mGoogleApiClient;
    private PendingIntent mActivityDetectionPendingIntent;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public BikeActivityService() {
        super("BikeActivityService");
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
    
    public void ensureGoogle() {
        // ensure the google api client is running
        if (mGoogleApiClient == null)
            buildGoogleApiClient();

        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Intent handler used for the Activity recognition
     * @param intent
     */
    @Override
    protected void doWakefulWork(Intent intent) {
        ensureGoogle();

        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();
            mostProbableActivity.getVersionCode();

            Log.d("JC", "BikeActivityService: Activity: " + getNameFromType(activityType) + ", confidence: "+confidence);

            // If we trust the reading, send it downstream.
            if (confidence > CONFIDENCE_THRESHOLD) {
                TrackingManager.getInstance().onActivityChanged(activityType, confidence);
            }
        }
    }

    /**
     * Map detected activity types to strings
     * Copied from: http://stackoverflow.com/questions/24818517/activity-recognition-api /jc
     *
     * @param activityType The detected activity type
     * @return A user-readable name for the type
     */
    private static String getNameFromType(int activityType) {
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
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                0,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
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
    }
}
