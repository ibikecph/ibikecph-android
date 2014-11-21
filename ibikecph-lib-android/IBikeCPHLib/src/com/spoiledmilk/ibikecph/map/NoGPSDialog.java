// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;

/**
 * The dialog that comes up if the GPS reception is off. Spawns the relevant
 * settings dialog.
 * @author jens
 *
 */
public class NoGPSDialog extends DialogFragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_no_gps, container);

		Button btnCancel = (Button) view.findViewById(R.id.btnCancel);
		btnCancel.setTypeface(IbikeApplication.getNormalFont());
		btnCancel.setText(IbikeApplication.getString("leave_no_gps"));
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();

			}
		});

		TexturedButton btnGPS = (TexturedButton) view.findViewById(R.id.btnActivateGps);
		btnGPS.setBackgroundResource(R.drawable.btn_blue_selector);
		btnGPS.setTextureResource(R.drawable.pattern_btn);
		btnGPS.setTypeface(IbikeApplication.getNormalFont());
		btnGPS.setTextColor(Color.WHITE);
		btnGPS.setText(IbikeApplication.getString("activate_gps"));
		btnGPS.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				getDialog().dismiss();
			}
		});

		((TextView) view.findViewById(R.id.textTitle)).setText(IbikeApplication.getString("gps_off"));
		((TextView) view.findViewById(R.id.textTitle)).setTypeface(IbikeApplication.getBoldFont());
		getDialog().getWindow().requestFeature(STYLE_NO_TITLE);

		return view;
	}
}
