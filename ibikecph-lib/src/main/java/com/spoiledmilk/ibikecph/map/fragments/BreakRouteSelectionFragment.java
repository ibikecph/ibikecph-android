package com.spoiledmilk.ibikecph.map.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.BreakRouteRequester;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.ObservableInteger;
import com.spoiledmilk.ibikecph.map.OnIntegerChangeListener;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.tracking.TrackListAdapter;
import com.viewpagerindicator.CirclePageIndicator;

/**
 * Created by kraen on 21-06-16.
 */
public class BreakRouteSelectionFragment extends RouteSelectionFragment {

    protected View breakRouteContainer;
    protected CirclePageIndicator tabs;
    protected ViewPager pager;
    protected ProgressBar progressBar;
    protected FrameLayout progressBarHolder;

    @Override
    public void refreshView() {
        super.refreshView();

        SMRoute route = mapState.getRoute();

        // TODO: Refactor so deprecated static members are no longer used.
        // Set the distance label
        if (MapActivity.isBreakChosen && Geocoder.totalBikeDistance != null) {
            float distance = 0;
            float duration;
            long arrivalTime = 0;

            distance = Geocoder.totalBikeDistance.get(NavigationMapHandler.obsInt.getPageValue());
            duration = Geocoder.totalTime.get(NavigationMapHandler.obsInt.getPageValue());
            arrivalTime = Geocoder.arrivalTime.get(NavigationMapHandler.obsInt.getPageValue());
            sourceText.setText(IBikeApplication.getString("current_position")); //Just set current position as default because this is the only option working right now.
            destinationText.setText(DestinationPreviewFragment.name);

            arrivalTime = arrivalTime * 1000;
            etaText.setText(dateFormat.format(arrivalTime).toString());

            // TODO: Change the location of this utility function
            durationText.setText(TrackListAdapter.durationToFormattedTime(duration));

            if (distance > 1000) {
                distance /= 1000;
                lengthText.setText(String.format("%.1f km", distance));
            } else {
                lengthText.setText(String.format("%d m", (int) distance));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        breakButton.setVisibility(View.VISIBLE);
        cargoButton.setVisibility(View.GONE);

        breakRouteContainer = v.findViewById(R.id.breakRouteContainer);
        tabs = (CirclePageIndicator) v.findViewById(R.id.tabs);
        pager = (ViewPager) v.findViewById(R.id.pager);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        progressBarHolder = (FrameLayout) v.findViewById(R.id.progressBarHolder);

        // Setting up the tabs
        tabs.setRadius(10);
        tabs.setCentered(true);
        tabs.setFillColor(getResources().getColor(R.color.PrimaryColor));

        // Update the route type right away, to make trigger a route type change
        mapState.setType(RouteType.FASTEST);

        return v;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.break_route_selection_fragment;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.routeSelectionBreakButton) {
            // TODO: Refactor out the following two lines.
            NavigationMapHandler.routePos = 0;
            pager.setAdapter(null);

            mapState.setType(RouteType.BREAK);
        } else {
            super.onClick(v);
        }
    }

    public void brokenRouteReady(BreakRouteRequester.BreakRouteResponse response) {
        progressBarHolder.setVisibility(View.GONE);
        breakRouteContainer.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
        tabs.setVisibility(View.VISIBLE);

        MapActivity activity = (MapActivity) getActivity();
        FragmentManager fm = activity.getSupportFragmentManager();
        pager.setAdapter(new BreakRoutePagerAdapter(fm, response));
        tabs.setViewPager(pager);

        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("BreakRouteSelectionFrag", "onPageSelected called with " + position);
                NavigationMapHandler.obsInt.setPageValue(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /*
     * Creates a static observable integer for the Geocoder to callback when the amount of
     * alternative breaking routes are available. When they are a listner is registered on the
     * CirclePageIndicator tabs, that will notify the NavigationMapHandler when the user swipes.
     * TODO: Consider refactoring this so static members of classes are no longer needed.
     * /
    public void setupBreakRouteListener() {
        MapActivity.obsInt = new ObservableInteger();
        MapActivity.obsInt.setOnIntegerChangeListener(new OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                if (newValue > 0) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (MapActivity.isBreakChosen) {
                                progressBarHolder.setVisibility(View.GONE);
                                breakRouteContainer.setVisibility(View.VISIBLE);
                                pager.setVisibility(View.VISIBLE);
                                tabs.setVisibility(View.VISIBLE);

                                MapActivity activity = (MapActivity) getActivity();
                                FragmentManager fm = activity.getSupportFragmentManager();
                                pager.setAdapter(new BreakRoutePagerAdapter(fm));
                                tabs.setViewPager(pager);
                            }
                        }
                    });
                }
            }
        });
    }
    */

    @Override
    public void routeTypeChanged(RouteType newType) {
        super.routeTypeChanged(newType);
        if(newType == RouteType.BREAK) {
            progressBarHolder.setVisibility(View.VISIBLE);
            breakRouteContainer.setVisibility(View.GONE);
        } else {
            progressBarHolder.setVisibility(View.GONE);
            breakRouteContainer.setVisibility(View.GONE);
        }
    }

    /**
     * TODO: Refactor by renaming
     */
    class BreakRoutePagerAdapter extends FragmentStatePagerAdapter {

        protected BreakRouteRequester.BreakRouteResponse response;

        public BreakRoutePagerAdapter(FragmentManager fm, BreakRouteRequester.BreakRouteResponse response) {
            super(fm);
            this.response = response;
        }

        @Override
        public int getCount() {
            return response.getJsonNode().size();
        }

        @Override
        public Fragment getItem(int position) {
            return BreakRouteFragment.newInstance(response, position);
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }
}
