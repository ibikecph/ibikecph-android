package com.spoiledmilk.ibikecph.map.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.states.RouteSelectionState;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.DB;

/**
 * This is the Fragment that appears when a user long-presses on the map. It shows the address that the user long-
 * pressed on, and allows her to 1) save a favorite on that address, and 2) navigate to it.
 */
public class DestinationPreviewFragment extends Fragment implements View.OnClickListener {

    private Address address;
    /**
     * @deprecated Let's not use static fields like this.
     */
    public static String name = "";

    public DestinationPreviewFragment() {
        super();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.destination_preview_fragment, container, false);

        this.address = (Address) getArguments().getSerializable("address");

        if (this.address != null) {
            if (this.address.hasSpecialName()) {
                Log.d("DV", "Special name!");
                ((TextView) v.findViewById(R.id.addressNameLabel)).setText(this.address.getStreetAddress());
                ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getPostCodeAndCity());
                name = this.address.getStreetAddress();
            } else {
                Log.d("DV", "Ikke special name!");
                ((TextView) v.findViewById(R.id.addressNameLabel)).setText(this.address.getDisplayName());
                ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getPostCodeAndCity());
                name = this.address.getDisplayName();
            }
        }

        // Set click listeners
        v.findViewById(R.id.btnStartRoute).setOnClickListener(this);
        v.findViewById(R.id.btnAddFavorite).setOnClickListener(this);

        FavoritesData a = (new DB(getActivity()).getFavoriteByAddressForInfoPane(FavoritesData.fromAddress(this.address).getAdress()));

        if (a != null) {
            v.findViewById(R.id.btnAddFavorite).setTag("filled");
            ((ImageButton) v.findViewById(R.id.btnAddFavorite)).setImageResource(R.drawable.btn_add_favorite_filled);
            ((TextView) v.findViewById(R.id.addressNameLabel)).setText(a.getName());
            if (this.address.hasSpecialName()) {
                Log.d("DV", "Special name!");
                ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getStreetAddress() + "\n" + this.address.getPostCodeAndCity());
            } else {
                Log.d("DV", "Ikke special name!");
                ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getDisplayName() + "\n" + this.address.getPostCodeAndCity());
            }
        } else {
            v.findViewById(R.id.btnAddFavorite).setTag("notFilled");
            ((ImageButton) v.findViewById(R.id.btnAddFavorite)).setImageResource(R.drawable.btn_add_favorite);
        }

        ((TextView) v.findViewById(R.id.startRouteText)).setText(IBikeApplication.getString("new_route"));

        return v;
    }

    public void btnStartRouteClicked(View v) {
        if (IBikeApplication.getService().hasValidLocation()) {
            if (getActivity() instanceof MapActivity) {
                Address a = (Address) getArguments().getSerializable("address");
                MapActivity activity = (MapActivity) getActivity();
                RouteSelectionState state = activity.changeState(RouteSelectionState.class);
                state.setDestination(a);
            }
        } else {
            Toast.makeText(IBikeApplication.getContext(), IBikeApplication.getString("error_no_gps_location"), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btnStartRoute) {
            btnStartRouteClicked(v);
        } else if (i == R.id.btnAddFavorite) {
            Log.d("JC", "Saving favorite");

            if (v.findViewById(R.id.btnAddFavorite).getTag().toString().equals("filled")) {
                Log.d("DV", "notFilled!");

                final FavoritesData favoritesData = FavoritesData.fromAddress(this.address);
                FavoritesData a = (new DB(getActivity()).getFavoriteByAddressForInfoPane(FavoritesData.fromAddress(this.address).getAdress()));
                favoritesData.setApiId(a.getApiId());

                // Start a thread that removes the favorite from the db.
                Thread saveThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        (new DB(getActivity())).deleteFavorite(favoritesData, getActivity());
                        (new DB(IBikeApplication.getContext())).getFavoritesFromServer(IBikeApplication.getContext(), null);
                    }
                });
                saveThread.start();

                try {
                    saveThread.join();
                    ((ImageButton) v.findViewById(R.id.btnAddFavorite)).setImageResource(R.drawable.btn_add_favorite);
                    v.findViewById(R.id.btnAddFavorite).setTag("notFilled");
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }
            } else {
                Log.d("DV", "Filled!");
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
                    v.findViewById(R.id.btnAddFavorite).setTag("filled");
                    //((ImageButton) v.findViewById(R.id.btnAddFavorite)).setOnClickListener(null);

                } catch (Exception e) {
                    e.getLocalizedMessage();
                }

            }

        }
    }
}
