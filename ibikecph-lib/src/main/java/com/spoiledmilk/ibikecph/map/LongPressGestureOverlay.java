// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.map;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.SafeDrawOverlay;
import org.osmdroid.views.safecanvas.ISafeCanvas;

import android.content.Context;
import android.view.MotionEvent;

public class LongPressGestureOverlay extends SafeDrawOverlay {

	OnMapLongPressListener listener;

	public LongPressGestureOverlay(OnMapLongPressListener listener, final MapView mapView) {
		super(listener.getContext());

		this.listener = listener;
	}

	@Override
	protected void drawSafe(ISafeCanvas c, MapView osmv, boolean shadow) {
		// No drawing necessary
	}

	@Override
	public boolean onLongPress(final MotionEvent e, MapView mapView) {
		int X = (int) e.getX();
		int Y = (int) e.getY();
		if (listener != null) {
			listener.onMapLongPress(X, Y);
		}

		return super.onLongPress(e, mapView);
	}
}

interface OnMapLongPressListener {
	public void onMapLongPress(int x, int y);

	public Context getContext();
}
