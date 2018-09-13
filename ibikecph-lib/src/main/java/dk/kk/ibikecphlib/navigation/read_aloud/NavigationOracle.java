package dk.kk.ibikecphlib.navigation.read_aloud;

import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.google.android.gms.location.LocationListener;
import dk.kk.ibikecphlib.IBikeApplication;
import dk.kk.ibikecphlib.R;
import dk.kk.ibikecphlib.map.states.NavigatingState;
import dk.kk.ibikecphlib.navigation.NavigationState;
import dk.kk.ibikecphlib.navigation.NavigationStateListener;
import dk.kk.ibikecphlib.navigation.routing_engine.TurnInstruction;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;

import dk.kk.ibikecphlib.navigation.NavigationStateListener;

import static dk.kk.ibikecphlib.navigation.routing_engine.TransportationType.isPublicTransportation;

/**
 * The Navigation oracle speaks
 * Created by kraen on 06-06-16.
 */
public class NavigationOracle implements LocationListener, TextToSpeech.OnInitListener, NavigationStateListener {

    /**
     * After this distance in silence, the Oracle will read a message to the user.
     * Currently 1:30 minutes at 20km/t
     */
    protected final static float MAX_SILENCE_DISTANCE = 20000.0f / 60.0f * 1.5f;

    /**
     * The distance to an upcoming instruction when the oracle reads it aloud.
     */
    protected final static float DISTANCE_TO_INSTRUCTION_BIKING = 75.0f;
    protected final static float DISTANCE_TO_INSTRUCTION_DRIVING = 150.0f;

    protected TextToSpeech tts;
    protected AudioManager am;

    /**
     * The route that we are reading aloud.
     */
    protected NavigationState state;
    /**
     * The last instruction that the oracle read aloud, as we got closer than
     * DISTANCE_TO_INSTRUCTION
     */
    protected TurnInstruction lastCloseInstruction;
    /**
     * The last instruction that the oracle read aloud, as it became the next upcoming instruction.
     */
    protected TurnInstruction lastUpcomingInstruction;
    /**
     * The last physical location on which the oracle spoke last.
     */
    protected Location lastSpeakLocation;

    protected boolean enabled = false;
    protected boolean readAloudVehicleChange;

    public boolean isEnabled() {
        return enabled;
    }

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

    public NavigationOracle(Context context, NavigatingState state, NavigationOracleListener listener) {
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
        this.state = state.getNavigationState();
        readAloudVehicleChange = context.getResources().getBoolean(R.bool.readAloudVehicleChange);
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

    protected static SecureRandom random = new SecureRandom();

    /**
     * Generates a random string that can be used to send to the TTS engine.
     * @return
     */
    protected String generateUtteranceId() {
        return new BigInteger(130, random).toString(32);
    }

    /**
     * The journey is ready - this speaks a greeting
     */
    protected void routeReady() {
        // If an end address is known, let's start by reading that aloud.
        if(state.getRoute().getEndAddress() != null) {
            String greeting = IBikeApplication.getString("read_aloud_enabled");
            String destination = state.getRoute().getEndAddress().getDisplayName();
            destination = turnSpeakable(destination);
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

    /**
     * Enable the navigation oracle
     */
    public void enable() {
        Log.d("NavigationOracle", "Enabling");
        Locale locale = IBikeApplication.getLocale();
        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
            enabled = true;
            if (state.getRoute() != null) {
                // TODO: Consider adding a listener on the state, to know if the journey changes.
                routeReady();
            }
            emitEnabled();
        } else {
            Log.e("NavigationOracle", "Language was not supported");
            emitUnsupportedLanguage();
            disable();
        }
    }

    /**
     * Disable the navigation oracle
     */
    public void disable() {
        Log.d("NavigationOracle", "Disabling");
        enabled = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        emitDisabled();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(!enabled) {
            throw new RuntimeException("Called onLocationChanged but NavigationOracle is disabled");
        }
        // Log.d("NavigationOracle", "Got onLocationChanged");
        TurnInstruction instruction = state.getNextStep();
        if(state.getRoute() != null && instruction != null) {
            // If the next instruction arrival but we are not on the last leg
            // This prevents the oracle from stating that we've arrived before we actually have
            String instructionSentence = generateInstructionSentence(instruction);
            // If we are close enough and the instruction has not been read aloud
            if (lastCloseInstruction != instruction &&
               location.distanceTo(instruction.getLocation()) < getDistanceWhenReading(instruction)) {
                speak(instructionSentence, lastCloseInstruction == null);
                // Make sure we will not be reading this aloud again.
                lastUpcomingInstruction = instruction;
                lastCloseInstruction = instruction;
                // Remember where we were when reading this
                lastSpeakLocation = location;
            } else if (lastUpcomingInstruction != instruction) {
                float distance = state.getDistanceToStep(instruction);
                String upcomingSentence = generateUpcomingSentence(Math.round(distance));
                speak(upcomingSentence + " " + instructionSentence, lastUpcomingInstruction == null);
                // Make sure we will not be reading this aloud again.
                lastUpcomingInstruction = instruction;
                // Remember where we were when reading this
                lastSpeakLocation = location;
            }

            if(lastSpeakLocation != null &&
               location.distanceTo(lastSpeakLocation) > MAX_SILENCE_DISTANCE &&
               !instruction.getTransportType().isPublicTransportation()) {
                int minutesToArrival = (int) Math.round(state.getBikingDuration() / 60.0f);
                String encouragement = generateEncouragementSentence(minutesToArrival);
                // If we want to say an encouragement - let's speak
                if(encouragement != null) {
                    speak(encouragement);
                    lastSpeakLocation = location;
                }
            }
        }
    }

    /**
     * Reads aloud ex "In {metresToInstruction} metres {instructionString}"
     * or ex "In {metresToInstruction / 1000} kilometres {instructionString}" if metresToInstruction
     * is greater than 1000 metres.
     * @param metresToInstruction
     */
    protected String generateUpcomingSentence(int metresToInstruction) {
        // Let's pick the closest 10 metres
        metresToInstruction = Math.round(metresToInstruction / 10.f) * 10;
        String upcomingString = IBikeApplication.getString("read_aloud_upcoming_instruction");
        upcomingString = upcomingString.replace("%@", "%s");
        if(metresToInstruction < 1000) {
            String unit = IBikeApplication.getString("unit_metre");
            return String.format(upcomingString, String.valueOf(metresToInstruction), unit);
        } else {
            String unit;
            if(metresToInstruction == 1000) {
                unit = IBikeApplication.getString("unit_kilometre");
            } else {
                unit = IBikeApplication.getString("unit_kilometres");
            }
            String kmToInstruction = String.format(Locale.getDefault(), "%.1f", metresToInstruction / 1000f);
            return String.format(upcomingString, kmToInstruction, unit);
        }
    }

    /**
     * Generates the text to speak aloud, that describes an instruction that the user could follow.
     * @param instruction
     * @return
     */
    private String generateInstructionSentence(TurnInstruction instruction) {
        String result = "";
        if(instruction != null) {
            if (readAloudVehicleChange &&
                lastCloseInstruction != null &&
                lastCloseInstruction.transportType != instruction.transportType) {
                // The user should change vehicle
                int vehicleId = instruction.transportType.getVehicleId();
                if (vehicleId > 0) {
                    result += IBikeApplication.getString("vehicle_changed_instruction_" + vehicleId);
                }
            }
            result += instruction.getSpeakableString();
        }
        return result;
    }

    private String generateEncouragementSentence(int minutesToArrival) {
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

    private float getDistanceWhenReading(TurnInstruction instruction) {
        if(isPublicTransportation(instruction.transportType)) {
            return DISTANCE_TO_INSTRUCTION_DRIVING;
        } else {
            return DISTANCE_TO_INSTRUCTION_BIKING;
        }
    }

    public static String turnSpeakable(String string) {
        // "F" replaced with something that is not read "Farad"
        string = string.replaceAll("(?<=[0-9])F", " æf");
        // "C" replaced with something that is not read "Coulomb"
        string = string.replaceAll("(?<=[0-9])C", " se");
        return string;
    }

    @Override
    public void destinationReached() {
        speak(IBikeApplication.getString("you_have_reached_your_destination"));
    }

    @Override
    public void navigationStarted() {
        Log.d("NavigationOracle", "Route has started!");
    }

    @Override
    public void routeNotFound() {
        speak(IBikeApplication.getString("error_route_not_found"));
    }

    @Override
    public void routeRecalculationStarted() {
        speak(IBikeApplication.getString("read_aloud_recalculating_route"));
    }

    @Override
    public void routeRecalculationCompleted() {

    }

    @Override
    public void serverError() {

    }
}
