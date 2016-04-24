package com.spoiledmilk.ibikecph.map;

import android.support.v4.app.FragmentActivity;

/**
 * Created by jens on 7/10/15.
 */

public abstract class BaseMapActivity extends FragmentActivity {

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
