package dk.kk.ibikecphlib.map.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.states.RouteSelectionState;
import dk.kk.ibikecphlib.search.Address;
import dk.kk.ibikecphlib.util.DB;

import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.favorites.FavoriteListItem;
import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.map.states.RouteSelectionState;
import dk.kk.ibikecphlib.search.Address;

/**
 * This is the Fragment that appears when a user long-presses on the map. It shows the address that the user long-
 * pressed on, and allows her to 1) save a favorite on that address, and 2) navigate to it.
 */
public class DestinationPreviewFragment extends Fragment implements View.OnClickListener {

    private Address address;

    public DestinationPreviewFragment() {
        super();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.destination_preview_fragment, container, false);

        this.address = (Address) getArguments().getSerializable("address");

        if (this.address != null) {
            ((TextView) v.findViewById(R.id.addressNameLabel)).setText(this.address.getPrimaryDisplayString());
            ((TextView) v.findViewById(R.id.addressLabel)).setText(this.address.getSecondaryDisplayString());
        }

        // Set click listeners
        v.findViewById(R.id.btnStartRoute).setOnClickListener(this);
        v.findViewById(R.id.btnAddFavorite).setOnClickListener(this);

        FavoriteListItem a = (new DB(getActivity()).getFavoriteByAddress(this.address));

        if (a != null) {
            updateFavoriteState(v, true);
        } else {
            updateFavoriteState(v, false);
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
    public void onClick(final View v) {
        int i = v.getId();
        if (i == R.id.btnStartRoute) {
            btnStartRouteClicked(v);
        } else if (i == R.id.btnAddFavorite) {
            if (v.findViewById(R.id.btnAddFavorite).getTag().equals("filled")) {
                // This is already a favorite
                final FavoriteListItem favoriteListItem = (new DB(getActivity()).getFavoriteByAddress(this.address));
                if(favoriteListItem != null) {
                    // Start a thread that removes the favorite from the db.
                    Thread saveThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            (new DB(getActivity())).deleteFavorite(favoriteListItem);
                            (new DB(IBikeApplication.getContext())).getFavoritesFromServer(null);
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateFavoriteState(v, false);
                                }
                            });
                        }
                    });
                    saveThread.start();
                }

            } else {
                // This should become a favorite
                final FavoriteListItem favoriteListItem = FavoriteListItem.fromAddress(this.address);

                // Start a thread that saves the favorite to the db.
                Thread saveThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        (new DB(getActivity())).saveFavorite(favoriteListItem, false);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateFavoriteState(v, true);
                            }
                        });
                    }
                });
                saveThread.start();
            }

        }
    }

    private void updateFavoriteState(View v, boolean isFavorite) {
        if(v == null) {
            throw new RuntimeException("Expected a non-null view");
        }

        ImageButton favoriteBtn = (ImageButton) v.findViewById(R.id.btnAddFavorite);
        if(favoriteBtn == null) {
            throw new RuntimeException("Unable to locate the add favorite button.");
        }

        if(isFavorite) {
            favoriteBtn.setImageResource(R.drawable.btn_add_favorite_filled);
            favoriteBtn.setTag("filled");
        } else {
            favoriteBtn.setImageResource(R.drawable.btn_add_favorite);
            favoriteBtn.setTag("notFilled");
        }
    }
}
