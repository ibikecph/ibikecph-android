// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.
package dk.kk.ibikecphlib.search;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.fasterxml.jackson.databind.JsonNode;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.util.DB;
import dk.kk.ibikecphlib.util.LOG;
import dk.kk.ibikecphlib.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class SearchAutocompleteActivity extends Activity {

    public static final int RESULT_AUTOTOCMPLETE_SET = 103;
    public static final int RESULT_AUTOTOCMPLETE_NOT_SET = 104;

    private EditText textSrch;
    private ListView listSearch;
    private AutocompleteAdapter adapter;
    private SearchListItem currentSelection;
    private int lastTextSize = 0;
    private boolean addressPicked = false, isA = false, isClose = false;
    /**
     * @deprecated Use the address on the currentSelection SearchListItem instead
     */
    private Address address;
    private ProgressBar progressBar;

    private Thread kmsThread;
    private Address lastAddress = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_autocomplete_activity);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        Bundle data = getIntent().getExtras();
        if (data != null) {
            isA = data.getBoolean("isA", false);
        }
        textSrch = (EditText) findViewById(R.id.textLocation);
        textSrch.addTextChangedListener(new MyTextWatcher());
        textSrch.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
        textSrch.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (currentSelection == null && listSearch.getAdapter() != null && listSearch.getAdapter().getCount() > 0) {
                        onItemClicked(0, false);
                    } else if (currentSelection != null) {
                        finishEditing();
                    }
                    return true;
                }
                return false;
            }
        });
        listSearch = (ListView) findViewById(R.id.listSearch);
        adapter = new AutocompleteAdapter(this, new ArrayList<SearchListItem>(), isA);
        listSearch.setAdapter(adapter);
        if (isA) {
            adapter.add(new CurrentLocationListItem());
        }
        listSearch.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long arg3) {
                onItemClicked(position, true);
            }

        });
        if (data != null && data.containsKey("lastName")) {
            ArrayList<SearchListItem> listData = new ArrayList<SearchListItem>();
            DB db = new DB(this);
            String reuseName = data.getString("lastName");
            SearchListItem sli = db.getFavoriteByName(reuseName);
            if (sli != null) {
                listData.add(sli);
            } else {
                sli = db.getSearchHistoryByName(reuseName);
                if (sli != null) {
                    listData.add(sli);
                } else {
                    SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                    String nodeStr = prefs.getString("lastSearchItem", null);
                    if (nodeStr != null) {
                        JsonNode node = Util.stringToJsonNode(nodeStr);
                        if (node != null) {
                            sli = SearchListItem.instantiate(node);
                            if (sli != null) {
                                listData.add(sli);
                            }
                        }
                    }
                }
            }
            db.close();
            address = AddressParser.parseAddressRegex(reuseName);
            adapter.updateListData(listData, AddressParser.addresWithoutNumber(reuseName), address);
            textSrch.setText(reuseName);
            textSrch.setSelection(reuseName.length());
        }
        MapActivity.fromSearch = true;
    }


    private void onItemClicked(int position, boolean isFromAdapter) {
        currentSelection = (SearchListItem) listSearch.getAdapter().getItem(position);
        boolean isFinishing = false;
        if (currentSelection.type != SearchListItem.nodeType.KORTFOR && currentSelection.type != SearchListItem.nodeType.CURRENT_POSITION) {
            isFinishing = true;
            finishAndPutData();
        }
        /*
        else if (currentSelection.type == SearchListItem.nodeType.CURRENT_POSITION) {
            isFinishing = true;
            (new Thread() {

                @Override
                public void run() {
                    JsonNode node = null;
                    try {
                        node = HTTPAutocompleteHandler.getOiorestAddress(IBikeApplication.getService().getLastValidLocation().getLatitude(),
                                IBikeApplication.getService().getLastValidLocation().getLongitude());
                    } catch (Exception ex) {
                    }
                    if (node != null) {
                        currentSelection.jsonNode = node;
                    } else {
                        //Dialog der siger slå GPS-til?
                        return;
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            finishAndPutData();
                        }
                    });
                }
            }).start();
        }
         */
        else if (currentSelection instanceof KortforsyningenListItem) {
            if (((KortforsyningenListItem) currentSelection).isPlace()) {
                isFinishing = true;
                finishAndPutData();
            } else {
                KortforsyningenListItem kd = (KortforsyningenListItem) currentSelection;
                if (kd.getAddress().getHouseNumber() != null && !kd.getAddress().getHouseNumber().equals("") && kd.hasCoordinates()) {
                    address.setHouseNumber(kd.getAddress().getHouseNumber());
                    isFinishing = true;
                    finishAndPutData();
                }
            }
        }

        if (!isFinishing) {
            addressPicked = true;
            String number = AddressParser.numberFromAddress(textSrch.getText().toString());
            if (isFromAdapter) {
                textSrch.setText(currentSelection.getOneLineName());
            }
            int firstCommaIndex = textSrch.getText().toString().indexOf(',');
            if (number != null) {
                if (firstCommaIndex > 0) {
                    String street = textSrch.getText().toString().replaceAll("\n", ",").substring(0, firstCommaIndex);
                    String city = textSrch.getText().toString().replaceAll("\n", ",")
                            .substring(firstCommaIndex, textSrch.getText().toString().length() - 1);
                    if (isFromAdapter) {
                        textSrch.setText(street + " " + number + city);
                        textSrch.setSelection(street.length() + 2);
                    }
                } else
                    textSrch.setText(textSrch.getText().toString() + " " + number);
            } else if (firstCommaIndex > 0) {
                // add a whitespace for a house number input
                if (isFromAdapter) {
                    textSrch.setText(textSrch.getText().toString().substring(0, firstCommaIndex) + " "
                            + textSrch.getText().toString().substring(firstCommaIndex, textSrch.getText().toString().length()));
                    textSrch.setSelection(firstCommaIndex + 1);
                }
            }
            if (!isFromAdapter) {

                // TODO: nope nope nope nope

                /*
                if (!address.hasHouseNumber() && currentSelection != null && currentSelection.getNumber() != null) {
                    address.setHouseNumber(currentSelection.getName());
                }
                */
                finishEditing();
            } else if (currentSelection != null && currentSelection.getAddress().getHouseNumber() != null && !currentSelection.getAddress().getHouseNumber().isEmpty()) {
                address.setHouseNumber(currentSelection.getAddress().getHouseNumber());
                finishEditing();
            }
        }

    }

    private void finishEditing() {
        progressBar.setVisibility(View.VISIBLE);
        if (address != null && !address.hasHouseNumber() && currentSelection != null
                && currentSelection instanceof KortforsyningenListItem && !((KortforsyningenListItem) currentSelection).isPlace()) {
            address.setHouseNumber("1");
        }
        performGeocode();
    }

    private void performGeocode() {
        (new Thread() {
            @Override
            public void run() {
                if (address != null && address.hasHouseNumber() && !isClose) {
                    JsonNode node;
                    try {
                        if (currentSelection == null) {
                            currentSelection = new KortforsyningenListItem(AddressParser.addresWithoutNumber(textSrch.getText().toString()), address.getHouseNumber());
                        }

                        /*
                         TODO: Refactor this to Geocoder class

                        LOG.d("Street searchfor the number " + address.getHouseNumber());
                        String urlString = "http://geo.oiorest.dk/adresser.json?q="
                                + URLEncoder.encode(currentSelection.getAddress().getStreet() + " " + address.getHouseNumber(), "UTF-8");
                        boolean coordinatesFound = false;
                        if (adapter != null && adapter.getCount() > 0 && adapter.getItem(0) instanceof KortforsyningenListItem) {
                            KortforsyningenListItem kd = (KortforsyningenListItem) adapter.getItem(0);
                            if (kd.getAddress().getHouseNumber() != null && kd.getAddress().getHouseNumber().equals(address.getHouseNumber()) && kd.hasCoordinates()) {
                                currentSelection.getAddress().setLocation(kd.getAddress().getLocation());
                                coordinatesFound = true;
                            }
                        }
                        if (!coordinatesFound) {
                            node = HTTPAutocompleteHandler.getOiorestGeocode(urlString, "" + address.getHouseNumber());
                            if (node != null) {
                                if (node.has("wgs84koordinat") && node.get("wgs84koordinat").has("bredde") && node.get("wgs84koordinat").has("længde")) {
                                    double latitude = Double.parseDouble(node.get("wgs84koordinat").get("bredde").asText());
                                    double longitude = Double.parseDouble(node.get("wgs84koordinat").get("længde").asText());
                                    currentSelection.getAddress().setLocation(new LatLng(latitude, longitude));
                                }
                            }
                        }
                        */

                        if (currentSelection != null && currentSelection.getAddress().getLocation().getLatitude() > -1 && currentSelection.getAddress().getLocation().getLongitude() > -1)
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    finishAndPutData();
                                }
                            });
                    } catch (Exception e) {
                        LOG.e(e.getLocalizedMessage());
                    }

                } else if (isClose)
                    finishAndPutData();
            }

        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard();
    }

    public void updateListData(List<SearchListItem> list, String searchText, Address addr) {
        if (textSrch.getText().toString().equals(searchText)) {
            // We are currently searching for this
            adapter.updateListData(list, searchText, addr);
        }
    }

    public void hideKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private class MyTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(Editable statusText) {
            if (kmsThread != null && kmsThread.isAlive()) {
                kmsThread.interrupt();
            }
            final Address newAddress = AddressParser.parseAddressRegex(textSrch.getText().toString().replaceAll("\n", ","));
            LOG.d("after text changed");
            if (address == null || !address.equals(newAddress)) {
                LOG.d("clearing the adapter and spawning the search threads");
                adapter.clear();
                if (textSrch.getText().toString().length() >= 2) {
                    final Location loc1;
                    if (IBikeApplication.getService().hasValidLocation()) {
                        loc1 = IBikeApplication.getService().getLastValidLocation();
                    } else {
                        loc1 = Util.COPENHAGEN;
                    }
                    // TODO: Consider why we would like to search for only the name
                    // final String searchText = AddressParser.addresWithoutNumber(textSrch.getText().toString());
                    final String searchText = textSrch.getText().toString();

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            spawnSearchThreads(loc1, searchText, newAddress);
                        }
                    }, 500);

                    adapter.updateListData(textSrch.getText().toString(), newAddress);

                    if (textSrch.getText().toString().length() != lastTextSize && textSrch.getText().toString().length() > 1
                            && !addressPicked) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    addressPicked = false;
                    lastTextSize = textSrch.getText().toString().length();
                }
            }
            address = newAddress;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int count, int after) {

        }
    }

    private void spawnSearchThreads(final Location loc, final String searchText, final Address addr) {
        Log.d("SearchAutocomplete", "spawnSearchThreads called with searchText = " + searchText);
        if (lastAddress != null && lastAddress.equals(addr) && adapter != null && adapter.getCount() != 0) {
            Log.d("SearchAutocomplete", "Apparently there was no need to search, with lastAddress " + lastAddress);
            return;
        } else {
            lastAddress = addr;
        }
        if (!Util.isNetworkConnected(this)) {
            Util.launchNoConnectionDialog(this);
            progressBar.setVisibility(View.INVISIBLE);
        } else {
            // fetch the Kortforsyningen autocomplete
            kmsThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("SearchAutocomplete", "Running the kmsThread with searchText=" + searchText);
                    // final List<JsonNode> kortforsyningenList = new ArrayList<JsonNode>();
                    final ArrayList<SearchListItem> data = new ArrayList<SearchListItem>();
                    if (addr.hasStreet()) {

                        // Wrapping this in try/catch in case the user has already quit the Activity before the thread stops running
                        try {
                            LOG.d("fetching from KSM, input has no street");
                            List<JsonNode> list = HTTPAutocompleteHandler.getKortforsyningenAutocomplete(loc, addr);
                            int count = 0;
                            if (list != null) {
                                for (JsonNode node : list) {
                                    if (count == 10) {
                                        break;
                                    }
                                    KortforsyningenListItem kd = new KortforsyningenListItem(node);
                                    if (addr.getZip() != null && !addr.getZip().equals("") && kd.getAddress().getZip() != null) {
                                        if (!addr.getZip().trim().toLowerCase(Locale.UK).equals(kd.getAddress().getZip().toLowerCase(Locale.UK))) {
                                            continue;
                                        }
                                    }
                                    if (addr.hasCity() && !addr.getCity().equals(addr.getStreet()) && kd.getAddress().getCity() != null) {
                                        if (!(addr.getCity().trim().toLowerCase(Locale.UK).contains(kd.getAddress().getCity().toLowerCase(Locale.UK)) ||
                                                kd.getAddress().getCity().trim().toLowerCase(Locale.UK).contains(addr.getCity().toLowerCase(Locale.UK)))) {
                                            continue;
                                        }
                                    }
                                    data.add(kd);
                                    count++;
                                }

                            }

                        } catch (NullPointerException e) {
                            // whatever
                        }

                    }
                    if (!addr.isAddress()) {
                        LOG.d("fetching from KSM, input is not an address");
                        List<JsonNode> places = HTTPAutocompleteHandler.getKortforsyningenPlaces(loc, addr);
                        if (places != null) {
                            int count = 0;
                            if (places != null) {
                                LOG.d("places count = " + places.size() + " data = " + places.toString());
                                for (JsonNode node : places) {
                                    if (count == 10) {
                                        break;
                                    }
                                    KortforsyningenListItem kd = new KortforsyningenListItem(node);
                                    if (addr.hasZip() && kd.getAddress().getZip() != null) {
                                        if (!addr.getZip().trim().toLowerCase(Locale.UK).equals(kd.getAddress().getZip().toLowerCase(Locale.UK))) {
                                            continue;
                                        }
                                    }
                                    if (addr.hasCity() && !addr.getCity().equals(addr.getStreet())
                                            && kd.getAddress().getCity() != null) {
                                        if (!(addr.getCity().trim().toLowerCase(Locale.UK).contains(kd.getAddress().getCity().toLowerCase(Locale.UK)) || kd
                                                .getAddress().getCity().trim().toLowerCase(Locale.UK).contains(addr.getCity().toLowerCase(Locale.UK)))) {
                                            continue;
                                        }
                                    }
                                    data.add(kd);
                                    count++;
                                }

                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            updateListData(data, searchText, addr);

                        }
                    });
                }

            });
            kmsThread.start();

        }
    }

    boolean isFinishing = false;

    /**
     * TODO: Clean this up + fix errors when currentSelection.getAddress().getLocation() == null
     */
    public void finishAndPutData() {
        progressBar.setVisibility(View.INVISIBLE);
        if (isFinishing) {
            return;
        }
        isFinishing = true;

        Intent intent = new Intent();
        if (currentSelection != null) {
            currentSelection.getAddress().setSource(Address.Source.SEARCH);
            intent.putExtra("addressObject", currentSelection.getAddress());
            setResult(RESULT_AUTOTOCMPLETE_SET, intent);
        } else {
            setResult(RESULT_AUTOTOCMPLETE_NOT_SET, intent);
        }

        // TODO: Consider what this latter part is good for?
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if (currentSelection != null) {
            JsonNode node = currentSelection.getJsonNode();
            if (node != null) {
                prefs.edit().putString("lastSearchItem", node.toString()).commit();
            }
        }
        finish();
    }

    @Override
    public void onStop() {
        if (kmsThread != null) {
            kmsThread.interrupt();
            kmsThread = null;
        }

        super.onStop();
    }

}
