package dk.kk.ibikecph.map.overlays;

import dk.kk.ibikecphlib.map.overlays.DownloadedOverlay;

import dk.kk.ibikecph.BuildConfig;

/**
 * The green paths overlay, will show green paths in Copenhagen.
 * Created by kraen on 21-05-16.
 */
public class HarborRingOverlay extends DownloadedOverlay {

    @Override
    public String getFilename() {
        return "havneringen.geojson";
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
