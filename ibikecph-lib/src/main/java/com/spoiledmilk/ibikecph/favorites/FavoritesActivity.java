// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.search.AddressParser;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.Util;

public class FavoritesActivity extends Activity implements SMLocationListener {

	private TextView textTitle;
	private TextView textAddFavorite;
	private EditText textHome;
	private EditText textWork;
	private Button btnSave;
	private Button btnSkip;
	private TextView textSkip;
	private FavoritesData homeFavorite = null;
	private FavoritesData workFavorite = null;
	private static final int REQUEST_CODE_HOME = 1;
	private static final int REQUEST_CODE_WORK = 2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		chooseLayout();
		textSkip = (TextView) findViewById(R.id.textSkip);
		textTitle = (TextView) findViewById(R.id.textTitle);
		textAddFavorite = (TextView) findViewById(R.id.textAddFavorite);
		textHome = (EditText) findViewById(R.id.textHome);
		textHome.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(FavoritesActivity.this, SearchAutocompleteActivity.class);
				i.putExtra("isA", true);
				startActivityForResult(i, REQUEST_CODE_HOME);
			}

		});

		textWork = (EditText) findViewById(R.id.textWork);
		textWork.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(FavoritesActivity.this, SearchAutocompleteActivity.class);
				i.putExtra("isA", true);
				startActivityForResult(i, REQUEST_CODE_WORK);
			}

		});

		btnSave = (Button) findViewById(R.id.btnSave);
		btnSave.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				DB db = new DB(FavoritesActivity.this);
				if (homeFavorite != null || workFavorite != null) {
					if (homeFavorite != null) {
						db.saveFavorite(homeFavorite, FavoritesActivity.this, true);
					}
					if (workFavorite != null) {
						db.saveFavorite(workFavorite, FavoritesActivity.this, true);
					}
					launchMainMapActivity();
				} else {
					Util.showSimpleMessageDlg(FavoritesActivity.this, IbikeApplication.getString("register_error_fields"));
				}
			}

		});

		btnSkip = (Button) findViewById(R.id.btnSkip);
		btnSkip.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				launchMainMapActivity();
			}

		});

        // Set the ActionBar
        try {
            this.getActionBar().setTitle(IbikeApplication.getString("favorites"));
        } catch(NullPointerException e) {
            // There was no ActionBar. Oh well...
        }
	}

	protected void chooseLayout() {
		if (Util.getScreenHeight() < 800)
			setContentView(R.layout.activity_favorites_ldpi);
		else
			setContentView(R.layout.activity_favorites);
	}

	@Override
	public void onResume() {
		super.onResume();

		SMLocationManager locManager = SMLocationManager.getInstance();
		locManager.init(this, this);

		textTitle.setText(IbikeApplication.getString("favorites_title"));
		textTitle.setTypeface(IbikeApplication.getBoldFont());
		textAddFavorite.setText(IbikeApplication.getString("favorites_text"));
		textAddFavorite.setTypeface(IbikeApplication.getBoldFont());
		textHome.setHint(Html.fromHtml("<i>" + IbikeApplication.getString("favorites_home_placeholder") + "</i>"));
		textHome.setHintTextColor(getResources().getColor(R.color.HintColor));
		textWork.setHint(Html.fromHtml("<i>" + IbikeApplication.getString("favorites_work_placeholder") + "</i>"));
		textWork.setHintTextColor(getResources().getColor(R.color.HintColor));
		btnSave.setText(IbikeApplication.getString("save_favorites"));
		btnSave.setTypeface(IbikeApplication.getBoldFont());
		textSkip.setText(IbikeApplication.getString("skip"));
		textSkip.setTypeface(IbikeApplication.getBoldFont());
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_TOP, btnSkip.getId());
		params.addRule(RelativeLayout.ALIGN_BOTTOM, btnSkip.getId());
		params.addRule(RelativeLayout.ALIGN_LEFT, btnSkip.getId());
		params.leftMargin = (int) (Util.getScreenWidth() / 2 - Util.dp2px(5) * textSkip.getText().toString().length());
		findViewById(R.id.imgSkip).setLayoutParams(params);

	}

	public void launchMainMapActivity() {
		Intent i = new Intent(FavoritesActivity.this, MapActivity.class);
		FavoritesActivity.this.startActivity(i);
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		finish();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET) {
			Bundle data = intent.getExtras();
			if (data != null && data.containsKey("address")) {
				String address = AddressParser.textFromBundle(data);
				if (requestCode == REQUEST_CODE_HOME) {
					homeFavorite = new FavoritesData(IbikeApplication.getString("Home"), address, FavoritesData.favHome,
							data.getDouble("lat"), data.getDouble("lon"), -1);
					textHome.setText(address);
				} else if (requestCode == REQUEST_CODE_WORK) {
					workFavorite = new FavoritesData(IbikeApplication.getString("Work"), address, FavoritesData.favWork,
							data.getDouble("lat"), data.getDouble("lon"), -1);
					textWork.setText(address);
				}
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {

	}

	@Override
	public void onPause() {
		super.onPause();
	}

}
