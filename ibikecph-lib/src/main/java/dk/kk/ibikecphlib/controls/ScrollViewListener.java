// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.controls;

public interface ScrollViewListener {

	void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy);

}
