package dk.kk.ibikecph;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.spoiledmilk.ibikecph.IbikeApplication;

public class IBikeCPHApplication extends IbikeApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        initializeGoogleAnalytics(R.xml.global_tracker);
    }
}
