package com.spoiledmilk.ibikecph.map.states;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;

import com.mapbox.mapboxsdk.api.ILatLng;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.AddressDisplayInfoPaneFragment;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.search.Address;

/**
 * The user has selected a potential destination location.
 * Created by kraen on 02-05-16.
 */
public class DestinationPreviewState extends MapState {

    Address destinationAddress;

    public DestinationPreviewState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from) {
        activity.getMapView().setUserLocationEnabled(true);
        // Hide the info pane container
        activity.findViewById(R.id.infoPaneContainer).setVisibility(View.VISIBLE);
    }

    @Override
    public void transitionAway(MapState to) {
        activity.getMapView().setUserLocationEnabled(false);
        // Remove the address marker
        // TODO: Consider renaming the method from the generic "address marker" to "temporary .."
        activity.getMapView().removeAddressMarker();
        // Hide the info pane container
        activity.findViewById(R.id.infoPaneContainer).setVisibility(View.GONE);
    }

    public void setDestination(ILatLng destinationLatLng) {
        Geocoder.getAddressForLocation(destinationLatLng, new Geocoder.GeocoderCallback() {
            @Override
            public void onSuccess(Address address) {
                DestinationPreviewState.this.setDestination(address);
            }

            @Override
            public void onFailure() {

            }
        });
        // Center the map around the search result.
        activity.getMapView().setCenter(destinationLatLng, true);
    }

    public void setDestination(Address destinationAddress) {
        this.destinationAddress = destinationAddress;
        activity.getMapView().showAddress(destinationAddress);
        activity.getMapView().setCenter(destinationAddress.getLocation());
        showAddressInfoPane(destinationAddress);
    }

    public void showAddressInfoPane(Address a) {
        // Show the infopane
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Prepare the infopane with the address we just got.
        AddressDisplayInfoPaneFragment adp = new AddressDisplayInfoPaneFragment();

        // Supply the address
        Bundle arguments = new Bundle();
        arguments.putSerializable("address", a);
        adp.setArguments(arguments);

        ft.replace(R.id.infoPaneContainer, adp);
        ft.commit();
    }

    @Override
    public void onBackPressed() {
        activity.ensureState(BrowsingState.class);
    }
}
