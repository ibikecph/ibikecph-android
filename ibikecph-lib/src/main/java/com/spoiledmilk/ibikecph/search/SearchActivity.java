// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.ObservableScrollView;
import com.spoiledmilk.ibikecph.controls.ScrollViewListener;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

/**
 * This is the activity used for searching for an address. It would be very nice to integrate the same UI element as on
 * the TrackingActivity for the listviews.
 */
public class SearchActivity extends Activity implements ScrollViewListener {

    public static final int RESULT_SEARCH_ROUTE = 102;
    private static final long HISTORY_FETCHING_TIMEOUT = 120 * 1000;
    private static final int MAX_RECENT_ADDRESSES = 3;

    private TextView textCurrentLoc, textB, textA, textFavorites, textRecent, textOverviewHeader;
    private ListView listHistory, listFavorites;
    private double BLatitude = -1, BLongitude = -1, ALatitude = -1, ALongitude = -1;
    private HistoryData historyData;
    private boolean isAsearched = false;
    private ArrayList<SearchListItem> favorites;
    private ObservableScrollView scrollView;
    private int listItemHeight = 0;
    private String fromName = "", toName = "", aName = "", bName = "";
    ArrayList<SearchListItem> searchHistory = new ArrayList<SearchListItem>();
    private long timestampHistoryFetched = 0;
    private boolean isDestroyed = false;
    private ActionBar actionBar;
    private Address addressFound;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDestroyed = false;
        setContentView(R.layout.search_activity);
        listHistory = (ListView) findViewById(R.id.historyList);
        listFavorites = (ListView) findViewById(R.id.favoritesList);
        textOverviewHeader = (TextView) findViewById(R.id.textOverviewHeader);
        scrollView = (ObservableScrollView) findViewById(R.id.scrollView);
        scrollView.setScrollViewListener(this);

        actionBar = getActionBar();

        textCurrentLoc = (TextView) findViewById(R.id.textCurrentLoc);
        textCurrentLoc.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isAsearched = true;
                Intent i = new Intent(SearchActivity.this, SearchAutocompleteActivity.class);
                i.putExtra("isA", true);
                startActivityForResult(i, 1);
            }

        });

        textB = (TextView) findViewById(R.id.textB);
        textB.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isAsearched = false;
                Intent i = new Intent(SearchActivity.this, SearchAutocompleteActivity.class);
                i.putExtra("lastName", bName);
                startActivityForResult(i, 1);
            }

        });

        textA = (TextView) findViewById(R.id.textA);
        textA.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                isAsearched = true;
                Intent i = new Intent(SearchActivity.this, SearchAutocompleteActivity.class);
                i.putExtra("isA", true);
                i.putExtra("lastName", aName);
                startActivityForResult(i, 1);
            }

        });

        textFavorites = (TextView) findViewById(R.id.textFavorites);
        textRecent = (TextView) findViewById(R.id.textRecent);
    }

    /**
     * Init the ActionBar, setting the relevant strings
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity_actions, menu);

        return super.onCreateOptionsMenu(menu);
    }


    /**
     * Handler for the Start routing button.
     */
    public void startButtonHandler(Address address) {
        // Start routing
        Intent intent = new Intent();
        if (ALatitude == -1 || ALongitude == -1) {
            Location start = IbikeApplication.getService().getLastValidLocation();
            if (start == null) {
                start = IbikeApplication.getService().getLastKnownLocation();
            }
            if (start != null) {
                ALatitude = start.getLatitude();
                ALongitude = start.getLongitude();
            }
        }
        String st = "Start: " + textA.getText().toString() + " (" + ALatitude + "," + ALongitude + ") End: " + textB.getText().toString()
                + " (" + BLongitude + "," + BLatitude + ")";

        intent.putExtra("startLng", ALongitude);
        intent.putExtra("startLat", ALatitude);
        intent.putExtra("endLng", BLongitude);
        intent.putExtra("endLat", BLatitude);
        intent.putExtra("fromName", fromName);
        intent.putExtra("toName", toName);

        if (address != null) {
            intent.putExtra("addressObject", address);
        }

        if (address.getAddressSource() == Address.AddressSource.SEARCH) {
            Calendar cal = Calendar.getInstance();
            String date = cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR);

            HistoryData hd = new HistoryData(-1, address.name, address.street, date, date, "", "", address.getLocation().getLatitude(), address.getLocation().getLongitude());

            Log.d("DV", "handleButton, name " + address.name);
            Log.d("DV", "handleButton, street " + address.street);
            Log.d("DV", "handleButton, houseNumber " + address.houseNumber);
            Log.d("DV", "handleButton, city " + address.city);
            Log.d("DV", "handleButton, zip " + address.zip);

            /*if (address.name.matches(".*\\d+.*")) {
                if (address.zip.length() < 5) {
                    hd.setName(address.name + ", " + address.zip + " " + address.city);
                    hd.setAddress(address.name + ", " + address.zip + " " + address.city);
                } else {
                    hd.setName(address.name + ", " + address.zip);
                    hd.setAddress(address.name + ", " + address.zip);
                }
            } else {
                if (address.houseNumber != null) {
                    if (address.zip.length() < 5) {
                        hd.setName(address.name + " " + address.houseNumber + ", " + address.zip + " " + address.city);
                        hd.setAddress(address.name + " " + address.houseNumber + ", " + address.zip + " " + address.city);
                    } else {
                        hd.setName(address.name + " " + address.houseNumber + ", " + address.zip);
                        hd.setAddress(address.name + " " + address.houseNumber + ", " + address.zip);
                    }
                } else {
                    if (address.zip.length() < 5) {
                        hd.setName(address.name + ", " + address.zip + " " + address.city);
                        hd.setAddress(address.name + ", " + address.zip + " " + address.city);
                    } else {
                        hd.setName(address.name + ", " + address.zip);
                        hd.setAddress(address.name + ", " + address.zip);
                    }

                }
            }*/

            Log.d("DV", "SearchActivity, address.street = " + hd.getAdress());
            new DB(SearchActivity.this).saveSearchHistory(hd, hd, SearchActivity.this);
            intent.putExtra("addressObject", address);
        }

        if (address.getAddressSource() == Address.AddressSource.HISTORYDATA) {
            Log.d("DV", "HISTORY!");
            if (address != null) {
                intent.putExtra("addressObject", address);
            }
        }

        // TODO: This sucks. It'd be nice to keep Address objects in the Recent buffer, rather than re-establishing here

        setResult(Activity.RESULT_OK, intent);
        finish();
        overridePendingTransition(R.anim.slide_out_down, R.anim.fixed);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
        if (textCurrentLoc.getVisibility() == View.VISIBLE
                && (IbikeApplication.getService().getLastValidLocation() != null || IbikeApplication.getService().getLastKnownLocation() != null)) {
            Location loc = IbikeApplication.getService().getLastValidLocation();
            if (loc == null) {
                loc = IbikeApplication.getService().getLastKnownLocation();
            }
            ALatitude = loc.getLatitude();
            ALongitude = loc.getLongitude();
        }

        if (System.currentTimeMillis() - timestampHistoryFetched > HISTORY_FETCHING_TIMEOUT) {
            searchHistory = new ArrayList<SearchListItem>();
            tFetchSearchHistory thread = new tFetchSearchHistory();
            thread.start();
        } else {
            searchHistory = new DB(this).getSearchHistory();
        }
        HistoryAdapter adapter = new HistoryAdapter(this, searchHistory);
        listHistory.setAdapter(adapter);
        listHistory.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                HistoryData hd = (HistoryData) ((HistoryAdapter) listHistory.getAdapter()).getItem(position);
                Address address = Address.fromHistoryData(hd);
                textB.setText(hd.getName().length() > 30 ? hd.getName().substring(0, 27) + "..." : hd.getName());
                bName = hd.getName();
                toName = hd.getAdress();
                toName = hd.getAdress();
                if (toName.contains(","))
                    toName = toName.substring(0, toName.indexOf(','));
                BLatitude = hd.getLatitude();
                BLongitude = hd.getLongitude();

                startButtonHandler(address);
            }

        });
        favorites = new ArrayList<SearchListItem>();
        favorites = new DB(this).getFavorites2();
        if (favorites != null && favorites.size() == 0) {
            tFetchFavorites thread2 = new tFetchFavorites();
            thread2.start();
        }
        show3favorites();
        listFavorites.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                FavoritesData hd = (FavoritesData) ((HistoryAdapter) listFavorites.getAdapter()).getItem(position);
                Address address = Address.fromFavoritesData(hd);
                textB.setText(hd.getName().length() > 30 ? hd.getName().substring(0, 27) + "..." : hd.getName());
                bName = hd.getName();
                BLatitude = hd.getLatitude();
                BLongitude = hd.getLongitude();
                toName = hd.getAdress();
                if (toName.contains(",")) {
                    toName = toName.substring(0, toName.indexOf(','));
                }
                startButtonHandler(address);
            }

        });
        resizeLists();
        updateLayout();
    }

    private void updateLayout() {
        if (listHistory.getAdapter() == null || listHistory.getAdapter().getCount() == 0) {
            listHistory.setVisibility(View.GONE);
        } else {
            listHistory.setVisibility(View.VISIBLE);
        }
        if (listFavorites.getAdapter() == null || listFavorites.getAdapter().getCount() == 0) {
            listFavorites.setVisibility(View.GONE);
        } else {
            listFavorites.setVisibility(View.VISIBLE);
        }
    }

    private void initStrings() {
        textCurrentLoc.setText(IbikeApplication.getString("current_position"));
        textB.setHint(IbikeApplication.getString("search_to_placeholder"));
        textFavorites.setText(IbikeApplication.getString("favorites"));
        textFavorites.setTypeface(IbikeApplication.getBoldFont());
        textRecent.setText(IbikeApplication.getString("recent_results"));
        textRecent.setTypeface(IbikeApplication.getBoldFont());
        ((TextView) findViewById(R.id.textOverviewHeader)).setTypeface(IbikeApplication.getBoldFont());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET:
                if (data != null) {
                    Bundle b = data.getExtras();

                    Address address = (Address) b.getSerializable("addressObject");

                   /*Log.d("DV", "SearchActivity, city == " + address.city);
                    Log.d("DV", "SearchActivity, street == " + address.street);
                    Log.d("DV", "SearchActivity, name == " + address.name);
                    Log.d("DV", "SearchActivity, zip == " + address.zip);
                    Log.d("DV", "SearchActivity, lat == " + address.lat);
                    Log.d("DV", "SearchActivity, lon == " + address.lon);*/

                    try {
                        BLatitude = b.getDouble("lat");
                        BLongitude = b.getDouble("lon");
                        String txt = AddressParser.textFromBundle(b);
                        bName = txt;
                        textB.setText(txt);
                        toName = b.getString("address");
                        if (toName.contains(",")) {
                            toName = toName.substring(0, toName.indexOf(','));
                        }
                        startButtonHandler(address);
                    } catch (Exception e) {
                        LOG.e(e.getLocalizedMessage());
                        BLatitude = -1;
                        BLongitude = -1;
                    }
                }
                break;
        }
    }

    private class tFetchSearchHistory extends Thread {

        @Override
        public void run() {
            final ArrayList<SearchListItem> searchHistory = IbikeApplication.isUserLogedIn() ? new DB(SearchActivity.this)
                    .getSearchHistoryFromServer(SearchActivity.this) : null;
            if (SearchActivity.this != null && !isDestroyed) {
                SearchActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        ArrayList<SearchListItem> searchHistory2 = searchHistory;
                        if (searchHistory == null || !IbikeApplication.isUserLogedIn()) {
                            searchHistory2 = (new DB(SearchActivity.this)).getSearchHistory();
                            if (searchHistory2 != null) {
                                final HistoryAdapter adapter = new HistoryAdapter(SearchActivity.this, searchHistory2);
                                listHistory.setAdapter(adapter);
                            }
                        } else {
                            SearchActivity.this.searchHistory.clear();
                            Iterator<SearchListItem> it = searchHistory.iterator();
                            int count = 0;
                            while (it.hasNext() && count < MAX_RECENT_ADDRESSES) {
                                SearchListItem sli = it.next();
                                if (sli.getName().contains(".")) {
                                    continue;
                                }
                                SearchActivity.this.searchHistory.add(sli);
                                count++;
                            }
                        }
                        ((HistoryAdapter) listHistory.getAdapter()).notifyDataSetChanged();
                        resizeLists();
                        updateLayout();
                        timestampHistoryFetched = System.currentTimeMillis();
                    }
                });
            }

        }
    }

    private class tFetchFavorites extends Thread {

        @Override
        public void run() {
            DB db = new DB(SearchActivity.this);
            db.getFavoritesFromServer(SearchActivity.this, null);
            favorites = db.getFavorites2();
            if (favorites == null)
                favorites = new ArrayList<SearchListItem>();

            SearchActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    show3favorites();

                }
            });

        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void resizeLists() {
        // this is needed when there is a list view inside a scroll view
        ListAdapter listAdapter = listHistory.getAdapter();
        if (listAdapter != null) {
            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listHistory);
                listItem.measure(0, 0);
                listItemHeight = listItem.getMeasuredHeight();
                totalHeight += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = listHistory.getLayoutParams();
            params.height = totalHeight + (listHistory.getDividerHeight() * (listAdapter.getCount()));
            listHistory.setLayoutParams(params);
        }
        listAdapter = listFavorites.getAdapter();
        if (listAdapter != null) {
            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listFavorites);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = listFavorites.getLayoutParams();
            params.height = totalHeight + (listFavorites.getDividerHeight() * (listAdapter.getCount()));
            listFavorites.setLayoutParams(params);
        }
        findViewById(R.id.rootLayout).invalidate();
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    private void show3favorites() {
        if (favorites != null && favorites.size() != 0) {
            final HistoryAdapter adapter = new HistoryAdapter(SearchActivity.this, favorites);
            listFavorites.setAdapter(adapter);
            resizeLists();
        }
        updateLayout();
    }

    @Override
    public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
        if (listFavorites.getAdapter() != null) {
            if (y > 0) {
                textOverviewHeader.setVisibility(View.VISIBLE);
            } else {
                textOverviewHeader.setVisibility(View.GONE);
            }
            if (y <= (listFavorites.getAdapter().getCount() + 2) * listItemHeight) {
                textOverviewHeader.setText(IbikeApplication.getString("favorites"));
            } else {
                textOverviewHeader.setText(IbikeApplication.getString("recent_results"));
            }
        }
    }
}
