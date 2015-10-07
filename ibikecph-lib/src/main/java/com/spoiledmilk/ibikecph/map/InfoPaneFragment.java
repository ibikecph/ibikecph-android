package com.spoiledmilk.ibikecph.map;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.spoiledmilk.ibikecph.R;

/**
 * Created by jens on 6/1/15.
 */
public abstract class InfoPaneFragment extends Fragment {
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.infopane_empty, container, false);

        return v;
    }
}
