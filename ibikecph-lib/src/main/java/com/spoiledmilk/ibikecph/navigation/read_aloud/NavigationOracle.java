package com.spoiledmilk.ibikecph.navigation.read_aloud;

import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRoute;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMRouteListener;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMTurnInstruction;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;

/**
 * The Navigation oracle speaks
 * Created by kraen on 06-06-16.
 */
public class NavigationOracle implements LocationListener, TextToSpeech.OnInitListener, SMRouteListener {

    /**
     * After this distance in silence, the Oracle will read a message to the user.
     * Currently 1:30 minutes at 20km/t
     */
    protected final static float MAX_SILENCE_DISTANCE = 20000.0f / 60.0f * 1.5f;

    /**
     * The distance to an upcoming instruction when the oracle reads it aloud.
     */
    protected final static float DISTANCE_TO_INSTRUCTION = 75.0f;

    protected TextToSpeech tts;
    protected AudioManager am;

    /**
     * The route that we are reading aloud.
     */
    protected SMRoute route;
    /**
     * The last instruction that the oracle read aloud, as we got closer than
     * DISTANCE_TO_INSTRUCTION
     */
    protected SMTurnInstruction lastCloseInstruction;
    /**
     * The last instruction that the oracle read aloud, as it became the next upcoming instruction.
     */
    protected SMTurnInstruction lastUpcomingInstruction;
    /**
     * The last physical location on which the oracle spoke last.
     */
    protected Location lastSpeakLocation;

    protected boolean enabled = false;

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
        // Request audio focus while speaking
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Request audio focus when speaking
                int requestStatus = am.requestAudioFocus(null,
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                if(requestStatus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    Log.w("NavigationOracle", "Failed to get audio focus");
                }
            }

            @Override
            public void onDone(String utteranceId) {
                am.abandonAudioFocus(null);
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
        // We would use the audio manager to request audio focus when speaking
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.listener = listener;
        this.route = route;
    }

    protected void speak(String text) {
        speak(text, false);
    }

    protected void speak(String text, boolean wait) {
        Log.d("NavigationOracle", "Reading aloud '" + text + "'" + (wait ? " (waiting)" : ""));
        HashMap<String, String> map = new HashMap<>();
        String utteranceId = generateUtteranceId();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        int queueMode = wait ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH;
        // Read it aloud
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, queueMode, null, utteranceId);
        } else {
            tts.speak(text, queueMode, map);
        }
    }

    private SecureRandom random = new SecureRandom();

    protected String generateUtteranceId() {
        return new BigInteger(130, random).toString(32);
    }

    public void setRoute(SMRoute route) {
        if (this.route != null) {
            this.route.removeListener(this);
        }
        this.route = route;
        if(route != null && enabled) {
            routeReady();
        }
    }

    protected void routeReady() {
        // Register the oracle to receive updates on the route.
        route.addListener(this);
        // If an end address is known, let's start by reading that aloud.
        if(route.endAddress != null) {
            String greeting = IBikeApplication.getString("read_aloud_enabled");
            String destination = route.endAddress.getDisplayName();
            greeting = String.format(greeting.replace("%@", "%s"), destination);
            speak(greeting);
        } else {
            Log.w("NavigationOracle", "End address was null when route was ready");
        }
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS) {
            enable();
        } else {
            Log.e("NavigationOracle", "Error setting up the text-to-speech: " + status);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.e("NavigationOracle", "Available languages:");
                Log.e("NavigationOracle", tts.getAvailableLanguages().toArray().toString());
            }
            emitInitError();
            disable();
        }
    }

    public void enable() {
        Log.d("NavigationOracle", "Enabling");
        Locale locale = IBikeApplication.getLocale();
        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
            enabled = true;
            if (route != null) {
                routeReady();
            }
            emitEnabled();
        } else {
            Log.e("NavigationOracle", "Language was not supported");
            emitUnsupportedLanguage();
            disable();
        }
    }

    public void disable() {
        Log.d("NavigationOracle", "Disabling");
        enabled = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        // This removes the route listener
        setRoute(null);
        emitDisabled();
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
        if(!enabled) {
            throw new RuntimeException("Called onLocationChanged but NavigationOracle is disabled");
        }
        Log.d("NavigationOracle", "Got onLocationChanged");
        SMTurnInstruction instruction = getNextInstruction();
        if(route != null && instruction != null) {
            // If we are close enough and the instruction has not been read aloud
            if(location.distanceTo(instruction.getLocation()) < DISTANCE_TO_INSTRUCTION &&
               lastCloseInstruction != instruction) {
                String instructionString = instruction.generateFullDescriptionString();
                speak(instructionString, lastCloseInstruction == null);
                // Make sure we will not be reading this aloud again.
                lastUpcomingInstruction = instruction;
                lastCloseInstruction = instruction;
                // Remember where we were when reading this
                lastSpeakLocation = location;
            } else if (lastUpcomingInstruction != instruction) {
                String upcomingString = IBikeApplication.getString("read_aloud_upcoming_instruction");
                // Let's pick the closest 10 metres
                int metresToInstruction = Math.round(instruction.lengthInMeters / 10.f) * 10;
                upcomingString = String.format(upcomingString.replace("%@", "%d"), metresToInstruction);
                String instructionString = instruction.generateFullDescriptionString();
                // Read it aloud
                speak(upcomingString + " " + instructionString, lastUpcomingInstruction == null);
                // Make sure we will not be reading this aloud again.
                lastUpcomingInstruction = instruction;
                // Remember where we were when reading this
                lastSpeakLocation = location;
            }

            if(lastSpeakLocation != null &&
               location.distanceTo(lastSpeakLocation) > MAX_SILENCE_DISTANCE) {
                int minutesToArrival = Math.round(route.getEstimatedArrivalTime() / 60.0f);
                String encouragement = generateEncouragement(minutesToArrival);
                // If we want to say an encouragement - let's speak
                if(encouragement != null) {
                    speak(encouragement);
                    lastSpeakLocation = location;
                }
            }
        }
    }

    private String generateEncouragement(int minutesToArrival) {
        int hoursToArrival = minutesToArrival / 60;
        minutesToArrival %= 60;

        String minutes = String.valueOf(minutesToArrival);
        String minuteUnit;
        if(minutesToArrival == 1) {
            minuteUnit = IBikeApplication.getString("unit_m_long_singular");
        } else {
            minuteUnit = IBikeApplication.getString("unit_m_long");
        }

        if(hoursToArrival == 0) {
            String encouragement = IBikeApplication.getString("read_aloud_encouragement_time_m");
            return String.format(encouragement.replaceAll("%@", "%s"),
                                 minutes,
                                 minuteUnit);
        } else {
            String hours = String.valueOf(hoursToArrival);
            String hoursUnit;
            if(hoursToArrival == 1) {
                hoursUnit = IBikeApplication.getString("unit_h_long_singular");
            } else {
                hoursUnit = IBikeApplication.getString("unit_h_long");
            }

            String encouragement = IBikeApplication.getString("read_aloud_encouragement_time_h_m");
            return String.format(encouragement.replaceAll("%@", "%s"),
                                 hours,
                                 hoursUnit,
                                 minutes,
                                 minuteUnit);
        }
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
    public void serverError() {

    }
}
