package dk.kk.ibikecphlib.navigation;

import dk.kk.ibikecphlib.navigation.routing_engine.TurnInstruction;

/**
 * A listener you can register on the NavigationState, to receive updates on the users navigation
 * alongside the route.
 * Created by kraen on 16-08-16.
 */
public interface NavigationStateListener {
    void navigationStarted();
    void destinationReached();
    void routeRecalculationStarted();
    void routeRecalculationCompleted();
    void serverError();
    void routeNotFound();
}
