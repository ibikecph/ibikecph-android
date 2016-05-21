package com.spoiledmilk.ibikecph.map.states;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.mapbox.mapboxsdk.api.ILatLng;

import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.map.fragments.DestinationPreviewFragment;
import com.spoiledmilk.ibikecph.map.Geocoder;
import com.spoiledmilk.ibikecph.map.handlers.OverviewMapHandler;
import com.spoiledmilk.ibikecph.search.Address;

/**
 * The user has selected a potential destination location.
 * Created by kraen on 02-05-16.
 */
public class DestinationPreviewState extends MapState {

    Address destinationAddress;

    DestinationPreviewFragment destinationPreviewFragment;

    public DestinationPreviewState() {
        super();
    }

    @Override
    public void transitionTowards(MapState from, FragmentTransaction fragmentTransaction) {
        // Ensure that the correct map handler is active
        if(!(activity.getMapView().getMapHandler() instanceof OverviewMapHandler)) {
            activity.getMapView().setMapViewListener(OverviewMapHandler.class);
        }
        activity.getMapView().setUserLocationEnabled(true);
        activity.getMapView().getUserLocationOverlay().setDrawAccuracyEnabled(true);
    }

    @Override
    public void transitionAway(MapState to, FragmentTransaction fragmentTransaction) {
        activity.getMapView().setUserLocationEnabled(false);
        // Remove the address marker
        // TODO: Consider renaming the method from the generic "address marker" to "temporary .."
        activity.getMapView().removeAddressMarker();
        if(destinationPreviewFragment != null) {
            fragmentTransaction.remove(destinationPreviewFragment);
        }
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
    }

    public void setDestination(Address destinationAddress) {
        this.destinationAddress = destinationAddress;
        activity.getMapView().showAddress(destinationAddress);
        updateDestinationPreviewFragment(destinationAddress);
    }

    public void updateDestinationPreviewFragment(Address a) {
        // Show the infopane
        FragmentManager fragmentManager = activity.getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Prepare the infopane with the address we just got.
        destinationPreviewFragment = new DestinationPreviewFragment();
        fragmentTransaction.add(R.id.topFragment, destinationPreviewFragment);

        // Supply the address
        Bundle arguments = new Bundle();
        arguments.putSerializable("address", a);
        destinationPreviewFragment.setArguments(arguments);

        fragmentTransaction.commit();
    }

    @Override
    public BackPressBehaviour onBackPressed() {
        activity.changeState(BrowsingState.class);
        return BackPressBehaviour.STOP_PROPAGATION;
    }
}
