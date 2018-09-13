package dk.kk.ibikecph;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecph.map.overlays.GreenPathsOverlay;
import dk.kk.ibikecph.map.overlays.HarborRingOverlay;
import dk.kk.ibikecphlib.map.overlays.TogglableOverlay;
import dk.kk.ibikecphlib.util.Config;

import java.util.List;

public class IBikeCPHApplication extends IBikeApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Config.generateUrls(BuildConfig.BASE_URL);
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
