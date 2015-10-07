package com.spoiledmilk.ibikecph.map;

import android.app.Activity;

/**
 * Created by jens on 7/10/15.
 */

public abstract class IBCMapActivity extends Activity {
    protected boolean problemButtonVisible;

    public void showProblemButton() {
        this.problemButtonVisible = true;
        invalidateOptionsMenu();
    }

    public void hideProblemButton() {
        this.problemButtonVisible = false;
        invalidateOptionsMenu();
    }
}
