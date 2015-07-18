package com.spoiledmilk.ibikecph.util.bearing;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Utility class that provides bearing values to true north.
 *
 * This code is lifted from Github: https://raw.githubusercontent.com/meniku/bearing-example
 *
 * It is released under the MIT License:
 *
 * The MIT License (MIT)

 * Copyright (c) 2014 NiKu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class BearingToNorthProvider implements SensorEventListener, LocationListener
{
    public static final String TAG = "BearingToNorthProvider";

    /**
     * Interface definition for a callback to be invoked when the bearing changes.
     */
    public static interface ChangeEventListener {
        /**
         * Callback method to be invoked when the bearing changes.
         * @param bearing the new bearing value
         */
        void onBearingChanged(double bearing);
    }

    private final SensorManager mSensorManager;
    private final LocationManager mLocationManager;
    private final Sensor mSensorAccelerometer;
    private final Sensor mSensorMagneticField;

    // some arrays holding intermediate values read from the sensors, used to calculate our azimuth
    // value

    private float[] mValuesAccelerometer;
    private float[] mValuesMagneticField;
    private float[] mMatrixR;
    private float[] mMatrixI;
    private float[] mMatrixValues;

    /**
     * minimum change of bearing (degrees) to notify the change listener
     */
    private final double mMinDiffForEvent;

    /**
     * minimum delay (millis) between notifications for the change listener
     */
    private final double mThrottleTime;

    /**
     * the change event listener
     */
    private ChangeEventListener mChangeEventListener;

    /**
     * angle to magnetic north
     */
    private AverageAngle mAzimuthRadians;

    /**
     * smoothed angle to magnetic north
     */
    private double mAzimuth = Double.NaN;

    /**
     * angle to true north
     */
    private double mBearing = Double.NaN;

    /**
     * last notified angle to true north
     */
    private double mLastBearing = Double.NaN;

    /**
     * Current GPS/WiFi location
     */
    private Location mLocation;

    /**
     * when we last dispatched the change event
     */
    private long mLastChangeDispatchedAt = -1;

    /**
     * Default constructor.
     *
     * @param context Application Context
     */
    public BearingToNorthProvider(Context context) {
        this(context, 10, 0.5, 50);
    }

    /**
     * @param context Application Context
     * @param smoothing the number of measurements used to calculate a mean for the azimuth. Set
     *                      this to 1 for the smallest delay. Setting it to 5-10 to prevents the
     *                      needle from going crazy
     * @param minDiffForEvent minimum change of bearing (degrees) to notify the change listener
     * @param throttleTime minimum delay (millis) between notifications for the change listener
     */
    public BearingToNorthProvider(Context context, int smoothing, double minDiffForEvent, int throttleTime)
    {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mSensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mValuesAccelerometer = new float[3];
        mValuesMagneticField = new float[3];

        mMatrixR = new float[9];
        mMatrixI = new float[9];
        mMatrixValues = new float[3];

        mMinDiffForEvent = minDiffForEvent;
        mThrottleTime = throttleTime;

        mAzimuthRadians = new AverageAngle(smoothing);
    }

    //==============================================================================================
    // Public API
    //==============================================================================================

    /**
     * Call this method to start bearing updates.
     */
    public void start()
    {
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorMagneticField, SensorManager.SENSOR_DELAY_UI);

        for (final String provider : mLocationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider)
                    || LocationManager.PASSIVE_PROVIDER.equals(provider)
                    || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                if (mLocation == null) {
                    mLocation = mLocationManager.getLastKnownLocation(provider);
                }
                mLocationManager.requestLocationUpdates(provider, 0, 100.0f, this);
            }
        }
    }

    /**
     * call this method to stop bearing updates.
     */
    public void stop()
    {
        mSensorManager.unregisterListener(this, mSensorAccelerometer);
        mSensorManager.unregisterListener(this, mSensorMagneticField);
        mLocationManager.removeUpdates(this);
    }

    /**
     * @return current bearing
     */
    public double getBearing()
    {
        return mBearing;
    }

    /**
     * Returns the bearing event listener to which bearing events must be sent.
     * @return the bearing event listener
     */
    public ChangeEventListener getChangeEventListener()
    {
        return mChangeEventListener;
    }

    /**
     * Specifies the bearing event listener to which bearing events must be sent.
     * @param changeEventListener the bearing event listener
     */
    public void setChangeEventListener(ChangeEventListener changeEventListener)
    {
        this.mChangeEventListener = changeEventListener;
    }

    //==============================================================================================
    // SensorEventListener implementation
    //==============================================================================================

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mValuesAccelerometer, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mValuesMagneticField, 0, 3);
                break;
        }

        boolean success = SensorManager.getRotationMatrix(mMatrixR, mMatrixI,
                mValuesAccelerometer,
                mValuesMagneticField);

        // calculate a new smoothed azimuth value and store to mAzimuth
        if (success) {
            SensorManager.getOrientation(mMatrixR, mMatrixValues);
            mAzimuthRadians.putValue(mMatrixValues[0]);
            mAzimuth = Math.toDegrees(mAzimuthRadians.getAverage());
        }

        // update mBearing
        updateBearing();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {   }

    //==============================================================================================
    // LocationListener implementation
    //==============================================================================================

    @Override
    public void onLocationChanged(Location location)
    {
        // set the new location
        this.mLocation = location;

        // update mBearing
        updateBearing();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {   }

    @Override
    public void onProviderEnabled(String s) {   }

    @Override
    public void onProviderDisabled(String s) {   }

    //==============================================================================================
    // Private Utilities
    //==============================================================================================

    private void updateBearing()
    {
        if (!Double.isNaN(this.mAzimuth)) {
            if(this.mLocation == null) {
                Log.w(TAG, "Location is NULL bearing is not true north!");
                mBearing = mAzimuth;
            } else {
                mBearing = getBearingForLocation(this.mLocation);
            }

            // Throttle dispatching based on mThrottleTime and minDiffForEvent
            if( System.currentTimeMillis() - mLastChangeDispatchedAt > mThrottleTime &&
                    (Double.isNaN(mLastBearing) || Math.abs(mLastBearing - mBearing) >= mMinDiffForEvent)) {
                mLastBearing = mBearing;
                if(mChangeEventListener != null) {
                    mChangeEventListener.onBearingChanged(mBearing);
                }
                mLastChangeDispatchedAt = System.currentTimeMillis();
            }
        }
    }

    private double getBearingForLocation(Location location)
    {
        return mAzimuth + getGeomagneticField(location).getDeclination();
    }

    private GeomagneticField getGeomagneticField(Location location)
    {
        GeomagneticField geomagneticField = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis());
        return geomagneticField;
    }
}