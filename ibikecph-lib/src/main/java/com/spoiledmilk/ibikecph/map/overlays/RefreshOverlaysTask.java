package com.spoiledmilk.ibikecph.map.overlays;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * This task refreshes overlays in the background.
 * Created by kraen on 21-05-16.
 */
public class RefreshOverlaysTask extends AsyncTask<Void, Void, Boolean> {

    protected Context context;

    public RefreshOverlaysTask(Context context) {
        this.context = context;
    }

    TogglableOverlayFactory factory = TogglableOverlayFactory.getInstance();

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            factory.loadOverlays(context);
            return true;
        } catch (IOException e) {
            Log.e("RefreshOverlaysTask", e.getMessage());
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
