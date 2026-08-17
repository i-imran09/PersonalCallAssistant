package com.imran.personalcallassistant;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

import java.util.Locale;

public class AssistantCallService extends InCallService {

    private TextToSpeech tts;
    private AudioManager audioManager;
    private Call currentCall;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String incomingNumber = "Unknown";
    private String detectedName = "Unknown Caller";
    private String detectedReason = "General Inquiry";
    private String selectedLanguage = "Tamil";

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                tts.setAudioAttributes(attributes);

                int result = tts.setLanguage(new Locale("ta", "IN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        currentCall = call;

        if (call.getState() == Call.STATE_RINGING) {
            Uri handle = call.getDetails().getHandle();
            if (handle != null) {
                incomingNumber = handle.getSchemeSpecificPart();
            }

            if (isKnownContact(incomingNumber)) {
                return;
            }

            // Auto answer unknown incoming SIM 1 call after 2 seconds
            mainHandler.postDelayed(() -> {
                if (currentCall != null && currentCall.getState() == Call.STATE_RINGING) {
                    currentCall.answer(0);
                    configureAudioAndRunBot();
                }
            }, 2000);
        }
    }

    private boolean isKnownContact(String number) {
        Cursor cursor = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return false;
    }

    private void configureAudioAndRunBot() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setMicrophoneMute(false);
            // Low/medium speaker routing so audio transmits clearly through mic uplink
            audioManager.setSpeakerphoneOn(true);
            setAudioRoute(CallAudioState.ROUTE_SPEAKER);
        }

        // 1. Greet and ask for language
        speak("வணக்கம், நான் இம்ரானின் வாய்ஸ் அசிஸ்டண்ட். Hi, I am Imran's personal voice assistant. Please choose Tamil or English.");

        // 2. Ask for Name
        mainHandler.postDelayed(() -> {
            speak("தயவுசெய்து உங்கள் பெயரைச் சொல்லுங்கள். Please say your name.");
            detectedName = "Caller (" + incomingNumber + ")";
        }, 7000);

        // 3. Ask for Reason
        mainHandler.postDelayed(() -> {
            speak("இம்ரானை எதற்காக அழைக்கிறீர்கள்? What is the reason for calling Imran?");
            detectedReason = "Wants to speak with Imran urgently";
        }, 13000);

        // 4. Thank you message
        mainHandler.postDelayed(() -> {
            speak("இம்ரானை அழைத்ததற்கு நன்றி. உங்கள் தகவலைப் பெற்றுக்கொண்டோம், அவர் விரைவில் பதிலளிப்பார். Thank you, Imran will call you back shortly.");
        }, 19000);

        // Disconnect and log to Dashboard
        mainHandler.postDelayed(() -> {
            CallStorageHelper.saveRecord(getApplicationContext(), new CallRecord(
                    incomingNumber,
                    detectedName,
                    detectedReason,
                    selectedLanguage,
                    System.currentTimeMillis()
            ));

            if (currentCall != null) {
                currentCall.disconnect();
            }
            resetAudio();
        }, 25000);
    }

    private void speak(String text) {
        if (tts != null) {
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
            tts.speak(text, TextToSpeech.QUEUE_ADD, params, "ASSISTANT_VOICE_" + System.currentTimeMillis());
        }
    }

    private void resetAudio() {
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (currentCall == call) {
            currentCall = null;
            resetAudio();
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        resetAudio();
        super.onDestroy();
    }
}
