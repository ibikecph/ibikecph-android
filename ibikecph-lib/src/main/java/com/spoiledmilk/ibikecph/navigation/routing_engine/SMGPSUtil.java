// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;

import com.spoiledmilk.ibikecph.util.Util;

public class SMGPSUtil {

	static final double DEG_TO_RAD = 0.017453292519943295769236907684886;
	static final double EARTH_RADIUS_IN_METERS = 6372797.560856;

	static double DegreesToRadians(double degrees) {
		return degrees * Math.PI / 180;
	};

	static double RadiansToDegrees(double radians) {
		return radians * 180 / Math.PI;
	};

	// Calculates distance between point C and arc AB in radians
	// dA - distance between point C and point A in radians
	// dB - distance between point C and point B in radians
	// dAB - length of arc AB in radians
	static double distanceFromArc(double dA, double dB, double dAB) {
		// In spherical trinagle ABC
		// a is length of arc BC, that is dB
		// b is length of arc AC, that is dA
		// c is length of arc AB, that is dAB
		// We rename parameters so following formulas are more clear:
		double a = dB;
		double b = dA;
		double c = dAB;

		// First, we calculate angles alpha and beta in spherical triangle ABC
		// and based on them we decide how to calculate the distance:
		if (Math.sin(b) * Math.sin(c) == 0.0 || Math.sin(c) * Math.sin(a) == 0.0) {
			// It probably means that one of distance is n*pi, which gives around 20000km for n = 1,
			// unlikely for Denmark, so we should be fine.
			return -1.0;
		}

		double alpha = Math.acos((Math.cos(a) - Math.cos(b) * Math.cos(c)) / (Math.sin(b) * Math.sin(c)));
		double beta = Math.acos((Math.cos(b) - Math.cos(c) * Math.cos(a)) / (Math.sin(c) * Math.sin(a)));

		// It is possible that both sinuses are too small so we can get nan when dividing with them
		if (Double.isNaN(alpha) || Double.isNaN(beta)) {
			// double cosa = cos(a);
			// double cosbc = cos(b) * cos(c);
			// double minus1 = cosa - cosbc;
			// double sinbc = sin(b) * sin(c);
			// double div1 = minus1 / sinbc;
			//
			// double cosb = cos(b);
			// double cosca = cos(a) * cos(c);
			// double minus2 = cosb - cosca;
			// double sinca = sin(a) * sin(c);
			// double div2 = minus2 / sinca;

			return -1.0;
		}

		// If alpha or beta are zero or pi, it means that C is on the same circle as arc AB,
		// we just need to figure out if it is between AB:
		if (alpha == 0.0 || beta == 0.0) {
			return (dA + dB > dAB) ? Math.min(dA, dB) : 0.0;
		}

		// If alpha is obtuse and beta is acute angle, then
		// distance is equal to dA:
		if (alpha > Math.PI / 2 && beta < Math.PI / 2)
			return dA;

		// Analogously, if beta is obtuse and alpha is acute angle, then
		// distance is equal to dB:
		if (beta > Math.PI / 2 && alpha < Math.PI / 2)
			return dB;

		// If both alpha and beta are acute or both obtuse or one of them (or both) are right,
		// distance is the height of the spherical triangle ABC:

		// Again, unlikely, since it would render at least pi/2*EARTH_RADIUS_IN_METERS, which is too much.
		if (Math.cos(a) == 0.0)
			return -1;

		double x = Math.atan(-1.0 / Math.tan(c) + (Math.cos(b) / (Math.cos(a) * Math.sin(c))));

		// Similar to previous edge cases...
		if (Math.cos(x) == 0.0)
			return -1.0;

		return Math.acos(Math.cos(a) / Math.cos(x));
	}

	// dA - distance between point C and point A in radians
	// dB - distance between point C and point B in radians
	// dAB - length of arc AB in radians
	static double distanceFromPointOnArc(double dA, double dB, double dAB) {
		// In spherical trinagle ABC
		// a is length of arc BC, that is dB
		// b is length of arc AC, that is dA
		// c is length of arc AB, that is dAB
		// We rename parameters so following formulas are more clear:
		double a = dB;
		double b = dA;
		double c = dAB;

		// First, we calculate angles alpha and beta in spherical triangle ABC
		// and based on them we decide how to calculate the distance:
		if (Math.sin(b) * Math.sin(c) == 0.0 || Math.sin(c) * Math.sin(a) == 0.0) {
			// It probably means that one of distance is n*pi, which gives around 20000km for n = 1,
			// unlikely for Denmark, so we should be fine.
			return -1.0;
		}

		double alpha = Math.acos((Math.cos(a) - Math.cos(b) * Math.cos(c)) / (Math.sin(b) * Math.sin(c)));
		double beta = Math.acos((Math.cos(b) - Math.cos(c) * Math.cos(a)) / (Math.sin(c) * Math.sin(a)));

		// It is possible that both sinuses are too small so we can get nan when dividing with them
		if (Double.isNaN(alpha) || Double.isNaN(beta)) {
			return -1.0;
		}

		// If alpha or beta are zero or pi, it means that C is on the same circle as arc AB,
		// we just need to figure out if it is between AB:
		if (alpha == 0.0 || beta == 0.0) {
			return (dA + dB > dAB) ? Math.min(dA, dB) : 0.0;
		}

		// If alpha is obtuse and beta is acute angle, then
		// distance is equal to dA:
		if (alpha > Math.PI / 2 && beta < Math.PI / 2)
			return -1;

		// Analogously, if beta is obtuse and alpha is acute angle, then
		// distance is equal to dB:
		if (beta > Math.PI / 2 && alpha < Math.PI / 2)
			return -1;

		// Again, unlikely, since it would render at least pi/2*EARTH_RADIUS_IN_METERS, which is too much.
		if (Math.cos(a) == 0.0)
			return -1;

		double x = Math.atan(-1.0 / Math.tan(c) + (Math.cos(b) / (Math.cos(a) * Math.sin(c))));

		return x;
	}

	// Length of arc AB in radians.
	static double arcInRadians(Location A, Location B) {
		double latitudeArc = (A.getLatitude() - B.getLatitude()) * DEG_TO_RAD;
		double longitudeArc = (A.getLongitude() - B.getLongitude()) * DEG_TO_RAD;
		double latitudeH = Math.sin(latitudeArc * 0.5);
		latitudeH *= latitudeH;
		double lontitudeH = Math.sin(longitudeArc * 0.5);
		lontitudeH *= lontitudeH;
		double tmp = Math.cos(A.getLatitude() * DEG_TO_RAD) * Math.cos(B.getLatitude() * DEG_TO_RAD);
		return 2.0 * Math.asin(Math.sqrt(latitudeH + tmp * lontitudeH));
	}

	// Calculates distance between location C and path AB in meters.
	public static double distanceFromLineInMeters(Location C, Location A, Location B) {
		double dA = arcInRadians(C, A);
		double dB = arcInRadians(C, B);
		double dAB = arcInRadians(A, B);

		if (dA == 0)
			return 0;
		if (dB == 0)
			return 0;
		if (dAB == 0)
			return dA;

		return EARTH_RADIUS_IN_METERS * distanceFromArc(dA, dB, dAB);
	}

	// Finds the closest point from point C on arc AB.
	public static Location closestCoordinate(Location C, Location A, Location B) {
		double dA = arcInRadians(C, A);
		double dB = arcInRadians(C, B);
		double dAB = arcInRadians(A, B);

		if (dA == 0)
			return A;
		if (dB == 0)
			return B;
		if (dAB == 0)
			return A;

		double x = distanceFromPointOnArc(dA, dB, dAB);

		if (x < 0d) {
			x = distanceFromLineInMeters(C, A, B);
			if (x < 0d || x > 20d) {
				if (C.distanceTo(A) <= 20) {
					return A;
				} else if (C.distanceTo(B) <= 20) {
					return B;
				} else {
					return C;
				}
			} else {
				Location ret = closestCoordinateOnLine(C, A, B);
				if (ret.distanceTo(C) <= 20) {
					return ret;
				} else {
					return C;
				}
			}
		}

		Location ret = Util.locationFromCoordinates(B.getLatitude() - (B.getLatitude() - A.getLatitude()) * x / dAB,
				B.getLongitude() - (B.getLongitude() - A.getLongitude()) * x / dAB);

		// Log.d("closestCoord B = " + B.getLatitude() + " , " + B.getLongitude() + " A = " + A.getLatitude() + " , " +
		// A.getLongitude()
		// + " C = " + C.getLatitude() + " , " + C.getLongitude() + " x = " + x);

		if (ret.distanceTo(C) > 20)
			return C;

		return ret;
	}

	public static Location closestCoordinateOnLine(Location C, Location A, Location B) {
		double apx = C.getLatitude() - A.getLatitude();
		double apy = C.getLongitude() - A.getLongitude();
		double abx = B.getLatitude() - A.getLatitude();
		double aby = B.getLongitude() - A.getLongitude();

		double ab2 = abx * abx + aby * aby;
		double ap_ab = apx * abx + apy * aby;
		double t = ap_ab / ab2;
		// if (clampToSegment) {
		// if (t < 0) {
		// t = 0;
		// } else if (t > 1) {
		// t = 1;
		// }
		// }
		Location ret = Util.locationFromCoordinates(A.getLatitude() + abx * t, A.getLongitude() + aby * t);
		return ret;
	}

	// Calculates bearing between two locations
	public static double bearingBetween(Location startLocation, Location endLocation) {
		double lat1 = DegreesToRadians(startLocation.getLatitude());
		double lon1 = DegreesToRadians(startLocation.getLongitude());

		double lat2 = DegreesToRadians(endLocation.getLatitude());
		double lon2 = DegreesToRadians(endLocation.getLongitude());

		double dLon = lon2 - lon1;

		double y = Math.sin(dLon) * Math.cos(lat2);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
		double radiansBearing = Math.atan2(y, x);

		return RadiansToDegrees(radiansBearing);
	}
}
