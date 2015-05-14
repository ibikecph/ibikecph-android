package com.spoiledmilk.ibikecph;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jens on 5/14/15.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("JC", "Bootreceiver got boot intent");
        Toast.makeText(context, "Got boot event", Toast.LENGTH_LONG).show();

        context.startService(new Intent(context, BikeLocationService.class));
    }
}
