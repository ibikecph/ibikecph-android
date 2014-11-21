// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.favorites;

import android.graphics.Color;

import com.spoiledmilk.cykelsuperstier.R;

public class EditFavoriteFragment extends com.spoiledmilk.ibikecph.favorites.EditFavoriteFragment {

	@Override
	protected int getSelectedTextColor() {
		return getActivity().getResources().getColor(R.color.TextFavoritesDarkGrey);
	}

	@Override
	protected int getUnSelectedTextColor() {
		return Color.GRAY;
	}

}
