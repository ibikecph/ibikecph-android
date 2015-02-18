// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.search;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.spoiledmilk.cykelsuperstier.CykelsuperstierApplication;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.util.Util;

public class SearchActivity extends com.spoiledmilk.ibikecph.search.SearchActivity {

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (Util.getScreenWidth() * 9 / 12), Util.dp2px(1));
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_TOP, findViewById(R.id.imgInput).getId());
		params.leftMargin = Util.dp2px(24);
		params.topMargin = Util.dp2px(50);
		findViewById(R.id.lineSplit).setLayoutParams(params);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (CykelsuperstierApplication.isUserLogedIn()) {
			findViewById(R.id.lineShowMoreTopSeparator).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.lineShowMoreTopSeparator).setVisibility(View.INVISIBLE);
		}
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		this.btnStart = menu.getItem(0);
		return ret;
        // Inflate the menu items for use in the action bar
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

}
