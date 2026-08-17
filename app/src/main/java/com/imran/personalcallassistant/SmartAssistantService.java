package com.imran.personalcallassistant;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import java.util.List;
import java.util.Locale;

public class SmartAssistantService extends InCallService {

    private TextToSpeech tts;
    private AudioManager audioManager;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBotRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ta", "IN"));
            }
        });
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        String number = call.getDetails().getHandle().getSchemeSpecificPart();

        // 1. If it's outgoing call (that WE initiated), start Bot
        if (call.getState() == Call.STATE_DIALING || call.getState() == Call.STATE_ACTIVE) {
            if (isBotRunning) {
                playAIScript(call);
            }
            return;
        }

        // 2. If it's incoming & unknown, Reject and Callback
        if (call.getState() == Call.STATE_RINGING && !isKnownContact(number)) {
            call.reject(false, null); // Reject
            isBotRunning = true;
            mainHandler.postDelayed(() -> makeCallback(number), 2000);
        }
    }

    private void makeCallback(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Force SIM 1
        TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        try {
            List<PhoneAccountHandle> handles = tm.getCallCapablePhoneAccounts();
            if (handles != null && !handles.isEmpty()) {
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handles.get(0));
            }
        } catch (SecurityException ignored) {}
        startActivity(intent);
    }

    private void playAIScript(Call call) {
        audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setSpeakerphoneOn(true);
        
        speak("வணக்கம், நான் இம்ரானின் வாய்ஸ் அசிஸ்டண்ட். நீங்கள் யாரைப் பார்க்கிறீர்கள்?");
        mainHandler.postDelayed(() -> {
            speak("இம்ரான் தற்சமயம் பிஸியாக உள்ளார். உங்கள் பெயரைப் பதிவு செய்யுங்கள்.");
        }, 5000);
        mainHandler.postDelayed(() -> {
            isBotRunning = false;
            call.disconnect();
        }, 10000);
    }

    private void speak(String text) {
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ID");
    }

    private boolean isKnownContact(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return exists;
    }
}
