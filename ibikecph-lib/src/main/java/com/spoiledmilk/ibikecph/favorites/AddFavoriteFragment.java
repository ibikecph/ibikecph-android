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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

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
    private FavoritesData favoritesData = null;
    protected String currentFavoriteType = "";
    private AlertDialog dialog;
    boolean isTextChanged = false;

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
                getActivity().startActivityForResult(i, 2);
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
            if (favoritesData != null && textFavoriteName.getText().toString() != null
                    && !textFavoriteName.getText().toString().trim().equals("")) {
                if (new DB(getActivity()).favoritesForName(textFavoriteName.getText().toString().trim()) == 0) {
                    favoritesData.setName(textFavoriteName.getText().toString());
                    favoritesData.setSubSource(currentFavoriteType);
                    String st = favoritesData.getName() + " - (" + favoritesData.getLatitude() + "," + favoritesData.getLongitude()
                            + ")";


                    // TODO: Change this to the implementation described here
                    // https://developers.google.com/analytics/devguides/collection/android/v4/#send-an-event
                    // IBikeApplication.getTracker().sendEvent("Favorites", "Save", st, (long) 0);

                    Thread saveThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            (new DB(getActivity())).saveFavorite(favoritesData, getActivity(), false);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i("JC", "AddFavoriteFragment on address result");

        if (data != null) {
            Bundle b = data.getExtras();
            Address addresss = (Address) b.getSerializable("addressObject");
            String address = "";
            if (b.containsKey("address") && b.containsKey("lat") && b.containsKey("lon")) {
                try {
                    address = AddressParser.textFromBundle(b).replace("\n", "");
                } catch (Exception ex) {
                }

                /*if (address == null) {
                    address = "";
                } else if (address.equals("") && addresss != null) {
                    address = addresss.getDisplayName();
                }

                favoritesData = new FavoritesData(textFavoriteName.getText().toString(), address, currentFavoriteType, b.getDouble("lat"),
                        b.getDouble("lon"), -1);*/

                if (!address.equals("") && address != null) {
                    favoritesData = new FavoritesData(textFavoriteName.getText().toString(), address, currentFavoriteType, b.getDouble("lat"),
                            b.getDouble("lon"), -1);
                } else if (address.equals("") && address != null) {
                    address = Address.street_s + " " + Address.houseNumber_s + ", " + Address.zip_s + " " + Address.city_s;
                    favoritesData = new FavoritesData(textFavoriteName.getText().toString(), address, currentFavoriteType, Address.lat_s,
                            Address.lon_s, -1);
                } else {
                    address = addresss.getDisplayName();
                    favoritesData = new FavoritesData(textFavoriteName.getText().toString(), address, currentFavoriteType, b.getDouble("lat"),
                            b.getDouble("lon"), -1);
                }
                textAddress.setText(address);


                if (b.containsKey("poi")) {
                    textFavoriteName.setText(b.getString("poi"));
                } else {
                    textFavoriteName.setText(address);
                }
            }

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

            currentFavoriteType = FavoritesData.favFav;
        } else if (checkedId == R.id.radioButtonHome) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("Home"));

            currentFavoriteType = FavoritesData.favHome;
        } else if (checkedId == R.id.radioButtonSchool) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("School"));

            currentFavoriteType = FavoritesData.favSchool;
        } else if (checkedId == R.id.radioButtonWork) {
            if (isPredefinedName(textFavoriteName.getText().toString()))
                textFavoriteName.setText(IBikeApplication.getString("Work"));

            currentFavoriteType = FavoritesData.favWork;
        }
    }
}