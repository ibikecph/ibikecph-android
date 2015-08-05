package com.spoiledmilk.cykelsuperstier;

import android.os.Bundle;
import android.util.Log;

/**
 * Created by jens on 7/23/15.
 */
public class AcceptNewTermsActivity extends com.spoiledmilk.ibikecph.AcceptNewTermsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("JC", "Cykelplanen AcceptNewTermsActivity");

        this.findViewById(com.spoiledmilk.ibikecph.R.id.backgroundLayout).setBackgroundColor(this.getBackgroundColor());

    }

    @Override
    protected int getBackgroundColor() {
        return getResources().getColor(R.color.CPActionBar);
    }

}