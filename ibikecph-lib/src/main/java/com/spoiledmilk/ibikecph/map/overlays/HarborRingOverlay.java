package com.spoiledmilk.ibikecph.map.overlays;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The green paths overlay, will show green paths in Copenhagen.
 * Created by kraen on 21-05-16.
 */
public class HarborRingOverlay extends DownloadedOverlay {

    @Override
    String getFilename() {
        return "havneringen.geojson";
    }
}
