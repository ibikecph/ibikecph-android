package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This task refreshes overlays in the background.
 * Created by kraen on 21-05-16.
 */
public class RefreshOverlaysTask extends AsyncTask<Boolean, Void, Boolean> {

    protected Context context;

    public RefreshOverlaysTask(Context context) {
        this.context = context;
    }

    TogglableOverlayFactory factory = TogglableOverlayFactory.getInstance();

    @Override
    protected Boolean doInBackground(Boolean... params) {
        boolean forced;
        if(params.length == 0) {
            forced = false;
        } else if(params.length == 1) {
            forced = params[0];
        } else {
            throw new RuntimeException("Unexpected arguments, expected a single optional boolean");
        }
        try {
            factory.loadOverlays(context, forced);
            return true;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.e("RefreshOverlaysTask", sw.toString());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            factory.notifyOnOverlaysLoadedListeners(factory.getTogglableOverlays());
        } else {
            // TODO: Translate the message
            Toast.makeText(context, "Error occurred when loading overlays", Toast.LENGTH_SHORT).show();
        }
    }
}
