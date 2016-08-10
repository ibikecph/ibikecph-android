package com.spoiledmilk.ibikecph.navigation.routing_engine.v5;

import com.fasterxml.jackson.databind.JsonNode;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.navigation.read_aloud.Speakable;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A OSRM v5 reimplementation of the turn instruction
 * TODO: Move functionality from the SMTurnInstruction into this class as OSRMv4 is phased out.
 * Created by kraen on 12-07-16.
 */
public class TurnInstruction extends SMTurnInstruction implements Speakable {

    /**
     * turn:
     * a basic turn into direction of the modifier
     *
     * new name:
     * no turn is taken/possible, but the road name changes. The road can take a turn itself,
     * following modifier.
     *
     * depart:
     * indicates the departure of the leg
     *
     * arrive:
     * indicates the destination of the leg
     *
     * merge:
     * merge onto a street (e.g. getting on the highway from a ramp, the modifier specifies the
     * direction of the merge)
     *
     * on ramp:
     * take a ramp to enter a highway (direction given my modifier)
     *
     * off ramp:
     * take a ramp to exit a highway (direction given my modifier)
     *
     * fork:
     * take the left/right side at a fork depending on modifier
     *
     * end of road:
     * road ends in a T intersection turn in direction of modifier
     *
     * use lane:
     * going straight on a specific lane
     *
     * continue:
     * Turn in direction of modifier to stay on the same road
     *
     * roundabout:
     * traverse roundabout, has additional field exit with NR if the roundabout is left. the
     * modifier specifies the direction of entering the roundabout
     *
     * rotary:
     * a larger version of a roundabout, can offer rotary_name in addition to the exit parameter.
     *
     * roundabout turn:
     * Describes a turn at a small roundabout that should be treated as normal turn. The modifier
     * indicates the turn direction. Example instruction: At the roundabout turn left.
     *
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
            if(type == null) {
                return null;
            }
            return Type.valueOf(type.toUpperCase().replace(" ", "_"));
        }
    }

    /**
     * uturn:
     * indicates reversal of direction
     *
     * sharp right:
     * a sharp right turn
     *
     * right:
     * a normal turn to the right
     *
     * slight right:
     * a slight turn to the right
     *
     * straight:
     * no relevant change in direction
     *
     * slight left:
     * a slight turn to the left
     *
     * left:
     * a normal turn to the left
     *
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
            if(modifier == null) {
                return null;
            }
            return Modifier.valueOf(modifier.toUpperCase().replace(" ", "_"));
        }

        public String asHeading() {
            return IBikeApplication.getString("heading_" + name().toLowerCase());
        }
    }

    protected Type type;
    protected Modifier modifier;
    protected int bearingAfter;

    public Type getType() {
        return type;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public TurnInstruction(JsonNode stepNode) {
        super();
        JsonNode maneuverNode = stepNode.get("maneuver");
        if(maneuverNode == null || !maneuverNode.isObject()) {
            throw new RuntimeException("Expected a 'maneuver' field");
        }

        JsonNode locationNode = maneuverNode.get("location");
        if(locationNode == null || !locationNode.isArray() || locationNode.size() != 2) {
            throw new RuntimeException("Expected an array with 2 items as 'location' field");
        }

        double lng = locationNode.get(0).asDouble();
        double lat = locationNode.get(1).asDouble();
        setLocation(Util.locationFromCoordinates(lat, lng));

        type = Type.parseString(maneuverNode.get("type").asText());
        if(maneuverNode.get("modifier") != null) {
            modifier = Modifier.parseString(maneuverNode.get("modifier").asText());
        }

        String mode = stepNode.get("mode").asText();
        if(mode.equals("cycling")) {
            transportType = SMRoute.TransportationType.BIKE;
        } else if(mode.equals("pushing bike")) {
            transportType = SMRoute.TransportationType.WALK;
        }

        name = translateStepName(stepNode.get("name").asText());

        double duration = stepNode.get("duration").asDouble();
        timeInSeconds = (int) Math.round(duration);

        if(maneuverNode.get("bearing_after") != null && maneuverNode.get("bearing_after").isNumber()) {
            bearingAfter = maneuverNode.get("bearing_after").asInt();
            directionAbbreviation = generateDirectionAbbreviation(bearingAfter);
        }
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
                        .replace("{{name}}", name);
            case DEPART:
                return IBikeApplication.getString("depart")
                        .replace("{{heading}}", modifier == null ? "" : modifier.asHeading())
                        .replace("{{name}}", name);
            case ARRIVE:
                return IBikeApplication.getString("arrive")
                        .replace("{{name}}", name)
                        .replace("{{side}}", modifier == null ? "" : modifier.asHeading());
            case ROUNDABOUT:
                return IBikeApplication.getString("roundabout")
                        .replace("%{exit}", "") // TODO: Make sure this value is read from the JSON
                        .replace("{{name}}", name);
            case ROTARY:
                return IBikeApplication.getString("rotary")
                        .replace("%{exit}", "") // TODO: Make sure this value is read from the JSON
                        .replace("{{name}}", name);
            case ON_RAMP:
                return IBikeApplication.getString("on_ramp")
                        .replace("{{name}}", name);
            case OFF_RAMP:
                return IBikeApplication.getString("off_ramp")
                        .replace("{{name}}", name);
            default:
                return null;
        }
    }

}
