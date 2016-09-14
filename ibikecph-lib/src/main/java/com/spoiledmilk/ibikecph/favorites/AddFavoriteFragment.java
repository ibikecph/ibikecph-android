// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.favorites;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.search.AddressParser;
import com.spoiledmilk.ibikecph.search.SearchAutocompleteActivity;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A Fragment used inside the LeftMenu for adding a favorite address.
 *
 * @author jens
 */
public class AddFavoriteFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    protected EditText textAddress;
    protected EditText textFavoriteName;
    private Button btnSave;
    private FavoriteListItem favoriteListItem = null;
    protected String currentFavoriteType = "";
    private AlertDialog dialog;
    boolean isTextChanged = false;

    public enum RequestCode {
        SearchForAddress
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View ret = inflater.inflate(R.layout.fragment_add_favorite, container, false);

        textAddress = (EditText) ret.findViewById(R.id.textAddress);
        textAddress.setClickable(true);
        textAddress.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(getActivity(), SearchAutocompleteActivity.class);
                i.putExtra("isA", true);
                getActivity().startActivityForResult(i, RequestCode.SearchForAddress.ordinal());
            }
        });

        textFavoriteName = (EditText) ret.findViewById(R.id.textFavoriteName);
        textFavoriteName.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !isTextChanged) {

                    isTextChanged = true;
                    //textFavoriteName.setText("");
                }
            }
        });


        btnSave = (Button) ret.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                saveFavorite();
            }
        });

        textFavoriteName.setText(IBikeApplication.getString("Favorite"));

        ((TextView) ret.findViewById(R.id.labelName)).setText(IBikeApplication.getString("Name"));
        ((TextView) ret.findViewById(R.id.labelAddress)).setText(IBikeApplication.getString("Address"));

        ((RadioButton) ret.findViewById(R.id.radioButtonFavorite)).setText(IBikeApplication.getString("Favorite"));
        ((RadioButton) ret.findViewById(R.id.radioButtonHome)).setText(IBikeApplication.getString("Home"));
        ((RadioButton) ret.findViewById(R.id.radioButtonSchool)).setText(IBikeApplication.getString("School"));
        ((RadioButton) ret.findViewById(R.id.radioButtonWork)).setText(IBikeApplication.getString("Work"));

        ((RadioGroup) ret.findViewById(R.id.favoriteTypeRadioGroup)).setOnCheckedChangeListener(this);

        ((RadioButton) ret.findViewById(R.id.radioButtonFavorite)).setChecked(true);
        onCheckedChanged((RadioGroup) ret.findViewById(R.id.favoriteTypeRadioGroup), R.id.radioButtonFavorite);

        return ret;
    }

    public final void saveFavorite(boolean finish) {
        if (Util.isNetworkConnected(getActivity())) {
            if (favoriteListItem != null && textFavoriteName.getText().toString() != null
                    && !textFavoriteName.getText().toString().trim().equals("")) {
                if (new DB(getActivity()).favoritesForName(textFavoriteName.getText().toString().trim()) == 0) {
                    favoriteListItem.getAddress().setName(textFavoriteName.getText().toString());
                    favoriteListItem.setSubSource(currentFavoriteType);


                    // TODO: Change this to the implementation described here
                    // https://developers.google.com/analytics/devguides/collection/android/v4/#send-an-event
                    // IBikeApplication.getTracker().sendEvent("Favorites", "Save", st, (long) 0);

                    Thread saveThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            (new DB(getActivity())).saveFavorite(favoriteListItem, false);
                        }
                    });

                    // TODO: Why even use threading if you join right after starting the thread?
                    saveThread.start();
                    try {
                        saveThread.join();
                    } catch (Exception e) {
                        e.getLocalizedMessage();
                    }

                    if (finish) {
                        getActivity().setResult(FavoritesListActivity.RESULT_OK);
                        getActivity().finish();
                    }

                } else {
                    Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(IBikeApplication.getString("name_used"));
                    builder.setTitle(IBikeApplication.getString("Error"));
                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.show();
                }
            } else if (getActivity() != null) {
                Util.showSimpleMessageDlg(getActivity(), IBikeApplication.getString("register_error_fields"));
            }
        } else {
            Util.launchNoConnectionDialog(getActivity());
        }
    }

    public final void saveFavorite() {
        saveFavorite(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        initStrings();
    }

    private void initStrings() {
        textAddress.setHint(IBikeApplication.getString("add_favorite_address_placeholder"));
        btnSave.setText(IBikeApplication.getString("save_favorite"));
        if (currentFavoriteType.equals(""))
            currentFavoriteType = IBikeApplication.getString("Favorite");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Figure out what request generated the result
        if(requestCode < 0 || RequestCode.values().length <= requestCode) {
            throw new RuntimeException("Unexpected activity result - requestCode out of bounds");
        }
        RequestCode request = RequestCode.values()[requestCode];
        if(request.equals(RequestCode.SearchForAddress)) {
            // The user wanted to search for an address
            Bundle b = intent.getExtras();
            Address addressObject = (Address) b.getSerializable("addressObject");

            if(addressObject == null) {
                throw new RuntimeException("Expected an addressObject");
            }

            String address = addressObject.getPrimaryDisplayString();
            LatLng location = addressObject.getLocation();
            favoriteListItem = new FavoriteListItem(textFavoriteName.getText().toString(), address, currentFavoriteType, location.getLatitude(), location.getLongitude(), -1);
            textAddress.setText(address);

            if (b.containsKey("poi")) {
                textFavoriteName.setText(b.getString("poi"));
            } else {
                textFavoriteName.setText(address);
            }
        } else {
            throw new RuntimeException("Unexpected activity result - requestCode not implemented");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        hideKeyboard();
    }

    private static boolean isPredefinedName(final String name) {
        if (
                name.equals(IBikeApplication.getString("Favorite")) ||
                        name.equals(IBikeApplication.getString("School")) ||
                        name.equals(IBikeApplication.getString("Work")) ||
                        name.equals(IBikeApplication.getString("Home")) ||
                        name.equals(""))
            return true;
        else
            return false;
    }

    public void hideKeyboard() {
        if (textFavoriteName != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(textFavoriteName.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.radioButtonFavorite) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("Favorite"));

            currentFavoriteType = FavoriteListItem.favFav;
        } else if (checkedId == R.id.radioButtonHome) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("Home"));

            currentFavoriteType = FavoriteListItem.favHome;
        } else if (checkedId == R.id.radioButtonSchool) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("School"));

            currentFavoriteType = FavoriteListItem.favSchool;
        } else if (checkedId == R.id.radioButtonWork) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("Work"));

            currentFavoriteType = FavoriteListItem.favWork;
        }
    }
}