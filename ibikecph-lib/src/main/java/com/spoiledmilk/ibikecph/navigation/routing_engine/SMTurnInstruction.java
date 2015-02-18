// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import java.util.Locale;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.util.LOG;

public class SMTurnInstruction {

	// ***** constants and types

	private static final int kVehicleBike = 1;

	// private static final int kVehicleWalk = 2;
	// private static final int kVehicleFerry = 3;
	// private static final int kVehicleTrain = 4;

	public enum TurnDirection {
		NoTurn(0), // Give no instruction at all
		GoStraight(1), TurnSlightRight(2), TurnRight(3), TurnSharpRight(4), UTurn(5), TurnSharpLeft(6), TurnLeft(7), TurnSlightLeft(8), ReachViaPoint(
				9), HeadOn(10), EnterRoundAbout(11), LeaveRoundAbout(12), StayOnRoundAbout(13), StartAtEndOfStreet(14), ReachedYourDestination(
				15), StartPushingBikeInOneway(16), StopPushingBikeInOneway(17), ReachingDestination(100), Station(18);

		TurnDirection(int i) {
			this.type = i;
		}

		private int type;

		public int getNumericType() {
			return type;
		}
	};

	int[] iconsSmall = { -1, R.drawable.up, R.drawable.right_ward, R.drawable.right, R.drawable.right, R.drawable.u_turn, R.drawable.left,
			R.drawable.left, R.drawable.left_ward, R.drawable.location, R.drawable.up, R.drawable.roundabout, R.drawable.roundabout,
			R.drawable.roundabout, R.drawable.up, R.drawable.flag, R.drawable.push_bike, R.drawable.bike, R.drawable.near_destination };

	int[] iconsLarge = { -1, R.drawable.white_up, R.drawable.white_right_ward, R.drawable.white_right, R.drawable.white_right,
			R.drawable.white_u_turn, R.drawable.white_left, R.drawable.white_left, R.drawable.white_left_ward, R.drawable.white_location,
			R.drawable.white_up, R.drawable.white_roundabout, R.drawable.white_roundabout, R.drawable.white_roundabout,
			R.drawable.white_up, R.drawable.white_flag, R.drawable.white_push_bike, R.drawable.white_bike,
			R.drawable.white_near_destination };

	// ***** fields

	public int vehicle;
	private int iconResource;
	public TurnDirection drivingDirection = TurnDirection.NoTurn;
	String ordinalDirection = "";
	public String wayName = "";
	public int lengthInMeters = 0;
	int timeInSeconds = 0;
	String lengthWithUnit = "";
	/**
	 * Length to next turn in units (km or m) This value will not auto update
	 */
	String fixedLengthWithUnit;
	String directionAbrevation; // N: north, S: south, E: east, W: west, NW:
								// North West, ...
	public float azimuth;
	public int waypointsIndex;
	Location loc;
	public String descriptionString;
	public String fullDescriptionString;
	boolean isFakeInstruction = false;
	public boolean plannedForRemoving = false;
	double lastD = -1;

	public SMTurnInstruction() {

	}

	// used only for the instructions adapter to add one another view
	public SMTurnInstruction(boolean isFakeInstruction) {
		this.isFakeInstruction = isFakeInstruction;
	}

	public void convertToStation(String stationName, int iconResource) {
		wayName = stationName;
		this.iconResource = iconResource;
		drivingDirection = TurnDirection.Station;
	}

	public Location getLocation() {
		return loc;
	}

	public Drawable smallDirectionIcon(Context context) throws Exception {
		if (drivingDirection == TurnDirection.Station)
			return context.getResources().getDrawable(iconResource);
		else
			return context.getResources().getDrawable(iconsSmall[drivingDirection.ordinal()]);

	}

	public Drawable largeDirectionIcon(Context context) {
		if (drivingDirection == TurnDirection.NoTurn)
			return null;
		else if (drivingDirection == TurnDirection.Station)
			return context.getResources().getDrawable(iconResource);
		else
			return context.getResources().getDrawable(iconsLarge[drivingDirection.ordinal()]);
	}

	public int getDirectionImageResource() {
		if (drivingDirection == TurnDirection.NoTurn)
			return -1;
		else if (drivingDirection == TurnDirection.Station)
			return iconResource;
		else
			return iconsLarge[drivingDirection.ordinal()];
	}

	public int getBlackDirectionImageResource() {
		if (drivingDirection == TurnDirection.NoTurn)
			return -1;
		else if (drivingDirection == TurnDirection.Station)
			return iconResource;
		else
			return iconsSmall[drivingDirection.ordinal()];
	}

	// Returns only string representation of the driving direction
	void generateDescriptionString() {
		switch (drivingDirection) {
		case Station:
			descriptionString = wayName;
			break;
		case EnterRoundAbout:
			descriptionString = String.format(IbikeApplication.getString("direction_" + drivingDirection.ordinal()).replace("%@", "@s"),
					IbikeApplication.getString("direction_number_" + ordinalDirection));
			break;
		default:
			descriptionString = IbikeApplication.getString("direction_" + drivingDirection.ordinal());

		}

		if (vehicle > kVehicleBike) {
			String v = "vehicle_" + vehicle;
			descriptionString = IbikeApplication.getString(v) + ": " + descriptionString;
		}
	}

	void generateStartDescriptionString() {
		switch (drivingDirection) {
		case Station:
			descriptionString = wayName;
			break;
		case NoTurn:
		case ReachedYourDestination:
		case ReachingDestination:
			descriptionString = IbikeApplication.getString("first_direction_" + drivingDirection.ordinal());
			break;
		case EnterRoundAbout:
			descriptionString = String.format(
					IbikeApplication.getString("first_direction_" + drivingDirection.ordinal()).replace("%@", "@s"), IbikeApplication
							.getString("direction_" + directionAbrevation).replace("%@", "@s"), IbikeApplication
							.getString("direction_number_" + ordinalDirection));
			break;
		default:
			String firstDirection = IbikeApplication.getString("first_direction_" + drivingDirection.ordinal());
			firstDirection = firstDirection.replace("%@", "%s");
			String secondDirection = IbikeApplication.getString("direction_" + directionAbrevation);
			LOG.d("First direction = " + firstDirection + " Second direction = " + secondDirection);
			descriptionString = String.format(firstDirection, secondDirection);
		}

		if (vehicle > kVehicleBike) {
			String v = "vehicle_" + vehicle;
			descriptionString = IbikeApplication.getString(v) + ": " + descriptionString;
		}
	}

	// Returns string representation of the driving direction including wayname
	public String generateFullDescriptionString() {
		if (drivingDirection == TurnDirection.Station)
			fullDescriptionString = wayName;
		else {
			fullDescriptionString = IbikeApplication.getString("direction_" + drivingDirection.ordinal());

			if (drivingDirection != TurnDirection.NoTurn && drivingDirection != TurnDirection.ReachedYourDestination
					&& drivingDirection != TurnDirection.ReachingDestination) {
				fullDescriptionString += " " + wayName;
			}
			// else if (drivingDirection == TurnDirection.ReachedYourDestination
			// || drivingDirection ==
			// TurnDirection.ReachingDestination) {
			// fullDescriptionString = IbikeApplication.getString("arrival");
			// }

			if (vehicle > kVehicleBike) {
				String v = "vehicle_" + vehicle;
				fullDescriptionString = IbikeApplication.getString(v) + ": " + fullDescriptionString;
			}
		}
		return fullDescriptionString;
	}

	public String getShortDescriptionString() {
		String ret = "";
		if (drivingDirection == TurnDirection.Station)
			ret = wayName;
		else {
			if (drivingDirection != TurnDirection.NoTurn && drivingDirection != TurnDirection.ReachedYourDestination
					&& drivingDirection != TurnDirection.ReachingDestination) {
				ret = wayName;
			}
			if (vehicle > kVehicleBike) {
				String v = "vehicle_" + vehicle;
				ret = IbikeApplication.getString(v) + ": " + ret;
			}
		}
		return ret;
	}

	// Full textual representation of the object, used mainly for debugging
	@Override
	public String toString() {
		if (drivingDirection == TurnDirection.Station)
			return wayName;
		else
			return String.format(Locale.US, "%s %s [SMTurnInstruction: %d, %d, %s, %s, %f, (%f, %f)]", descriptionString, wayName,
					lengthInMeters, timeInSeconds, lengthWithUnit, directionAbrevation, azimuth, getLocation().getLatitude(), getLocation()
							.getLongitude());
	}

	public String getPrefix() {
		String ret = "";
		if (vehicle > kVehicleBike) {
			String v = "vehicle_" + vehicle;
			ret = IbikeApplication.getString(v) + ": ";
		}
		return ret;
	}
}
