// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.login;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.cykelsuperstier.map.MapActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.util.DB;

import java.util.ArrayList;

public class LoginSplashActivity extends com.spoiledmilk.ibikecph.login.LoginSplashActivity {

    @Override
    protected Class<?> getMapActivityClass() {
        return MapActivity.class;
    }

}
