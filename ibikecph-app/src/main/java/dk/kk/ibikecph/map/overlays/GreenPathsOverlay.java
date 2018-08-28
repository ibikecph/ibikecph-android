package dk.kk.ibikecph.map.overlays;

import android.util.Log;

import com.spoiledmilk.ibikecph.map.overlays.DownloadedOverlay;

import dk.kk.ibikecph.BuildConfig;

/**
 * The green paths overlay, will show green paths in Copenhagen.
 * Created by kraen on 21-05-16.
 */
public class GreenPathsOverlay extends DownloadedOverlay {

    @Override
    public String getFilename() {
        return "groenne_stier.geojson";
    }

    @Override
    public String getBaseUrl() {
        if(BuildConfig.FLAVOR.equals("local")) {
            return "http://10.0.2.2:3000/geodata/";
        } else {
            return "https://assets.ibikecph.dk/geodata/";
        }
    }
}
