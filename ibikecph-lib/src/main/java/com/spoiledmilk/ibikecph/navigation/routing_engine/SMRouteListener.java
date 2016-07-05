// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

public interface SMRouteListener {
	void reachedDestination();
	void updateRoute();
	void startRoute();
	void routeNotFound();
	void routeRecalculationStarted();
	void routeRecalculationDone();
	void serverError();
}
