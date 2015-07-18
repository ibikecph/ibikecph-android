// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import com.spoiledmilk.ibikecph.BikeLocationService;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.util.LOG;

public class SMLocationManager implements LocationListener {

    static final int UPDATE_INTERVAL = 2000; // the minimum time interval in
                                             // milliseconds for
                                             // notifications from location
                                             // system service

    LocationManager locationManager;
    SMLocationListener listener;

    private static SMLocationManager instance;

    Location prevLastValidLocation;
    Location lastValidLocation;
    boolean locationServicesEnabled;
    private Context context;
    private boolean hasBeenInited = false;


    public Context getContext() {
		return context;
	}

	// Time to wait after the last gps location before using a non-gps location.
    public static final long GPS_WAIT_TIME = 20000; // 20 seconds
    private long lastGps = 0;

    private SMLocationManager() {
        lastValidLocation = null;
        locationServicesEnabled = false;

    }

    public static SMLocationManager getInstance() {
        if (instance == null) {
            instance = new SMLocationManager();
        }
        return instance;
    }

    public void init(Context context, SMLocationListener listener) {
        if (hasBeenInited) return;

    	this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.i("JC", "LocationServicesEnabled = " + locationServicesEnabled);

        IbikeApplication.getService().addGPSListener(this);

        if (locationServicesEnabled) {
            Intent subscriptionIntent = new Intent(getContext(), BikeLocationService.class);
            getContext().startService(subscriptionIntent);

        	this.listener = listener;
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, this);
            try {
                //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, this);
            } catch (Exception e) {
                LOG.e(e.getLocalizedMessage());
            }
        }

        hasBeenInited = true;
    }

    public void removeUpdates() {
        listener = null;
        locationManager.removeUpdates(this);
    }

    public boolean hasValidLocation() {
        Log.e("JC", "FIXME: SMLocationManager.hasValidLocation()");

        return IbikeApplication.getService().hasValidLocation();
    }

    public Location getLastValidLocation() {
        Log.e("JC", "FIXME: SMLocationManager.getLastValidLocation()");

        return IbikeApplication.getService().getLastValidLocation();
    }
    
    public Location getPrevLastValidLocation() {
        Log.e("JC", "FIXME: SMLocationManager.getPrevLastValidLocation()");

        return IbikeApplication.getService().getPrevLastValidLocation();
    }

    public Location getLastKnownLocation() {
        Log.e("JC", "FIXME: SMLocationManager.getLastKnownLocation()");

        return IbikeApplication.getService().getLastKnownLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
		//Log.i("JC", "SMLocationManager new location");
        // Ignore temporary non-gps fix
        if (shouldIgnore(location.getProvider(), System.currentTimeMillis())) {
            LOG.d("SMLocationManager onLocationChanged() location ignored: [" + location.getProvider() + "," + location.getLatitude() + ","
                    + location.getLongitude() + "]");
            return;
        }
        
        prevLastValidLocation = lastValidLocation;
        lastValidLocation = location;

        if (location != null) {
        	//Log.d("JC", "Listener is null: " + (listener == null) );
            if (listener != null) {
            	//Log.d("JC", "SMLocationManager telling downstream");
            	
                listener.onLocationChanged(location);
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onProviderEnabled(String provider) {
        locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean shouldIgnore(final String pProvider, final long pTime) {
        if (lastValidLocation != null) {
            LOG.d("shouldIgnore time diff = " + (lastValidLocation.getTime() - pTime));
        }
        
        if (LocationManager.GPS_PROVIDER.equals(pProvider)) {
            lastGps = pTime;
        } else if (pTime < lastGps + GPS_WAIT_TIME) {
            return true;
        }
        
        return false;
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
