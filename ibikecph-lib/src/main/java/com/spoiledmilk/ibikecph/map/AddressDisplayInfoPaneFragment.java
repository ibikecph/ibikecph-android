package com.spoiledmilk.ibikecph.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.search.Address;

/**
 * This is the Fragment that appears when a user long-presses on the map. It shows the address that the user long-
 * pressed on, and allows her to 1) save a favorite on that address, and 2) navigate to it.
 */
public class AddressDisplayInfoPaneFragment extends InfoPaneFragment {

    private TextView addressView;

    public AddressDisplayInfoPaneFragment() {
        super();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_address_display, container, false);

        Address a = (Address) getArguments().getSerializable("address");

        ((TextView) v.findViewById(R.id.addressLabel)).setText(a.getStreetAddress() + "\n" + a.getPostCodeAndCity());

        return v;
    }



}
