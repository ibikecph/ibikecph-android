package com.spoiledmilk.ibikecph.map.fragments;

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

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteRequester;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.RouteType;
import com.spoiledmilk.ibikecph.map.handlers.NavigationMapHandler;
import com.spoiledmilk.ibikecph.navigation.routing_engine.BreakRouteResponse;
import com.spoiledmilk.ibikecph.navigation.routing_engine.Journey;
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

        // Trigger the route type changed to update route type buttons.
        RouteSelectionState state = getMapState(RouteSelectionState.class);
        routeTypeChanged(state.getType());

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

    public void brokenRouteReady(final BreakRouteResponse response) {
        progressBarHolder.setVisibility(View.GONE);
        breakRouteContainer.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
        tabs.setVisibility(View.VISIBLE);

        MapActivity activity = (MapActivity) getActivity();
        FragmentManager fm = activity.getSupportFragmentManager();
        pager.setAdapter(new BreakRoutePagerAdapter(fm, response));
        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Journey journey = response.getJourney(position);
                mapState.setJourney(journey);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        };
        tabs.setOnPageChangeListener(pageChangeListener);
        tabs.setViewPager(pager);
        // Call the listener right away.
        pageChangeListener.onPageSelected(pager.getCurrentItem());
    }

    @Override
    public void routeTypeChanged(RouteType newType) {
        super.routeTypeChanged(newType);
        if(breakRouteContainer != null && breakRouteContainer != null) {
            if(newType == RouteType.BREAK) {
                progressBarHolder.setVisibility(View.VISIBLE);
                breakRouteContainer.setVisibility(View.GONE);
            } else {
                progressBarHolder.setVisibility(View.GONE);
                breakRouteContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * TODO: Refactor by renaming
     */
    class BreakRoutePagerAdapter extends FragmentStatePagerAdapter {

        protected BreakRouteResponse response;

        public BreakRoutePagerAdapter(FragmentManager fm, BreakRouteResponse response) {
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
