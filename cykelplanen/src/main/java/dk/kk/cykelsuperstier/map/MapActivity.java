// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package dk.kk.cykelsuperstier.map;

import dk.kk.cykelsuperstier.LeftMenu;

public class MapActivity extends dk.kk.ibikecphlib.map.MapActivity {

    @Override
    protected dk.kk.ibikecphlib.LeftMenu createLeftMenu() {
        return new LeftMenu();
    }

}
