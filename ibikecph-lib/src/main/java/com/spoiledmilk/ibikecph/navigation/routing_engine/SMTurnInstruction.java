// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;

import java.util.Locale;

import static com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute.TransportationType.isPublicTransportation;

public class SMTurnInstruction {

	// ***** constants and types

	public enum TurnDirection {
		NoTurn(0), // Give no instruction at all
		GoStraight(1),
		TurnSlightRight(2),
		TurnRight(3),
		TurnSharpRight(4),
		UTurn(5),
		TurnSharpLeft(6),
		TurnLeft(7),
		TurnSlightLeft(8),
		ReachViaPoint(9),
		HeadOn(10),
		EnterRoundAbout(11),
		LeaveRoundAbout(12),
		StayOnRoundAbout(13),
		StartAtEndOfStreet(14),
		ReachedYourDestination(15),
		StartPushingBikeInOneWay(16),
		StopPushingBikeInOneWay(17),
		GetOnPublicTransportation(18),
		GetOffPublicTransportation(19),
		ReachingDestination(100);

		TurnDirection(int i) {
			this.type = i;
		}

		private int type;

		/**
		 * Translate a turn direction into a human understandable direction.
		 * @return
		 */
		public String toDisplayString() {
			return toDisplayString(false);
		}

		/**
		 * Translate a turn direction into a human understandable direction.
		 * @param isFirst is this the first direction in a route?
         * @return
         */
		public String toDisplayString(boolean isFirst) {
			if (isFirst) {
				return IBikeApplication.getString("first_direction_" + type);
			} else {
				return IBikeApplication.getString("direction_" + type);
			}
		}
	};

	protected int[] iconsSmall = { 0, R.drawable.up, R.drawable.right_ward, R.drawable.right, R.drawable.right, R.drawable.u_turn, R.drawable.left,
			R.drawable.left, R.drawable.left_ward, R.drawable.location, R.drawable.up, R.drawable.roundabout, R.drawable.roundabout,
			R.drawable.roundabout, R.drawable.up, R.drawable.flag, R.drawable.push_bike, R.drawable.bike, R.drawable.near_destination };

	protected int[] iconsLarge = { 0, R.drawable.white_up, R.drawable.white_right_ward, R.drawable.white_right, R.drawable.white_right,
			R.drawable.white_u_turn, R.drawable.white_left, R.drawable.white_left, R.drawable.white_left_ward, R.drawable.white_location,
			R.drawable.white_up, R.drawable.white_roundabout, R.drawable.white_roundabout, R.drawable.white_roundabout,
			R.drawable.white_up, R.drawable.white_flag, R.drawable.white_push_bike, R.drawable.white_bike,
			R.drawable.white_near_destination };

	// ***** fields

	public TurnDirection drivingDirection = TurnDirection.NoTurn;
	TurnDirection secondaryDirection = null;
	public String wayName = "";
	public int lengthInMeters = 0;
	int timeInSeconds = 0;
	String lengthWithUnit = "";
	/**
	 * Length to next turn in units (km or m) This value will not auto update
	 */
	String fixedLengthWithUnit;
	public String directionAbrevation; // N: north, S: south, E: east, W: west, NW:
									   // North West, ...
	public float azimuth;
	public int waypointsIndex;
	Location loc;
	public String descriptionString;
	public String fullDescriptionString;
	public boolean plannedForRemoving = false;
	double lastD = -1;

	public SMTurnInstruction() {

	}

	public SMRoute.TransportationType transportType;

	public SMTurnInstruction(JsonNode instructionNode) {
		// Splitting on a dash an using the first value as an integer to indicate
		// the direction.
		String[] directionIndices = instructionNode.get(0).asText().split("-");

		int directionIndex = Integer.valueOf(directionIndices[0]);
		if (directionIndex < SMTurnInstruction.TurnDirection.values().length) {
			drivingDirection = SMTurnInstruction.TurnDirection.values()[directionIndex];
			if (directionIndices.length > 1) {
				int secondaryDrivingDirection = Integer.valueOf(directionIndices[1]);
				secondaryDirection = SMTurnInstruction.TurnDirection.values()[secondaryDrivingDirection];
			} else {
				secondaryDirection = null;
			}

			wayName = instructionNode.get(1).asText();
			if (wayName.matches("\\{.+\\:.+\\}"))
				wayName = IBikeApplication.getString(wayName);
			wayName = wayName.replaceAll("&#39;", "'");
			timeInSeconds = instructionNode.get(4).asInt();
			if (instructionNode.size() > 8) {
				int vehicle = instructionNode.get(8).asInt();
				if(vehicle == 1) {
					transportType = SMRoute.TransportationType.BIKE;
				} else if(vehicle == 2) {
					transportType = SMRoute.TransportationType.WALK;
				} else if(vehicle == 3) {
					transportType = SMRoute.TransportationType.F;
				} else if(vehicle == 4) {
					transportType = SMRoute.TransportationType.TOG;
				}

			}
			directionAbrevation = instructionNode.get(6).asText();
			azimuth = (float) instructionNode.get(7).asDouble();

			generateFullDescriptionString();
			waypointsIndex = instructionNode.get(3).asInt();
		}
	}

	public double getTransitionDistance() {
		if(isPublicTransportation(transportType)) {
			return 20d;
		} else {
			return 10d;
		}
	}

	public Location getLocation() {
		return loc;
	}

	public int getSmallDirectionResourceId() {
		if (drivingDirection == TurnDirection.GetOnPublicTransportation ||
			drivingDirection == TurnDirection.GetOffPublicTransportation) {
			return transportType.getDrawableId();
		} else {
			return iconsSmall[drivingDirection.ordinal()];
		}

	}

	public int getLargeDirectionResourceId() {
		if (drivingDirection == TurnDirection.GetOnPublicTransportation ||
			drivingDirection == TurnDirection.GetOffPublicTransportation) {
			return transportType.getDrawableId();
		} else {
			return iconsLarge[drivingDirection.ordinal()];
		}
	}

	// Returns only string representation of the driving direction
	void generateDescriptionString() {
		switch (drivingDirection) {
			case GetOnPublicTransportation:
			case GetOffPublicTransportation:
				descriptionString = wayName;
				break;
			case EnterRoundAbout:
				descriptionString = String.format(drivingDirection.toDisplayString().replace("%@", "@s"),
						IBikeApplication.getString("direction_number_" + secondaryDirection));
				break;
			default:
				descriptionString = drivingDirection.toDisplayString();

		}
		descriptionString = getPrefix() + descriptionString;
	}

	void generateStartDescriptionString() {
		switch (drivingDirection) {
			case GetOnPublicTransportation:
			case GetOffPublicTransportation:
				descriptionString = wayName;
				break;
			case NoTurn:
			case ReachedYourDestination:
			case ReachingDestination:
				descriptionString = drivingDirection.toDisplayString(true);
				break;
			case EnterRoundAbout:
				descriptionString = String.format(
					drivingDirection.toDisplayString(true).replace("%@", "%s"),
					IBikeApplication.getString("direction_" + directionAbrevation).replace("%@", "@s"),
					IBikeApplication.getString("direction_number_" + secondaryDirection)
				);
				break;
			default:
				String firstDirection = drivingDirection.toDisplayString(true);
				firstDirection = firstDirection.replace("%@", "%s");
				String secondDirection = IBikeApplication.getString("direction_" + directionAbrevation);
				descriptionString = String.format(firstDirection, secondDirection);
		}
		descriptionString = getPrefix() + descriptionString;
	}

	public void generateFullDescriptionString() {
		if (drivingDirection == TurnDirection.GetOnPublicTransportation ||
			drivingDirection == TurnDirection.GetOffPublicTransportation)
			fullDescriptionString = wayName;
		else {
			fullDescriptionString = drivingDirection.toDisplayString();

			if (drivingDirection != TurnDirection.NoTurn &&
				drivingDirection != TurnDirection.ReachedYourDestination &&
				drivingDirection != TurnDirection.ReachingDestination) {
				fullDescriptionString += " " + wayName;
			}
			fullDescriptionString = getPrefix() + fullDescriptionString;
		}
	}

	// Full textual representation of the object, used mainly for debugging
	@Override
	public String toString() {
		if (drivingDirection == TurnDirection.GetOnPublicTransportation ||
			drivingDirection == TurnDirection.GetOffPublicTransportation)
			return wayName;
		else
			return String.format(Locale.US, "%s %s [SMTurnInstruction: %d, %d, %s, %s, %f, (%f, %f)]", descriptionString, wayName,
					lengthInMeters, timeInSeconds, lengthWithUnit, directionAbrevation, azimuth, getLocation().getLatitude(), getLocation()
							.getLongitude());
	}

	public String getPrefix() {
		if (transportType != null && transportType != SMRoute.TransportationType.BIKE) {
			return transportType.toDisplayString() + ": ";
		} else {
			return "";
		}
	}
}
