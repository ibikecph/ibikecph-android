package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.spoiledmilk.ibikecph.IBikeApplication;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * This task refreshes overlays in the background.
 * Created by kraen on 21-05-16.
 */
public class RefreshOverlaysTask extends AsyncTask<Boolean, Void, Boolean> {

    protected IBikeApplication application;

    TogglableOverlayFactory factory;

    public RefreshOverlaysTask(IBikeApplication application) {
        this.application = application;
        this.factory = TogglableOverlayFactory.getInstance(application);
    }

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
            factory.loadOverlays(forced);
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
        List<TogglableOverlay> togglableOverlay = factory.getTogglableOverlays();
        if (!success) {
            // TODO: Translate the message
            Toast.makeText(application, "Error occurred when loading overlays", Toast.LENGTH_SHORT).show();
        }
        // Notify about the overlays that was actually loaded - if any
        if(togglableOverlay.size() > 0) {
            factory.notifyOnOverlaysLoadedListeners(togglableOverlay);
        }
    }
}
