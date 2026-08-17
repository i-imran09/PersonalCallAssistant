package com.imran.personalcallassistant;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telecom.Call;
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
                tts.setLanguage(new Locale("ta", "IN"));
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

            // Check if contact already exists in phonebook
            if (isKnownContact(incomingNumber)) {
                return; // Let normal ring happen for known contacts
            }

            // Auto answer unknown incoming SIM 1 call after 2 seconds
            mainHandler.postDelayed(() -> {
                if (currentCall != null && currentCall.getState() == Call.STATE_RINGING) {
                    currentCall.answer(0);
                    startAssistantConversation();
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

    private void startAssistantConversation() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(true);
        }

        // 1. Greet and ask for language
        speak("வணக்கம், நான் இம்ரானின் வாய்ஸ் அசிஸ்டண்ட். Hi, I am Imran's personal voice assistant. Please choose Tamil or English.");

        // 2. Ask for Name
        mainHandler.postDelayed(() -> {
            speak("தயவுசெய்து உங்கள் பெயரைச் சொல்லுங்கள். Please say your name.");
            detectedName = "Caller (" + incomingNumber + ")";
        }, 6000);

        // 3. Ask for Reason
        mainHandler.postDelayed(() -> {
            speak("இம்ரானை எதற்காக அழைக்கிறீர்கள்? What is the reason for calling Imran?");
            detectedReason = "Needs to speak with Imran urgently";
        }, 11000);

        // 4. Thank you and Auto Hangup
        mainHandler.postDelayed(() -> {
            speak("இம்ரானை அழைத்ததற்கு நன்றி. உங்கள் தகவலைப் பெற்றுக்கொண்டோம், அவர் விரைவில் பதிலளிப்பார். Thank you, Imran will call you back shortly.");
        }, 16000);

        // Disconnect Call & Save to Dashboard
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
        }, 22000);
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "UTTERANCE_ID");
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (currentCall == call) {
            currentCall = null;
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

