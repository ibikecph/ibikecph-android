// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.map.MapActivity;

public class FinishRouteDialog extends DialogFragment {
    private TextView textTitle;
    private Button btnBack;
    private TexturedButton btnStop;
    private TextView textReport;

    public FinishRouteDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = inflater.inflate(R.layout.dialog_stop, container);
        textReport = (TextView) view.findViewById(R.id.textReport);
        textReport.setText(IbikeApplication.getString("ride_report_a_problem"));
        textReport.setTypeface(IbikeApplication.getBoldFont());
        textReport.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ((SMRouteNavigationActivity) getActivity()).launchReportIssuesActivity();
            }

        });

        textTitle = (TextView) view.findViewById(R.id.textTitle);
        textTitle.setText(IbikeApplication.getString("route_stop_title"));
        textTitle.setTypeface(IbikeApplication.getNormalFont());
        btnBack = (Button) view.findViewById(R.id.btnBack);
        btnBack.setText(IbikeApplication.getString("back"));
        btnBack.setTypeface(IbikeApplication.getBoldFont());
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                getDialog().dismiss();
            }
        });
        btnStop = (TexturedButton) view.findViewById(R.id.btnStop);
        btnStop.setTextureResource(R.drawable.btn_pattern_repeteable);
        btnStop.setBackgroundResource(R.drawable.btn_blue_selector);
        btnStop.setText(IbikeApplication.getString("stop_ride"));
        btnStop.setTypeface(IbikeApplication.getBoldFont());
        btnStop.setTextColor(Color.WHITE);
        btnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                getDialog().dismiss();
                Bundle conData = new Bundle();
                conData.putInt("overlaysShown", ((SMRouteNavigationActivity) getActivity()).getOverlaysShown());
                Intent intent = new Intent();
                intent.putExtras(conData);
                getActivity().setResult(MapActivity.RESULT_RETURN_FROM_NAVIGATION, intent);
                getActivity().finish();
                getActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        return view;
    }

}
