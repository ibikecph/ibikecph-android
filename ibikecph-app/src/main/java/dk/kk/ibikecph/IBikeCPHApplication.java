package dk.kk.ibikecph;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.util.Config;

public class IBikeCPHApplication extends IBikeApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeGoogleAnalytics(R.xml.global_tracker);
        Config.generateUrls(BuildConfig.API_URL);
    }
}
