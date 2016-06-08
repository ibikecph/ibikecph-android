package dk.kk.ibikecph.map.overlays;

import com.mapbox.mapboxsdk.overlay.Overlay;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.overlays.DownloadedOverlay;
import com.spoiledmilk.ibikecph.util.IBikePreferences;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * The green paths overlay, will show green paths in Copenhagen.
 * Created by kraen on 21-05-16.
 */
public class GreenPathsOverlay extends DownloadedOverlay {

    @Override
    public String getFilename() {
        return "groenne_stier.geojson";
    }
}
