package com.spoiledmilk.ibikecph;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by jens on 5/14/15.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("JC", "Bootreceiver got boot intent");

        boolean trackingEnabled = context.getResources().getBoolean(R.bool.trackingEnabled);
        if (trackingEnabled) {
            context.startService(new Intent(context, BikeLocationService.class));
        }
    }
}
