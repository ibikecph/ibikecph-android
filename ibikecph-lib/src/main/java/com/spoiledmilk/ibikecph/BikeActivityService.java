package com.spoiledmilk.ibikecph;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by jens on 3/6/15.
 */
public class BikeActivityService extends WakefulIntentService {
    private static final int CONFIDENCE_THRESHOLD = 0 ;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public BikeActivityService() {
        super("BikeActivityService");
    }

    /**
     * Intent handler used for the Activity recognition
     * @param intent
     */
    @Override
    protected void doWakefulWork(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            final int confidence = mostProbableActivity.getConfidence();
            final int activityType = mostProbableActivity.getType();
            mostProbableActivity.getVersionCode();

            Log.d("JC", "BikeActivityService: Activity: " + getNameFromType(activityType) + ", confidence: " + confidence);

            // If we trust the reading, send it downstream.
            if (confidence > CONFIDENCE_THRESHOLD) {


                // We're in the context of an Intent, so we need to run the subscription on the main thread.
                Handler mainHandler = new Handler(IBikeApplication.getContext().getMainLooper());

                Runnable broadcastActivityChange = new Runnable() {
                    @Override
                    public void run() {
                        IBikeApplication.getTrackingManager().onActivityChanged(activityType, confidence);
                    }
                };

                mainHandler.post(broadcastActivityChange);

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
}
