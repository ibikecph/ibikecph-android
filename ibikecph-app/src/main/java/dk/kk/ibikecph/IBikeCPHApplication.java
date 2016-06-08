package dk.kk.ibikecph;

import com.spoiledmilk.ibikecph.IBikeApplication;
import dk.kk.ibikecph.map.overlays.GreenPathsOverlay;
import dk.kk.ibikecph.map.overlays.HarborRingOverlay;
import com.spoiledmilk.ibikecph.map.overlays.TogglableOverlay;
import com.spoiledmilk.ibikecph.util.Config;

import java.util.List;

public class IBikeCPHApplication extends IBikeApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeGoogleAnalytics(R.xml.global_tracker);
        Config.generateUrls(BuildConfig.API_URL);
    }

    @Override
    public boolean isDebugging() {
        return BuildConfig.DEBUG;
    }

    @Override
    public List<Class<? extends TogglableOverlay>> getTogglableOverlayClasses() {
        List<Class<? extends TogglableOverlay>> result = super.getTogglableOverlayClasses();
        result.add(GreenPathsOverlay.class);
        result.add(HarborRingOverlay.class);
        return result;
    }
}
