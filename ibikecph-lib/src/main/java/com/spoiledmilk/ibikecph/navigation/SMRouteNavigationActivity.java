// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation;

import java.util.ArrayList;
import java.util.List;

import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.IssuesActivity;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.SplashActivity;
import com.spoiledmilk.ibikecph.controls.InstructionsPagerAdapter;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.util.Config;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * Main navigation view.
 * 
 * TODO: Refactor?
 * @author jens
 *
 */
public class SMRouteNavigationActivity extends FragmentActivity implements View.OnTouchListener, View.OnClickListener {

    public enum InstrcutionViewState {
        Invisible, Minimized, Normal, Maximized;
    }

    private static final int SLIDE_THRESHOLD = 40;
    static int INSTRUCTIONS_TOP_MARGIN;

    public int instructionsViewHeight;
    float lastYMaximized = 0, lastYMinimized = 0, downYMinimized = 0, lastYNormal = 0, downYNormal = 0, posX = 0, touchX = 0;
    int maxSlide = 0, moveCount = 0;
    boolean slidden = false, isMaximized = false, instructionsUpdated = false, isFirstTimeInstructionsMax = true, bicycleTypeChanged = false;
    TranslateAnimation animation, animationInstructions;
    protected SMRouteNavigationMapFragment mapFragment;
    protected RelativeLayout overviewLayout;
    Button btnStart;
    RelativeLayout instructionsView, instructionsViewMax, leftContainer, parentContainer, routeFinishedContainer, reportProblemsView;
    ImageButton pullHandleMax, pullHandle, imageButtonPullHandleMin;
    protected ListView instructionList;
    protected InstructionListAdapter adapter;
    ImageButton btnTrack, btnClose;
    RelativeLayout viewDistance;
    ViewPager viewPager;
    InstructionsPagerAdapter pagerAdapter;
    ImageButton imgClose;
    ImageView imgCargoSlider;
	private TextView textGreen;
    TextView textGoodRide, textReport2, textRecalculating, textCargo, textBicycle, textTime, textReport, textDestAddress;
    ProgressBar progressBar;
    public ArrayList<String> turns;
    RelativeLayout.LayoutParams paramsForInstMaxContainer;
    View mapTopDisabledView, pullTouchNormal, viewPullTouchMaxExtended, mapDisabledView, darkenedView;
    RelativeLayout.LayoutParams paramsInstructionsMaxNormal = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            (int) (Util.getScreenHeight()));
    RelativeLayout.LayoutParams paramsInstructionsMaxMaximized = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            (int) (Util.getScreenHeight() - INSTRUCTIONS_TOP_MARGIN));
    RelativeLayout.LayoutParams paramsInstructionsMaxMinimized = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            (int) (Util.getScreenHeight()));
    InstrcutionViewState instructionsViewState = InstrcutionViewState.Invisible;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.init(getWindowManager());
        this.setContentView(R.layout.route_navigation_activity);
        INSTRUCTIONS_TOP_MARGIN = Util.dp2px(80);
        LayoutInflater inflater = LayoutInflater.from(this);
        reportProblemsView = (RelativeLayout) inflater.inflate(R.layout.report_problems_view, null);
        textReport = (TextView) reportProblemsView.findViewById(R.id.textReport);
        routeFinishedContainer = (RelativeLayout) findViewById(R.id.routeFinishedContainer);
        imgClose = (ImageButton) findViewById(R.id.imgClose);
        imgClose.setOnClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textGoodRide = (TextView) findViewById(R.id.textGoodRide);
        textRecalculating = (TextView) findViewById(R.id.textRecalculating);
        textBicycle = (TextView) findViewById(R.id.textBicycle);
        textCargo = (TextView) findViewById(R.id.textCargo);
		if (Config.GREEN_ROUTES_ENABLED) {
			textGreen = (TextView) findViewById(R.id.textGreen);
		}
        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams((int) (9 * Util.getScreenWidth() / 5),
                FrameLayout.LayoutParams.MATCH_PARENT);
        findViewById(R.id.root_layout).setLayoutParams(rootParams);
        this.maxSlide = (int) (4 * Util.getScreenWidth() / 5);
        Util.init(getWindowManager());
        leftContainer = (RelativeLayout) findViewById(R.id.leftContainer);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth() * 4 / 5,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        leftContainer.setLayoutParams(params);
        parentContainer = (RelativeLayout) findViewById(R.id.parent_container);
        params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth(), RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        parentContainer.setLayoutParams(params);
        imgCargoSlider = (ImageView) findViewById(R.id.imgCargoSlider);
        imgCargoSlider.setOnTouchListener(this);
        darkenedView = findViewById(R.id.darkenedView);
        darkenedView.setBackgroundColor(Color.BLACK);
        viewDistance = (RelativeLayout) findViewById(R.id.viewDistance);
        textTime = (TextView) findViewById(R.id.textTime);
        mapDisabledView = findViewById(R.id.mapDisabledView);
        mapDisabledView.setOnTouchListener(this);
        overviewLayout = (RelativeLayout) findViewById(R.id.overviewLayout);
        btnStart = (Button) overviewLayout.findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);
        btnClose = ((ImageButton) findViewById(R.id.btnClose));
        btnClose.setOnClickListener(this);
        btnClose.setOnTouchListener(this);
		if (Config.EXTENDED_PULL_TOUCH) {
			// increased touch area for the normal pull handle
			pullTouchNormal = findViewById(R.id.viewPullTouchNormalExtended);
			pullTouchNormal.setOnTouchListener(this);
			// increased touch area for the max pull handle
			viewPullTouchMaxExtended = findViewById(R.id.viewPullTouchMaxExtended);
			viewPullTouchMaxExtended.setOnTouchListener(this);
		}
        mapTopDisabledView = findViewById(R.id.mapTopDisabledView);
        instructionsView = (RelativeLayout) findViewById(R.id.instructionsView);
        instructionsView.setBackgroundColor(Color.BLACK);
        paramsInstructionsMaxNormal.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsInstructionsMaxNormal.addRule(RelativeLayout.ALIGN_TOP, instructionsView.getId());
        paramsInstructionsMaxNormal.bottomMargin = -(int) ((Util.getScreenHeight()));
        paramsInstructionsMaxMaximized.topMargin = INSTRUCTIONS_TOP_MARGIN;
        paramsInstructionsMaxMinimized.topMargin = (int) (Util.getScreenHeight() - Util.getScreenHeight() / 10);
        paramsInstructionsMaxMinimized.bottomMargin = -(int) ((Util.getScreenHeight()));
        pullHandle = (ImageButton) instructionsView.findViewById(R.id.imgPullHandle);
        pullHandle.setOnTouchListener(this);
        imageButtonPullHandleMin = (ImageButton) findViewById(R.id.imageButtonPullHandleMin);
        instructionsViewMax = (RelativeLayout) findViewById(R.id.instructionsViewMax);
        params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth(), RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        instructionsView.setLayoutParams(params);
        params = new RelativeLayout.LayoutParams((int) Util.getScreenWidth(), RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        findViewById(R.id.overviewLayout).setLayoutParams(params);
        pullHandleMax = (ImageButton) instructionsViewMax.findViewById(R.id.imgPullHandleMax);
        pullHandleMax.setOnTouchListener(this);
        instructionList = (ListView) instructionsViewMax.findViewById(R.id.listView);
        instructionList.addFooterView(reportProblemsView);
        setInstructionViewState(InstrcutionViewState.Invisible);
        FragmentManager fm = this.getFragmentManager();
        mapFragment = getMapFragment();
        if (savedInstanceState == null) {
            fm.beginTransaction().add(R.id.map_container, mapFragment).commit();
        } else {
            fm.beginTransaction().replace(R.id.map_container, mapFragment).commit();
        }
        viewPager = (ViewPager) instructionsView.findViewById(R.id.viewPager);
        viewPager.setOnPageChangeListener(new PagerListener());
        pagerAdapter = getPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnTouchListener(this);
        btnTrack = (ImageButton) findViewById(R.id.btnTrack);
        btnTrack.setOnClickListener(this);
        textReport.setOnClickListener(this);
        textReport2 = (TextView) findViewById(R.id.textReport2);
        textReport2.setOnClickListener(this);
        textDestAddress = (TextView) findViewById(R.id.textDestAddress);
        textDestAddress.setTypeface(IbikeApplication.getNormalFont());
        Config.OSRM_SERVER = Config.OSRM_SERVER_FAST;
        instructionsViewMax.setLayoutParams(paramsInstructionsMaxNormal);
        RelativeLayout.LayoutParams paramsBtnTrack = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsBtnTrack.setMargins(Util.dp2px(10), Util.dp2px(10), Util.dp2px(10), Util.dp2px(10));
        paramsBtnTrack.alignWithParent = true;
        paramsBtnTrack.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        paramsBtnTrack.addRule(RelativeLayout.ABOVE, overviewLayout.getId());
        btnTrack.setLayoutParams(paramsBtnTrack);
        instructionsView.measure(0, 0);
        instructionsViewHeight = instructionsView.getMeasuredHeight();
        turns = new ArrayList<String>();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    long lastUpTimestamp = 0, timestamp;
    float y;
    int yRaw;


    private boolean onPullHandleTouch(View arg0, MotionEvent event) {
        animateInstructions(event);
        return true;
    }

    public void onBicycleContainerClick(View v) {
    	turns.clear();
		bicycleTypeChanged = true;
		mapFragment.locationOverlay.disableMyLocation();
		
		v.setBackgroundResource(R.color.BlueListBackground);
		((ImageView) findViewById(R.id.imgBicycle)).setImageResource(R.drawable.fast_white);
		textBicycle.setTextColor(getResources().getColor(R.color.White));
		
		setCargoContainerInactive();
		if (Config.GREEN_ROUTES_ENABLED) {
			setGreenContainerInactive();
		}
		
		Config.OSRM_SERVER = Config.OSRM_SERVER_FAST;
		getRouteForNewBicycleType();
	}

	public void onCargoContainerClick(View v) {
		turns.clear();
		bicycleTypeChanged = true;
		mapFragment.locationOverlay.disableMyLocation();
		
		v.setBackgroundResource(R.color.BlueListBackground);
		((ImageView) findViewById(R.id.imgCargo)).setImageResource(R.drawable.cargo_white);
		textCargo.setTextColor(getResources().getColor(R.color.White));
		
		setBicycleContainerInactive();
		if (Config.GREEN_ROUTES_ENABLED) {
			setGreenContainerInactive();
		}
		
		Config.OSRM_SERVER = Config.OSRM_SERVER_CARGO;
		getRouteForNewBicycleType();
	}
	
	public void onGreenContainerClick(View v) {
		turns.clear();
		bicycleTypeChanged = true;
		mapFragment.locationOverlay.disableMyLocation();
		
		v.setBackgroundResource(R.color.BlueListBackground);
		((ImageView) findViewById(R.id.imgGreen)).setImageResource(R.drawable.green_white);
		((TextView) v.findViewById(R.id.textGreen)).setTextColor(getResources().getColor(R.color.White));
		
		setBicycleContainerInactive();
		setCargoContainerInactive();
		
		Config.OSRM_SERVER = Config.OSRM_SERVER_GREEN;
		getRouteForNewBicycleType();
	}

	private void setBicycleContainerInactive() {
		findViewById(R.id.bicycleContainer).setBackgroundResource(R.color.LeftGreyBackground);
		((ImageView) findViewById(R.id.imgBicycle)).setImageResource(R.drawable.fast_grey);
		textBicycle.setTextColor(getResources().getColor(R.color.TextLightGrey));
	}
	
	private void setCargoContainerInactive() {
		findViewById(R.id.cargoContainer).setBackgroundResource(R.color.LeftGreyBackground);
		((ImageView) findViewById(R.id.imgCargo)).setImageResource(R.drawable.cargo_grey);
		textCargo.setTextColor(getResources().getColor(R.color.TextLightGrey));
	}
	
	private void setGreenContainerInactive() {
		findViewById(R.id.greenContainer).setBackgroundResource(R.color.LeftGreyBackground);
		((ImageView) findViewById(R.id.imgGreen)).setImageResource(R.drawable.green_grey);
		textGreen.setTextColor(getResources().getColor(R.color.TextLightGrey));
	}

    protected boolean hasDarkImage() {
        return true;
    }

    private void getRouteForNewBicycleType() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        stopTrackingUser();
        mapFragment.routingStarted = false;
        mapFragment.getRouteForNewBicycleType();

    }

    public void onNewBicycleRoute() {
        translate(-maxSlide, true);
        setInstructionViewState(InstrcutionViewState.Invisible);
        overviewLayout.setVisibility(View.VISIBLE);
        btnStart.setEnabled(true);
        mapFragment.zoomToBoundingBox();
        bicycleTypeChanged = false;
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    public void showRouteOverview() {
        stopTrackingUser();
        mapFragment.routingStarted = false;
        translate(-maxSlide, true);
        setInstructionViewState(InstrcutionViewState.Invisible);
        overviewLayout.setVisibility(View.VISIBLE);
        btnStart.setEnabled(true);
        textTime.setText(mapFragment.getEstimatedArrivalTime());
    }

    protected InstructionListAdapter getInstructionsAdapter() {
        if (adapter == null) {
            adapter = new InstructionListAdapter(this, R.layout.direction_top_cell, mapFragment.route);
        }
        return adapter;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = getInstructionsAdapter();
        instructionList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        textTime.setText(mapFragment.getEstimatedArrivalTime());
        textTime.setTypeface(IbikeApplication.getBoldFont());
        textReport.setText(IbikeApplication.getString("ride_report_a_problem"));
        textReport.setTypeface(IbikeApplication.getBoldFont());
        textReport2.setText(IbikeApplication.getString("ride_report_a_problem"));
        textReport2.setTypeface(IbikeApplication.getBoldFont());
        textCargo.setText(IbikeApplication.getString("bike_type_2"));
        textCargo.setTextColor(getResources().getColor(R.color.TextLightGrey));
        textCargo.setTypeface(IbikeApplication.getNormalFont());
		if (Config.GREEN_ROUTES_ENABLED) {
			textGreen.setText(IbikeApplication.getString("bike_type_3"));
			textGreen.setTextColor(getResources().getColor(
					R.color.TextLightGrey));
			textGreen.setTypeface(IbikeApplication.getNormalFont());
		}
        textBicycle.setText(IbikeApplication.getString("bike_type_1"));
        textBicycle.setTypeface(IbikeApplication.getNormalFont());
        textGoodRide.setText(IbikeApplication.getString("good_ride"));
        textGoodRide.setTypeface(IbikeApplication.getBoldFont());
        textRecalculating.setVisibility(View.GONE);
        textRecalculating.setTypeface(IbikeApplication.getNormalFont());
        textRecalculating.setText(IbikeApplication.getString("calculating_new_route"));
        turns = new ArrayList<String>();
        btnStart.setText(IbikeApplication.getString("start_route"));
        String st = "Start: " + IbikeApplication.getString("current_position") + " (" + mapFragment.startLocation.getLatitude() + ","
                + mapFragment.startLocation.getLongitude() + ") End: " + mapFragment.destination + " (" + mapFragment.endLocation.getLatitude() + ","
                + mapFragment.endLocation.getLongitude() + ")";
        IbikeApplication.getTracker().sendEvent("Route", "Resume", st, (long) 0);
    }

    public void generateTurnStrings() {
        turns.clear();
        for (int i = 0; i < mapFragment.route.allTurnInstructions.size(); i++) {
            turns.add(mapFragment.route.allTurnInstructions.get(i).generateFullDescriptionString());
        }
        for (int i = 0; i < mapFragment.route.turnInstructions.size(); i++) {
            turns.add(mapFragment.route.turnInstructions.get(i).generateFullDescriptionString());
        }
    }

    public void showRouteFinishedDialog() {
        if (mapFragment.mapView.directionShown) {
            mapFragment.switchTracking();
        }
        btnTrack.setImageResource(mapFragment.getLastIcon());
        instructionsViewMax.setVisibility(View.GONE);
        instructionsView.setVisibility(View.GONE);
        textDestAddress.setText(mapFragment.destination);
        routeFinishedContainer.setVisibility(View.VISIBLE);
        darkenedView.setVisibility(View.GONE);
        IbikeApplication.getTracker().sendEvent("Route", "Overview", mapFragment.destination, (long) 0);
        btnClose.setVisibility(View.GONE);
        viewDistance.setVisibility(View.GONE);
        imgCargoSlider.setVisibility(View.GONE);
    }

    protected int getListItemHeight() {
        return Util.dp2px(64);
    }

    @Override
    public void onPause() {
        super.onPause();
        instructionList.setAdapter(null);
		if (Config.EXTENDED_PULL_TOUCH) {
			findViewById(R.id.viewListDarkOverlay).setVisibility(View.GONE);
		}
        findViewById(R.id.normalDarkOverlay).setVisibility(View.GONE);
        findViewById(R.id.normalProgressBar).setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    public void setInstructionViewState(InstrcutionViewState newState) {
        instructionList.smoothScrollToPosition(0);
        if (newState == InstrcutionViewState.Maximized) {
            instructionList.setVerticalScrollBarEnabled(true);
			if (Config.EXTENDED_PULL_TOUCH) {
				viewPullTouchMaxExtended.setVisibility(View.VISIBLE);
				pullTouchNormal.setVisibility(View.GONE);
			}
            mapTopDisabledView.setVisibility(View.VISIBLE);
            instructionsViewMax.setLayoutParams(paramsInstructionsMaxMaximized);
            instructionsView.setVisibility(View.VISIBLE);
            instructionsViewMax.setVisibility(View.VISIBLE);
            if (hasDarkImage()) {
                btnClose.setImageResource(R.drawable.btn_close_dark_selector);
                viewDistance.setBackgroundResource(R.drawable.distance_black);
                textTime.setTextColor(Color.WHITE);
            }
            darkenedView.getBackground().setAlpha(200);
        } else if (newState == InstrcutionViewState.Normal) {
			if (Config.EXTENDED_PULL_TOUCH) {
				viewPullTouchMaxExtended.setVisibility(View.GONE);
				pullTouchNormal.setVisibility(View.VISIBLE);
			}
            mapTopDisabledView.setVisibility(View.GONE);
            instructionsViewMax.setLayoutParams(paramsInstructionsMaxNormal);
            instructionsView.setVisibility(View.VISIBLE);
            instructionsViewMax.setVisibility(View.GONE);
            darkenedView.setVisibility(View.GONE);
            if (hasDarkImage()) {
                btnClose.setImageResource(R.drawable.btn_close_selector);
                viewDistance.setBackgroundResource(R.drawable.distance_white);
                textTime.setTextColor(Color.BLACK);
            }
            RelativeLayout.LayoutParams paramsBtnTrack = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            paramsBtnTrack.setMargins(Util.dp2px(10), Util.dp2px(10), Util.dp2px(10), Util.dp2px(10));
            paramsBtnTrack.alignWithParent = true;
            paramsBtnTrack.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            paramsBtnTrack.addRule(RelativeLayout.ABOVE, instructionsView.getId());
            btnTrack.setLayoutParams(paramsBtnTrack);
        } else if (newState == InstrcutionViewState.Minimized) {
			if (Config.EXTENDED_PULL_TOUCH) {
				viewPullTouchMaxExtended.setVisibility(View.GONE);
				pullTouchNormal.setVisibility(View.GONE);
				 imageButtonPullHandleMin.setVisibility(View.VISIBLE);
			}
            mapTopDisabledView.setVisibility(View.GONE);
            instructionsViewMax.setLayoutParams(paramsInstructionsMaxMinimized);
            darkenedView.setVisibility(View.GONE);
            if (hasDarkImage()) {
                btnClose.setImageResource(R.drawable.btn_close_selector);
                viewDistance.setBackgroundResource(R.drawable.distance_white);
                textTime.setTextColor(Color.BLACK);
            }
            instructionsView.setVisibility(View.GONE);
            instructionsViewMax.setVisibility(View.GONE);
           
            RelativeLayout.LayoutParams paramsBtnTrack = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            paramsBtnTrack.setMargins(Util.dp2px(10), Util.dp2px(10), Util.dp2px(10), Util.dp2px(10));
            paramsBtnTrack.alignWithParent = true;
            paramsBtnTrack.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            paramsBtnTrack.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            btnTrack.setLayoutParams(paramsBtnTrack);
        } else if (newState == InstrcutionViewState.Invisible) {
			if (Config.EXTENDED_PULL_TOUCH) {
				viewPullTouchMaxExtended.setVisibility(View.GONE);
				pullTouchNormal.setVisibility(View.GONE);
				imageButtonPullHandleMin.setVisibility(View.GONE);
			}
            mapTopDisabledView.setVisibility(View.GONE);
            instructionsViewMax.setLayoutParams(paramsInstructionsMaxNormal);
            darkenedView.setVisibility(View.GONE);
            if (hasDarkImage()) {
                btnClose.setImageResource(R.drawable.btn_close_selector);
                viewDistance.setBackgroundResource(R.drawable.distance_white);
                textTime.setTextColor(Color.BLACK);
            }
            instructionsView.setVisibility(View.GONE);
            instructionsViewMax.setVisibility(View.GONE);
           
        }
        instructionsViewState = newState;
    }

    public void hideOverview() {
        overviewLayout.setVisibility(View.GONE);
    }

    public void startTrackingUser() {
        mapFragment.setTrackingMode(true);
        btnTrack.setImageResource(mapFragment.getLastIcon());
    }

    private void changeTrackingIcon() {
        btnTrack.setImageResource(mapFragment.getLastIcon());
    }

    public void stopTrackingUser() {
        if (mapFragment != null && mapFragment.mapView != null) {
            if (!mapFragment.mapView.isPinchZooming) {
                mapFragment.setTrackingMode(false);
                btnTrack.setImageResource(R.drawable.icon_locate_no_tracking);
            }
        }
    }

    protected void showStopDlg() {
        FragmentManager fm = getFragmentManager();
        FinishRouteDialog roteFinishDialog = new FinishRouteDialog();
        roteFinishDialog.show(fm, "dialog_stop");
    }

    public void setOverview(String destination, String distance, String viaStreets) {
        ((TextView) overviewLayout.findViewById(R.id.overviewDestination)).setTypeface(IbikeApplication.getBoldFont());
        ((TextView) overviewLayout.findViewById(R.id.overviewDestination)).setText(destination);
        String distanceAndVia = distance + ", " + IbikeApplication.getString("via") + " " + viaStreets;
        ((TextView) overviewLayout.findViewById(R.id.overviewDistanceAndVia)).setText(distanceAndVia);
        EasyTracker.getInstance().setContext(this);
        IbikeApplication.getTracker().sendEvent("Route", "Overview", destination, (long) 0);
    }

    @Override
    public void onBackPressed() {
        showStopDlg();
    }

    public void reloadInstructions(List<SMTurnInstruction> turnInstructions, boolean isRecalc) {
        instructionsUpdated = true;
        if (turnInstructions.size() > 0) {
            LOG.d("instructions updated");
            pagerAdapter.setInstructionsUpdated(true);
            viewPager.getAdapter().startUpdate(viewPager);
            viewPager.getAdapter().notifyDataSetChanged();
            if (isRecalc) {
                viewPager.setCurrentItem(0);
            }
            viewPager.getAdapter().finishUpdate(viewPager);
            adapter = getInstructionsAdapter();
            adapter.setRoute(mapFragment.route);
            adapter.notifyDataSetChanged();
            textTime.setText(mapFragment.getEstimatedArrivalTime());

        } else {
            setInstructionViewState(InstrcutionViewState.Invisible);
        }
        pagerAdapter.setInstructionsUpdated(false);
        instructionsUpdated = false;
        if (turns == null) {
            turns = new ArrayList<String>();
        } else {
            turns.clear();
        }
        for (int i = 0; i < turnInstructions.size(); i++) {
            turns.add(turnInstructions.get(i).fullDescriptionString);
        }
    }

    public void showProgressBar() {
        if (instructionsViewState == InstrcutionViewState.Maximized) {
			if (Config.EXTENDED_PULL_TOUCH) {
				findViewById(R.id.viewListDarkOverlay).setVisibility(
						View.VISIBLE);
			}
            progressBar.setVisibility(View.VISIBLE);
            textRecalculating.setVisibility(View.VISIBLE);
        } else if (instructionsViewState == InstrcutionViewState.Normal) {
            findViewById(R.id.normalDarkOverlay).setVisibility(View.VISIBLE);
            findViewById(R.id.normalProgressBar).setVisibility(View.VISIBLE);
        }
    }

    public void hideProgressBar() {
		if (Config.EXTENDED_PULL_TOUCH) {
			findViewById(R.id.viewListDarkOverlay).setVisibility(View.GONE);
		}
        findViewById(R.id.normalDarkOverlay).setVisibility(View.GONE);
        findViewById(R.id.normalProgressBar).setVisibility(View.GONE);
        textRecalculating.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    // ***** Translate animation for the cargo/bicycle container *****

    private void translate(float deltaX, final boolean finalAnim) {
        // mapFragment.mapView.setEnabled(false);
        float newX = posX + deltaX;
        if (slidden) {
            if (newX < -maxSlide)
                newX = -maxSlide;
            else if (newX > 0)
                newX = 0;
        } else {
            if (newX < 0)
                newX = 0;
            else if (newX > maxSlide)
                newX = maxSlide;
        }

        if (((int) newX) <= 0) {
            mapDisabledView.setVisibility(View.GONE);
        }

        final boolean newSlidden = slidden ? newX > -SLIDE_THRESHOLD : newX > SLIDE_THRESHOLD;
        if (finalAnim) {
            newX = (slidden == newSlidden) ? 0 : (slidden ? -maxSlide : maxSlide);
        }

        if (animation != null && animation.isInitialized()) {
            animation.cancel();
            parentContainer.clearAnimation();
            leftContainer.invalidate();
        }
        animation = new TranslateAnimation(posX, newX, 0, 0);
        animation.setDuration(finalAnim ? 100 : 0);

        animation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!finalAnim) {
                    animation.setFillEnabled(true);
                    animation.setFillAfter(true);
                } else {
                    parentContainer.clearAnimation();
                    if (slidden == newSlidden) {
                        if (!slidden) {
                            leftContainer.setVisibility(View.GONE);
                            mapDisabledView.setVisibility(View.GONE);
                            leftContainer.invalidate();
                        } else {
                            leftContainer.setVisibility(View.VISIBLE);
                            mapDisabledView.setVisibility(View.VISIBLE);
                            leftContainer.invalidate();
                        }
                        return;
                    }
                    slidden = newSlidden;
                    int leftmargin = slidden ? maxSlide : 0;
                    int rightMargin = slidden ? 0 : maxSlide;
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) parentContainer.getLayoutParams();
                    lp.setMargins(leftmargin, lp.topMargin, rightMargin, lp.bottomMargin);
                    parentContainer.setLayoutParams(lp);

                    if (leftmargin == 0) {
                        leftContainer.setVisibility(View.GONE);
                        mapDisabledView.setVisibility(View.GONE);
                        leftContainer.invalidate();
                    }
                    posX = 0;
                }
            }
        });

        posX = newX;

        parentContainer.startAnimation(animation);
    }

    // ***** Translate animation for the maximized instructions *****
    float lastY;
    long lastDownTimestamp = 0;
    boolean isDoubleTap = false;
    static final int DOUBLE_TAP_PERIOD = 500;

    private void animateInstructions(MotionEvent event) {
        if (animationInstructions != null && animationInstructions.isInitialized()) {
            animationInstructions.cancel();
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            instructionList.setVerticalScrollBarEnabled(false);
            if (System.currentTimeMillis() - lastDownTimestamp <= DOUBLE_TAP_PERIOD) {
                isDoubleTap = true;
                lastDownTimestamp = System.currentTimeMillis();
            } else {
                lastDownTimestamp = 0;
            }
            lastDownTimestamp = System.currentTimeMillis();
            mapFragment.mapView.setNoRendering(true);
            lastY = event.getY();
            instructionsView.setVisibility(View.INVISIBLE);
			if (Config.EXTENDED_PULL_TOUCH) {
				imageButtonPullHandleMin.setVisibility(View.INVISIBLE);
			}
            instructionsViewMax.setVisibility(View.VISIBLE);
            darkenedView.setVisibility(View.VISIBLE);
            darkenedView.getBackground().setAlpha(Util.yToAlpha((int) event.getRawY()));
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mapFragment.mapView.setNoRendering(true);
            if (isDoubleTap && System.currentTimeMillis() - lastDownTimestamp <= DOUBLE_TAP_PERIOD) {
                if (instructionsViewState == InstrcutionViewState.Normal) {
                    setInstructionViewState(InstrcutionViewState.Maximized);
                } else if (instructionsViewState == InstrcutionViewState.Maximized) {
                    setInstructionViewState(InstrcutionViewState.Normal);
                } else if (instructionsViewState == InstrcutionViewState.Minimized) {
                    setInstructionViewState(InstrcutionViewState.Normal);
                }
            } else if (event.getRawY() < Util.getScreenHeight() / 2) {
                setInstructionViewState(InstrcutionViewState.Maximized);
            } else if (event.getRawY() < 9 * Util.getScreenHeight() / 10) {
                instructionList.smoothScrollToPosition(0);
                setInstructionViewState(InstrcutionViewState.Normal);
            } else {
                instructionList.smoothScrollToPosition(0);
                setInstructionViewState(InstrcutionViewState.Minimized);
            }
            instructionsViewMax.clearAnimation();
            isDoubleTap = false;
            return;

        }
        if (event.getRawY() > Util.getScreenHeight() - Util.dp2px(40)) {
            pullHandleMax.setBackgroundColor(Color.TRANSPARENT);
        } else {
            pullHandleMax.setBackgroundColor(getPullHandeBackground());
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            darkenedView.getBackground().setAlpha(Util.yToAlpha((int) event.getRawY()));
            animationInstructions = new TranslateAnimation(0, 0, lastY, event.getY());
            animationInstructions.setFillAfter(true);
            animationInstructions.setFillBefore(true);
            lastY = event.getY();
            animationInstructions.setDuration(0);
            instructionsViewMax.startAnimation(animationInstructions);
        }

    }

    protected int getPullHandeBackground() {
        return Color.rgb(26, 26, 26);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        mapFragment.mapView.getTileProvider().clearTileCache();
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    public int getOverlaysShown() {
        return 0;
    }

    public boolean onSliderTouch(View v, MotionEvent event) {
        leftContainer.setVisibility(View.VISIBLE);
        mapDisabledView.setVisibility(View.VISIBLE);

        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // snap
                v.setPressed(false);
                if (moveCount <= 3)
                    translate(slidden ? -maxSlide : maxSlide, true);
                else
                    translate(0, true);
                break;
            case MotionEvent.ACTION_DOWN:
                moveCount = 0;
                v.setPressed(true);
                touchX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (moveCount++ < 3)
                    break;
                float newTouchX = event.getX();
                float delta = newTouchX - touchX;
                translate(delta, false);
                touchX = newTouchX;
                break;
        }

        if (slidden)
            mapDisabledView.setVisibility(View.GONE);
        return true;
    }

    public void updateTime(String estimatedArrivalTime) {
        textTime.setText(estimatedArrivalTime);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean ret = false;
        if (v.getId() == R.id.imgCargoSlider) {
            ret = onSliderTouch(v, event);
        } else if (v.getId() == R.id.mapDisabledView) {
            // used to disable the map touching when sliden
            return onSliderTouch(v, event);
        } else if (v.getId() == R.id.btnClose) {
            // darken the button on touch
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                btnClose.setColorFilter(Color.argb(150, 155, 155, 155));
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                btnClose.setColorFilter(Color.argb(0, 155, 155, 155));
                return false;
            }
            return false;
        } else if (v.getId() == R.id.viewPullTouchMaxExtended) {
            return onPullHandleTouch(null, event);
        } else if (v.getId() == R.id.viewPullTouchNormalExtended) {
            return onPullHandleTouch(null, event);
        } else if (v.getId() == R.id.viewPager) {
            // used to disable viewPager swiping when the left menu is
            // opened
            return slidden;
        } else if (v.getId() == R.id.imgPullHandle) {
            return onPullHandleTouch(null, event);
        } else if (v.getId() == R.id.imgPullHandleMax) {
            return onPullHandleTouch(v, event);
        } else if (v.getId() == R.id.imageButtonPullHandleMin) {
            return onPullHandleTouch(v, event);
        }
        return ret;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imgClose) {
            onCloseClick();
        } else if (v.getId() == R.id.btnStart) {
            onStartClick();
        } else if (v.getId() == R.id.btnClose) {
            showStopDlg();
        } else if (v.getId() == R.id.btnTrack) {
            if (!mapFragment.getTrackingMode()) {
                startTrackingUser();
            } else {
                mapFragment.switchTracking();
                changeTrackingIcon();
            }
        } else if (v.getId() == R.id.textReport || v.getId() == R.id.textReport2) {
            launchReportIssuesActivity();
        }
    }

    private void onCloseClick() {
        Bundle conData = new Bundle();
        conData.putInt("overlaysShown", getOverlaysShown());
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(MapActivity.RESULT_RETURN_FROM_NAVIGATION, intent);
        setResult(MapActivity.RESULT_RETURN_FROM_NAVIGATION);
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void onStartClick() {
        viewDistance.setVisibility(View.VISIBLE);
        btnStart.setEnabled(false);
        hideOverview();
        textTime.setText(mapFragment.getEstimatedArrivalTime());
        mapFragment.startRouting();
        IbikeApplication.getTracker().sendEvent("Route", "Overview", mapFragment.destination, (long) 0);
        setInstructionViewState(InstrcutionViewState.Normal);
        RelativeLayout.LayoutParams paramsBtnTrack = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsBtnTrack.setMargins(Util.dp2px(10), Util.dp2px(10), Util.dp2px(10), Util.dp2px(10));
        paramsBtnTrack.alignWithParent = true;
        paramsBtnTrack.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        paramsBtnTrack.addRule(RelativeLayout.ABOVE, instructionsView.getId());
        btnTrack.setLayoutParams(paramsBtnTrack);
        startTrackingUser();
    }

    public void launchReportIssuesActivity() {
        generateTurnStrings();
        Intent i = new Intent(SMRouteNavigationActivity.this, IssuesActivity.class);
        Bundle b = new Bundle();
        b.putStringArrayList("turns", turns);
        b.putString("startLoc", mapFragment.startLocation.toString());
        b.putString("endLoc", mapFragment.endLocation.toString());
        String source = mapFragment.source;
        if (source == null || source.equals("")) {
            source = IbikeApplication.getString("current_position");
        }
        b.putString("startName", source);
        b.putString("endName", mapFragment.destination);
        i.putExtras(b);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    protected Class<?> getSplashActivityClass() {
        return SplashActivity.class;
    }

    protected SMRouteNavigationMapFragment getMapFragment() {
        if (mapFragment == null)
            return mapFragment = new SMRouteNavigationMapFragment();
        else
            return mapFragment;
    }

    protected InstructionsPagerAdapter getPagerAdapter() {
        return new InstructionsPagerAdapter(getSupportFragmentManager(), mapFragment, this);
    }

    @Override
    public void onLowMemory() {
        try {
            if (mapFragment != null && mapFragment.mapView != null && mapFragment.mapView.getTileProvider() != null) {
                mapFragment.mapView.getTileProvider().clearTileCache();
            }
        } catch (Exception e) {

        }
    }

    private class PagerListener implements OnPageChangeListener {

        @Override
        public void onPageSelected(int position) {
            if (!instructionsUpdated || (mapFragment.isRecalculation && !mapFragment.getTrackingMode())) {
                SMTurnInstruction turn = mapFragment.route.getTurnInstructions().get(position);
                if (turn.drivingDirection == SMTurnInstruction.TurnDirection.ReachedYourDestination
                        || turn.drivingDirection == SMTurnInstruction.TurnDirection.ReachingDestination) {
                    mapFragment.animateTo(mapFragment.route.getEndLocation());
                } else {
                    mapFragment.animateTo(turn.getLocation());
                }
                stopTrackingUser();
                mapFragment.rotateMap(-turn.azimuth);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    }

}
