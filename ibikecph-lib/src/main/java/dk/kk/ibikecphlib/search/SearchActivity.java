// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import android.app.Activity;
import android.content.Intent;
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

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.controls.ObservableScrollView;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.util.DB;
import dk.kk.ibikecphlib.util.LOG;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * This is the activity used for searching for an address. It would be very nice to integrate the same UI element as on
 * the TrackingActivity for the listviews.
 */
public class SearchActivity extends Activity {

    private TextView textCurrentLoc, textB, textFavorites, textRecent;
    private ListView listHistory, listFavorites;
    private ArrayList<SearchListItem> favorites;
    ArrayList<SearchListItem> searchHistory = new ArrayList<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        listHistory = (ListView) findViewById(R.id.historyList);
        listFavorites = (ListView) findViewById(R.id.favoritesList);

        textCurrentLoc = (TextView) findViewById(R.id.textCurrentLoc);
        textCurrentLoc.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(SearchActivity.this, SearchAutocompleteActivity.class);
                i.putExtra("isA", true);
                startActivityForResult(i, 1);
            }

        });

        textB = (TextView) findViewById(R.id.textB);
        textB.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SearchActivity.this, SearchAutocompleteActivity.class);
                // TODO: Consider passing a name to reuse as a starting point when auto-completing:
                // i.putExtra("lastName", bName);
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
        Log.d("SearchActivity", "startButtonHandler called with address = " + address);
        Log.d("SearchActivity", "\taddress.getSource() = " + address.getSource());

        // Start routing
        Intent intent = new Intent();

        if (address != null) {
            intent.putExtra("addressObject", address);
        }

        if (address.getSource() == Address.Source.SEARCH) {
            Calendar cal = Calendar.getInstance();
            String date = cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR);

            HistoryListItem hd = new HistoryListItem(-1, address.getName(), address.getFullAddress(), date, date, address.getLocation().getLatitude(), address.getLocation().getLongitude());

            new DB(SearchActivity.this).saveSearchHistory(hd);
            intent.putExtra("addressObject", address);
        }

        if (address.getSource() == Address.Source.HISTORYDATA) {
            if (address != null) {
                intent.putExtra("addressObject", address);
            }
        }

        if (address.getSource() == Address.Source.FAVORITE) {
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
    }

    @Override
    public void onResume() {
        super.onResume();

        initStrings();

        searchHistory = new DB(this).getSearchHistory();

        HistoryAdapter adapter = new HistoryAdapter(this, searchHistory);
        listHistory.setAdapter(adapter);
        listHistory.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                HistoryListItem hd = (HistoryListItem) ((HistoryAdapter) listHistory.getAdapter()).getItem(position);
                Address address = hd.getAddress();
                startButtonHandler(address);
            }

        });
        favorites = new ArrayList<>();
        favorites = new DB(this).getFavorites2();
        if (favorites != null && favorites.size() == 0) {
            tFetchFavorites thread2 = new tFetchFavorites();
            thread2.start();
        }
        show3favorites();
        listFavorites.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                FavoriteListItem hd = (FavoriteListItem) ((HistoryAdapter) listFavorites.getAdapter()).getItem(position);
                Address address = hd.getAddress();
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
        textCurrentLoc.setText(IBikeApplication.getString("current_position"));
        textB.setHint(IBikeApplication.getString("search_to_placeholder"));
        textFavorites.setText(IBikeApplication.getString("favorites"));
        textFavorites.setTypeface(IBikeApplication.getBoldFont());
        textRecent.setText(IBikeApplication.getString("recent_results"));
        textRecent.setTypeface(IBikeApplication.getBoldFont());
        ((TextView) findViewById(R.id.textOverviewHeader)).setTypeface(IBikeApplication.getBoldFont());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case SearchAutocompleteActivity.RESULT_AUTOTOCMPLETE_SET:
                if (data != null) {
                    Bundle b = data.getExtras();
                    Address address = (Address) b.getSerializable("addressObject");
                    startButtonHandler(address);
                } else {
                    throw new RuntimeException("Expected data when autocomplete was set.");
                }
                break;
        }
    }

    private class tFetchFavorites extends Thread {

        @Override
        public void run() {
            DB db = new DB(SearchActivity.this);
            db.getFavoritesFromServer(null);
            favorites = db.getFavorites2();
            if (favorites == null)
                favorites = new ArrayList<>();

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

    /**
     * Resize both lists
     */
    private void resizeLists() {
        resizeList(listHistory);
        resizeList(listFavorites);
        findViewById(R.id.rootLayout).invalidate();
        // scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    /**
     * Fixes an issue that occurs when list views of variable height is used within a scroll view
     * @see <a href="http://stackoverflow.com/questions/18367522/android-list-view-inside-a-scroll-view">stackoverflow issue</a>
     */
    protected void resizeList(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {
            int totalHeight = 0;
            for (int i = 0; i < listAdapter.getCount(); i++) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount()));
            listView.setLayoutParams(params);
        }
    }

    private void show3favorites() {
        if (favorites != null && favorites.size() != 0) {
            final HistoryAdapter adapter = new HistoryAdapter(SearchActivity.this, favorites);
            listFavorites.setAdapter(adapter);
            resizeLists();
        }
        updateLayout();
    }
}
