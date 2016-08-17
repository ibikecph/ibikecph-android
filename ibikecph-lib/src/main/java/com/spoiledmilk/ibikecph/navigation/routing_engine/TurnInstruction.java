// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.navigation.routing_engine;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.read_aloud.NavigationOracle;
import com.spoiledmilk.ibikecph.navigation.read_aloud.Speakable;
import com.spoiledmilk.ibikecph.search.Address;
import com.spoiledmilk.ibikecph.util.Util;

import java.text.DateFormat;

import static com.spoiledmilk.ibikecph.navigation.routing_engine.TransportationType.isPublicTransportation;

public class TurnInstruction implements Speakable {

    protected Type type;
    protected Modifier modifier;
    protected int bearingAfter;

    /**
     * When (if at any time) should this step be performed, if 0 - just as soon as possible
     */
    protected long time;

    protected static DateFormat timeFormat = IBikeApplication.getTimeFormat();

    public TurnInstruction(JsonNode stepNode) {
        JsonNode maneuverNode = stepNode.get("maneuver");
        if (maneuverNode == null || !maneuverNode.isObject()) {
            throw new RuntimeException("Expected a 'maneuver' field");
        }

        JsonNode locationNode = maneuverNode.get("location");
        if (!locationNode.isArray() || locationNode.size() != 2) {
            throw new RuntimeException("Expected an array with 2 items as 'location' field, got " + locationNode);
        }

        double lng = locationNode.get(0).asDouble();
        double lat = locationNode.get(1).asDouble();
        setLocation(Util.locationFromCoordinates(lat, lng));

        type = Type.parseString(maneuverNode.get("type").asText());
        if (maneuverNode.get("modifier") != null) {
            modifier = Modifier.parseString(maneuverNode.get("modifier").asText());
        }

        distance = (float) stepNode.get("distance").asDouble();

        String mode = stepNode.get("mode").asText();
        if (mode.equals("cycling")) {
            transportType = TransportationType.BIKE;
        } else if (mode.equals("pushing bike")) {
            transportType = TransportationType.WALK;
        } else if (mode.equals("ferry")) {
            transportType = TransportationType.F;
        } else if (mode.equals("idling")) {
            // This mode is an extension of the OSRM format to indicate that the user should
            // do nothing while i public transporation.
            // Setting it to null, will let the leg's transporation type overwrite it.
            transportType = null;
        } else {
            throw new RuntimeException("Encountered an unexpected mode: " + mode);
        }

        name = translateStepName(stepNode.get("name").asText());

        if (maneuverNode.get("bearing_after") != null && maneuverNode.get("bearing_after").isNumber()) {
            bearingAfter = maneuverNode.get("bearing_after").asInt();
            directionAbbreviation = generateDirectionAbbreviation(bearingAfter);
        }
    }

    public int getSmallDirectionResourceId() {
        /**
         * Not yet used:
         * R.drawable.location,
         * R.drawable.flag,
         * R.drawable.push_bike
         */
        switch (type) {
            case DEPART:
                return R.drawable.bike;
            case ARRIVE:
                return R.drawable.near_destination;
            case ROUNDABOUT:
            case ROTARY:
                return R.drawable.roundabout;
            default:
                switch (modifier) {
                    case STRAIGHT:
                        return R.drawable.up;
                    case UTURN:
                        return R.drawable.u_turn;
                    case LEFT:
                    case SHARP_LEFT:
                        return R.drawable.left;
                    case SLIGHT_LEFT:
                        return R.drawable.left_ward;
                    case RIGHT:
                    case SHARP_RIGHT:
                        return R.drawable.right;
                    case SLIGHT_RIGHT:
                        return R.drawable.right_ward;
                    default:
                        return 0;
                }
        }
    }

    public int getLargeDirectionResourceId() {
        /**
         * Not yet used:
         * R.drawable.location,
         * R.drawable.flag,
         * R.drawable.push_bike
         */
        switch (type) {
            case DEPART:
                return R.drawable.white_bike;
            case ARRIVE:
                return R.drawable.white_near_destination;
            case ROUNDABOUT:
            case ROTARY:
                return R.drawable.white_roundabout;
            default:
                switch (modifier) {
                    case STRAIGHT:
                        return R.drawable.white_up;
                    case UTURN:
                        return R.drawable.white_u_turn;
                    case LEFT:
                    case SHARP_LEFT:
                        return R.drawable.white_left;
                    case SLIGHT_LEFT:
                        return R.drawable.white_left_ward;
                    case RIGHT:
                    case SHARP_RIGHT:
                        return R.drawable.white_right;
                    case SLIGHT_RIGHT:
                        return R.drawable.white_right_ward;
                    default:
                        return 0;
                }
        }
    }

    @Override
    public String toString() {
        String result = this.getClass().getSimpleName() + "(";
        result += name != null && !name.isEmpty() ? name : "No name";
        result += ", ";
        result += type != null ? type.toString() : "no type";
        result += ", ";
        result += modifier != null ? modifier.toString() : "no modifier";
        result += ")";
        return result;
    }

    @Override
    public String getSpeakableString() {
        String speakableName = NavigationOracle.turnSpeakable(name);
        if(transportType.isPublicTransportation()) {
            speakableName = speakableName.replace("St.", "Station");
            switch (type) {
                case DEPART:
                    return IBikeApplication.getString("depart_with_train")
                            .replace("{{description}}", description == null ? "?" : description)
                            .replace("{{name}}", speakableName);
                case ARRIVE:
                    String arrivalTime = timeFormat.format(time);
                    return IBikeApplication.getString("arrive_with_train")
                            .replace("{{name}}", speakableName)
                            .replace("{{arrivalTime}}", arrivalTime);
                default:
                    return null;
            }
        } else {
            switch (type) {
                case TURN:
                case MERGE:
                case CONTINUE:
                case END_OF_ROAD:
                case FORK:
                case NEW_NAME:
                case NOTIFICATION:
                case ROUNDABOUT_TURN:
                    String languageKey;
                    switch (type) {
                        case END_OF_ROAD:
                        case FORK:
                        case NEW_NAME:
                        case NOTIFICATION:
                        case ROUNDABOUT_TURN:
                            languageKey = "turn";
                            break;
                        default:
                            languageKey = type.toString().toLowerCase();
                    }
                    languageKey += "_" + modifier.name().toLowerCase();
                    return IBikeApplication.getString(languageKey)
                            .replace("{{name}}", speakableName);
                case DEPART:
                    return IBikeApplication.getString("depart")
                            .replace("{{heading}}", modifier == null ? "" : modifier.asHeading())
                            .replace("{{name}}", speakableName);
                case ARRIVE:
                    return IBikeApplication.getString("arrive")
                            .replace("{{name}}", speakableName)
                            .replace("{{side}}", modifier == null ? "" : modifier.asHeading());
                case ROUNDABOUT:
                    return IBikeApplication.getString("roundabout")
                            .replace("%{exit}", "") // TODO: Make sure this value is read from the JSON
                            .replace("{{name}}", speakableName);
                case ROTARY:
                    return IBikeApplication.getString("rotary")
                            .replace("%{exit}", "") // TODO: Make sure this value is read from the JSON
                            .replace("{{name}}", speakableName);
                case ON_RAMP:
                    return IBikeApplication.getString("on_ramp")
                            .replace("{{name}}", speakableName);
                case OFF_RAMP:
                    return IBikeApplication.getString("off_ramp")
                            .replace("{{name}}", speakableName);
                default:
                    return null;
            }
        }
    }

    public Address toAddress() {
        Address result = new Address();
        result.setName(name);
        result.setLocation(Util.locationToLatLng(location));
        return result;
    }

    public String toDisplayString() {
        return getSpeakableString();
    }

    public String name = "";
    /**
     * The distance this turn instruction should be performed.
     * NB: This has changed to reflect the semantics of the OSRMv5
     */
    public float distance = 0;

    public String directionAbbreviation; // N: north, S: south, E: east, W: west, NW: North West, ...

    /**
     * The index into the Leg's list of points.
     * TODO: Consider moving this data to the Leg class, as this class has no reference to the Leg.
     */
    protected int pointsIndex;

    public void setPointsIndex(int pointsIndex) {
        this.pointsIndex = pointsIndex;
    }

    public int getPointsIndex() {
        return this.pointsIndex;
    }

    /**
     * The location in which this instruction should be taken by the user.
     */
    Location location;

    /**
     * This field holds an optional free-text description of the instruction.
     * For public transportation this will be the name or number of the train or bus line.
     */
    protected String description;

    public TransportationType transportType;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getTransitionDistance() {
        if (isPublicTransportation(transportType)) {
            return 100d;
        } else {
            return 20d;
        }
    }

    public Location getLocation() {
        return location;
    }

    public Type getType() {
        return type;
    }

    public static String translateStepName(String name) {
        if (name.matches("\\{.+:.*\\}")) {
            return IBikeApplication.getString(name);
        } else {
            return name;
        }
    }

    public static String generateDirectionAbbreviation(int bearing) {
        if (bearing < 23) {
            return "N";
        } else if (bearing < 23 + 45) {
            return "NE";
        } else if (bearing < 23 + 45 * 2) {
            return "E";
        } else if (bearing < 23 + 45 * 3) {
            return "SE";
        } else if (bearing < 23 + 45 * 4) {
            return "S";
        } else if (bearing < 23 + 45 * 5) {
            return "SW";
        } else if (bearing < 23 + 45 * 6) {
            return "W";
        } else if (bearing < 23 + 45 * 7) {
            return "NW";
        } else {
            return "N";
        }
    }

    public float getDistance() {
        return distance;
    }

    public void setTime(long time) {
        this.time = time;
    }

    /**
     * turn:
     * a basic turn into direction of the modifier
     * <p/>
     * new name:
     * no turn is taken/possible, but the road name changes. The road can take a turn itself,
     * following modifier.
     * <p/>
     * depart:
     * indicates the departure of the leg
     * <p/>
     * arrive:
     * indicates the destination of the leg
     * <p/>
     * merge:
     * merge onto a street (e.g. getting on the highway from a ramp, the modifier specifies the
     * direction of the merge)
     * <p/>
     * on ramp:
     * take a ramp to enter a highway (direction given my modifier)
     * <p/>
     * off ramp:
     * take a ramp to exit a highway (direction given my modifier)
     * <p/>
     * fork:
     * take the left/right side at a fork depending on modifier
     * <p/>
     * end of road:
     * road ends in a T intersection turn in direction of modifier
     * <p/>
     * use lane:
     * going straight on a specific lane
     * <p/>
     * continue:
     * Turn in direction of modifier to stay on the same road
     * <p/>
     * roundabout:
     * traverse roundabout, has additional field exit with NR if the roundabout is left. the
     * modifier specifies the direction of entering the roundabout
     * <p/>
     * rotary:
     * a larger version of a roundabout, can offer rotary_name in addition to the exit parameter.
     * <p/>
     * roundabout turn:
     * Describes a turn at a small roundabout that should be treated as normal turn. The modifier
     * indicates the turn direction. Example instruction: At the roundabout turn left.
     * <p/>
     * notification:
     * not an actual turn but a change in the driving conditions. For example the travel mode. If
     * the road takes a turn itself, the modifier describes the direction
     */
    public enum Type {
        TURN,
        NEW_NAME,
        DEPART,
        ARRIVE,
        MERGE,
        ON_RAMP,
        OFF_RAMP,
        FORK,
        END_OF_ROAD,
        USE_LANE,
        CONTINUE,
        ROUNDABOUT,
        ROTARY,
        ROUNDABOUT_TURN,
        NOTIFICATION;

        public static Type parseString(String type) {
            if (type == null) {
                return null;
            }
            return Type.valueOf(type.toUpperCase().replace(" ", "_"));
        }
    }

    /**
     * uturn:
     * indicates reversal of direction
     * <p/>
     * sharp right:
     * a sharp right turn
     * <p/>
     * right:
     * a normal turn to the right
     * <p/>
     * slight right:
     * a slight turn to the right
     * <p/>
     * straight:
     * no relevant change in direction
     * <p/>
     * slight left:
     * a slight turn to the left
     * <p/>
     * left:
     * a normal turn to the left
     * <p/>
     * sharp left:
     * a sharp turn to the left
     */
    public enum Modifier {
        UTURN,
        SHARP_RIGHT,
        RIGHT,
        SLIGHT_RIGHT,
        STRAIGHT,
        SLIGHT_LEFT,
        LEFT,
        SHARP_LEFT;

        public static Modifier parseString(String modifier) {
            if (modifier == null) {
                return null;
            }
            return Modifier.valueOf(modifier.toUpperCase().replace(" ", "_"));
        }

        public String asHeading() {
            return IBikeApplication.getString("heading_" + name().toLowerCase());
        }
    }
}
