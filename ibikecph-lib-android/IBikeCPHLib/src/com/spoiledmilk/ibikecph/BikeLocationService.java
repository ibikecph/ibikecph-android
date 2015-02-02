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

import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.LOG;

/**
 * A Service responsible for keeping track of a navigation context while the 
 * screen is off.
 * 
 * TODO: Think about whether it's a good idea to get the context from the 
 * SMLocationManager.
 * 
 * @author jens
 *
 */
public class BikeLocationService extends Service implements LocationListener {
	static final int UPDATE_INTERVAL = 2000;
	private final IBinder binder = new BikeLocationServiceBinder();
	SMLocationManager smLocationManager;
	LocationManager androidLocationManager;
	WakeLock wakeLock;
    boolean locationServicesEnabled;

	public BikeLocationService() {
		this.smLocationManager = SMLocationManager.getInstance();
		
		this.androidLocationManager = (LocationManager) this.smLocationManager.getContext().getSystemService(Context.LOCATION_SERVICE);
		
		PowerManager pm = (PowerManager) this.smLocationManager.getContext().getSystemService(Service.POWER_SERVICE);
		this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BikeLocationService");
		
		Log.i("com.spoiledmilk.ibikecph", "BikeLocationService instantiated.");
	}

	
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("com.spoiledmilk.ibikecph", "BikeLocationService started");
		
		this.androidLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, 0, this.smLocationManager);
        try {
            this.androidLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, 0, this.smLocationManager);
        } catch (Exception e) {
            LOG.e(e.getLocalizedMessage());
        }
		
		return START_STICKY;
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("com.spoiledmilk.ibikecph", "BikeLocationService bound.");
		
		return binder;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		Log.i("com.spoiledmilk.ibikecph", "BikeLocationService new location");
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	
	public class BikeLocationServiceBinder extends Binder {
		BikeLocationService getService() {
			return BikeLocationService.this;
		}
	}
}
