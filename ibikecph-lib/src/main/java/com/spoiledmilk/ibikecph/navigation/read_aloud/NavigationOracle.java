package com.spoiledmilk.ibikecph.navigation.read_aloud;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

import java.util.Locale;

/**
 * The Navigation oracle speaks
 * Created by kraen on 06-06-16.
 */
public class NavigationOracle implements LocationListener, TextToSpeech.OnInitListener, SMRouteListener {

    protected TextToSpeech tts;
    protected SMRoute route;
    protected SMTurnInstruction lastReadInstruction;

    public interface NavigationOracleListener {
        void enabled();
        void disabled();
        void initError();
        void unsupportedLanguage();
    }

    protected NavigationOracleListener listener;

    protected void emitEnabled() {
        if(listener != null) {
            listener.enabled();
        }
    }

    private void emitInitError() {
        if(listener != null) {
            listener.initError();
        }
    }

    private void emitUnsupportedLanguage() {
        if(listener != null) {
            listener.unsupportedLanguage();
        }
    }

    protected void emitDisabled() {
        if(listener != null) {
            listener.disabled();
        }
    }

    public NavigationOracle(Context context, SMRoute route) {
        this(context, route, null);
    }

    public NavigationOracle(Context context, SMRoute route, NavigationOracleListener listener) {
        tts = new TextToSpeech(context, this);
        this.listener = listener;
        this.route = route;
    }

    protected void speak(String text) {
        speak(text, false);
    }

    protected void speak(String text, boolean wait) {
        Log.d("NavigationOracle", "Reading aloud '" + text + "'" + (wait ? " (waiting)" : ""));
        int queueMode = wait ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH;
        // Read it aloud
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, queueMode, null, null);
        } else {
            tts.speak(text, queueMode, null);
        }
    }

    public void setRoute(SMRoute route) {
        this.route = route;
    }

    @Override
    public void onInit(int status) {
        Log.d("NavigationOracle", "onInit called with status " + status);
        if(status == TextToSpeech.ERROR) {
            // TODO: Report this back to the initiator of the object
            Log.e("NavigationOracle", "Error setting up the text-to-speech");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.e("NavigationOracle", "Available languages:");
                Log.e("NavigationOracle", tts.getAvailableLanguages().toArray().toString());
            }
            emitInitError();
            disable();
        } else {
            enable();
        }
    }

    String[] greetings = {
            "Højtlæsning aktiveret",
            "Nu læser jeg ruten op",
            "Jeg læser ruten op",
            "Hold fast i styret, træd i pedalerne og lyt til mine instrukser",
            "Er du klar?",
            "Vi er på vej til Nørrebrogade",
            "Vi er fremme om 10 minutter"
    };
    static int currentGreeting = 0;

    public void enable() {
        Locale locale = IBikeApplication.getLocale();
        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
            String greeting;

            // TODO: Remove when we're done debugging
            if(locale.getLanguage().equals("da")) {
                if(currentGreeting >= greetings.length) {
                    currentGreeting = 0;
                }
                greeting = greetings[currentGreeting];
                currentGreeting++;
            } else {
                greeting = IBikeApplication.getString("read_aloud_enabled");
            }

            speak(greeting);
            if (route != null) {
                route.addListener(this);
            }
            emitEnabled();
        } else {
            Log.e("NavigationOracle", "Language was not supported");
            emitUnsupportedLanguage();
            disable();
        }
    }

    public void disable() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        emitDisabled();
        if (route != null) {
            route.removeListener(this);
        }
    }

    public SMTurnInstruction getNextInstruction() {
        if (route != null && route.getTurnInstructions().size() > 0) {
            return route.getTurnInstructions().get(0);
        } else {
            return null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        SMTurnInstruction instruction = getNextInstruction();
        // If we are close enough and the instruction has not been read aloud
        if(location.distanceTo(instruction.getLocation()) < 50 && lastReadInstruction != instruction) {
            String instructionString = instruction.generateFullDescriptionString();
            speak(instructionString, lastReadInstruction == null);
            lastReadInstruction = instruction;
        }
    }

    @Override
    public void updateTurn(boolean firstElementRemoved) {

    }

    @Override
    public void reachedDestination() {
        speak(IBikeApplication.getString("you_have_reached_your_destination"));
    }

    @Override
    public void updateRoute() {

    }

    @Override
    public void startRoute() {
        Log.d("NavigationOracle", "Route has started!");
    }

    @Override
    public void routeNotFound() {
        speak(IBikeApplication.getString("error_route_not_found"));
    }

    @Override
    public void routeRecalculationStarted() {
        speak(IBikeApplication.getString("calculating_new_route"));
    }

    @Override
    public void routeRecalculationDone() {

    }

    @Override
    public void routeRecalculationDone(String type) {

    }

    @Override
    public void serverError() {

    }
}
