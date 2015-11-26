// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

public interface SMRouteListener {
	public void updateTurn(boolean firstElementRemoved);
	public void reachedDestination();
	public void updateRoute();
	public void startRoute();
	public void routeNotFound();
	public void routeRecalculationStarted();
	public void routeRecalculationDone();
	public void routeRecalculationDone(String type);
	public void serverError();
}
