// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.favorites;

import android.content.Context;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;

import java.util.ArrayList;

public class FavoritesAdapter extends
		com.spoiledmilk.ibikecph.favorites.FavoritesAdapter {

	public FavoritesAdapter(Context context, ArrayList<FavoritesData> objects,
			LeftMenu fragment) {
		super(context, objects, fragment);
	}

	@Override
	protected int getIconResourceId(FavoritesData fd) {
		int ret = R.drawable.fav_star_dark_gray;
		if (fd.getSubSource().equals(FavoritesData.favHome))
			ret = R.drawable.fav_home_dark_gray;
		else if (fd.getSubSource().equals(FavoritesData.favWork))
			ret = R.drawable.fav_work_dark_gray;
		else if (fd.getSubSource().equals(FavoritesData.favSchool))
			ret = R.drawable.fav_school_dark_gray;
		return ret;
	}

	@Override
	protected int getListRowLayout() {
		return R.layout.list_row_favorite;
	}

	@Override
	protected int getPadding(FavoritesData fd) {
		return fd.getPadding();
	}

}
