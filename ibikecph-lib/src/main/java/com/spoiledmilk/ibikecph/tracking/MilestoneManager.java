package com.spoiledmilk.ibikecph.tracking;

import com.spoiledmilk.ibikecph.IbikeApplication;
import io.realm.Realm;

/**
 * Created by jens on 3/13/15.
 */
public class MilestoneManager {

    public void checkForMilestones() {
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
    }

}
