package dk.kk.ibikecph;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.spoiledmilk.ibikecph.IbikeApplication;

public class IBikeCPHApplication extends IbikeApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
        // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
        gaTracker = analytics.newTracker(R.xml.global_tracker);
    }
}
