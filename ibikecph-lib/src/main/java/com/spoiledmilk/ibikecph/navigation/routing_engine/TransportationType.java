package com.spoiledmilk.ibikecph.navigation.routing_engine;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;

/**
 * Created by kraen on 11-08-16.
 */
public enum TransportationType {

    BIKE, M, S, WALK, TOG, BUS, IC, LYN, REG, EXB, NB, TB, F;

    /**
     * Translates a transportation type into displayable string.
     * TODO: Implement proper translations of all types available
     *
     * @return
     */
    public String toDisplayString() {
        int vehicleId = getVehicleId();
        if (vehicleId > 0) {
            String languageKey = "vehicle_" + vehicleId;
            return IBikeApplication.getString(languageKey);
        } else {
            return null;
        }
    }

    public int getVehicleId() {
        switch (this) {
            case BIKE:
                return 1;
            case WALK:
                return 2;
            case F:
                return 3;
            case IC:
            case LYN:
            case M:
            case REG:
            case S:
            case TOG:
                return 4;
        }
        return 0;
    }

    public enum DrawableSize {
        SMALL,
        LARGE
    }

    public int getDrawableId() {
        return getDrawableId(DrawableSize.LARGE);
    }

    /**
     * Returns a drawable representing the particular type of transportation
     * TODO: Consider implementing the two sizes available as drawables
     *
     * @return
     */
    public int getDrawableId(DrawableSize size) {
        if (this == TransportationType.BIKE) {
            return R.drawable.route_bike; // TODO: Add a large version
        } else if (this == TransportationType.M) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_metro_direction :
                    R.drawable.route_metro;
        } else if (this == TransportationType.S) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_s_direction :
                    R.drawable.route_s;
        } else if (this == TransportationType.TOG) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_train_direction :
                    R.drawable.route_train;
        } else if (this == TransportationType.WALK) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_walking_direction :
                    R.drawable.route_walk;
        } else if (this == TransportationType.IC) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_train_direction :
                    R.drawable.route_train;
        } else if (this == TransportationType.LYN) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_train_direction :
                    R.drawable.route_train;
        } else if (this == TransportationType.REG) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_train_direction :
                    R.drawable.route_train;
        } else if (this == TransportationType.BUS) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_bus_direction :
                    R.drawable.route_bus;
        } else if (this == TransportationType.EXB) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_bus_direction :
                    R.drawable.route_bus;
        } else if (this == TransportationType.NB) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_bus_direction :
                    R.drawable.route_bus;
        } else if (this == TransportationType.TB) {
            return size == DrawableSize.LARGE ?
                    R.drawable.route_bus_direction :
                    R.drawable.route_bus;
        } else if (this == TransportationType.F) {
            return R.drawable.route_ship_direction; // TODO: Add a large version
        } else {
            return 0;
        }
    }

    public static boolean isPublicTransportation(TransportationType type) {
        return (type != null &&
                type != TransportationType.BIKE &&
                type != TransportationType.WALK);
    }

    public boolean isPublicTransportation() {
        return isPublicTransportation(this);
    }
}
