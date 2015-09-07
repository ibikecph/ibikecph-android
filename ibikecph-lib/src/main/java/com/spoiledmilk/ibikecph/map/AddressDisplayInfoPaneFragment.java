package com.spoiledmilk.ibikecph.map;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.DB;

/**
 * This is the Fragment that appears when a user long-presses on the map. It shows the address that the user long-
 * pressed on, and allows her to 1) save a favorite on that address, and 2) navigate to it.
 */
public class AddressDisplayInfoPaneFragment extends InfoPaneFragment implements View.OnClickListener {

    private TextView addressView;
    private Address address;

    public AddressDisplayInfoPaneFragment() {
        super();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_address_display, container, false);

        this.address = (Address) getArguments().getSerializable("address");

        if (this.address.hasSpecialName()) {
            Log.d("DV", "Special name!");
            ((TextView) v.findViewById(R.id.addressNameLabel)).setText(this.address.getStreetAddress());
            ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getPostCodeAndCity());
        } else {
            Log.d("DV", "Ikke special name!");
            ((TextView) v.findViewById(R.id.addressNameLabel)).setText(this.address.getDisplayName());
            ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getPostCodeAndCity());
        }

        // Set click listeners
        v.findViewById(R.id.btnStartRoute).setOnClickListener(this);
        v.findViewById(R.id.btnAddFavorite).setOnClickListener(this);

        ((TextView) v.findViewById(R.id.newRouteText)).setText(IbikeApplication.getString("new_route"));
        if (IbikeApplication.getAppName().equals("Cykelplanen")) {
            ((TextView) v.findViewById(R.id.newRouteText)).setTextColor(getResources().getColor(R.color.CPActionBar));

        }


        return v;
    }

    public void btnStartRouteClicked(View v) {
        if (IbikeApplication.getService().hasValidLocation()) {
            Address a = (Address) getArguments().getSerializable("address");

            ((MapActivity) this.getActivity()).mapView.showRoute(a);
        } else {
            Toast.makeText(IbikeApplication.getContext(), IbikeApplication.getString("error_no_gps_location"), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnStartRoute) {
            btnStartRouteClicked(v);
        } else if (i == R.id.btnAddFavorite) {
            Log.d("JC", "Saving favorite");

            final FavoritesData favoritesData = FavoritesData.fromAddress(this.address);

            // Start a thread that saves the favorite to the db.
            Thread saveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    (new DB(getActivity())).saveFavorite(favoritesData, getActivity(), false);

                }
            });
            saveThread.start();

            try {
                saveThread.join();
                ((ImageButton) v.findViewById(R.id.btnAddFavorite)).setImageResource(R.drawable.btn_add_favorite_filled);
                ((ImageButton) v.findViewById(R.id.btnAddFavorite)).setOnClickListener(null);

            } catch (Exception e) {
                e.getLocalizedMessage();
            }

        }
    }
}
