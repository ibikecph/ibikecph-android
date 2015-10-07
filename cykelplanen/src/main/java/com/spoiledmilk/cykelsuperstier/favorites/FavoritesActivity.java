// Copyright (C) 2013 The Capital Region of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.cykelsuperstier.favorites;

import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.spoiledmilk.cykelsuperstier.BreakRouteSplashActivity;
import com.spoiledmilk.cykelsuperstier.R;
import com.spoiledmilk.ibikecph.util.Util;

public class FavoritesActivity extends com.spoiledmilk.ibikecph.favorites.FavoritesActivity {

	@Override
	protected void chooseLayout() {
		setContentView(R.layout.activity_favorites);
	}

	@Override
	public void onResume() {
		super.onResume();
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_TOP, findViewById(R.id.btnSave).getId());
		params.addRule(RelativeLayout.ALIGN_BOTTOM, findViewById(R.id.btnSave).getId());
		params.addRule(RelativeLayout.RIGHT_OF, findViewById(R.id.btnSave).getId());
		params.leftMargin = Util.dp2px(20);
		findViewById(R.id.imgSkip).setLayoutParams(params);
		((TextView) findViewById(R.id.textSkip)).setTextColor(Color.rgb(243, 109, 0));
		((TextView) findViewById(R.id.textTitle)).setTextColor(Color.rgb(243, 109, 0));
		((TextView) findViewById(R.id.textAddFavorite)).setTextColor(Color.rgb(116, 116, 116));
		((TextView) findViewById(R.id.textAddFavorite)).setGravity(Gravity.CENTER);

	}

	@Override
	public void launchMainMapActivity() {
		Intent i = new Intent(FavoritesActivity.this, BreakRouteSplashActivity.class);
		FavoritesActivity.this.startActivity(i);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		finish();
	}

}
