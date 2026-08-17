package com.imran.personalcallassistant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 201;
    private static final int REQ_SPEECH = 301;

    private FloatingActionButton fabVoice;
    private TextInputLayout inputLayoutName;
    private TextInputEditText etNameQuery;
    private MaterialButton btnCallAction;
    private MaterialCardView cardResult;
    private TextView tvVoiceStatus;
    private TextView tvMatchStatus;
    private TextView tvMatchDetails;

    private static class ContactInfo {
        String name;
        String number;
        ContactInfo(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lock-screen wake and direct appearance support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }

        setContentView(R.layout.activity_main);

        fabVoice = findViewById(R.id.fabVoice);
        inputLayoutName = findViewById(R.id.inputLayoutName);
        etNameQuery = findViewById(R.id.etNameQuery);
        btnCallAction = findViewById(R.id.btnCallAction);
        cardResult = findViewById(R.id.cardResult);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        tvMatchStatus = findViewById(R.id.tvMatchStatus);
        tvMatchDetails = findViewById(R.id.tvMatchDetails);

        fabVoice.setOnClickListener(v -> checkPermissionsAndPromptVoice());

        btnCallAction.setOnClickListener(v -> {
            String query = etNameQuery.getText() != null ? etNameQuery.getText().toString().trim() : "";
            if (TextUtils.isEmpty(query)) {
                inputLayoutName.setError("Please enter or speak a name");
            } else {
                inputLayoutName.setError(null);
                processCallTarget(query);
            }
        });

        // Request required permissions right away on start
        requestRequiredPermissions();
    }

    private void requestRequiredPermissions() {
        String[] permissions = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO
        };

        List<String> needed = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needed.add(perm);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), REQ_PERMISSIONS);
        }
    }

    private void checkPermissionsAndPromptVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestRequiredPermissions();
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.listening));
        try {
            startActivityForResult(intent, REQ_SPEECH);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0);
                etNameQuery.setText(spokenText);
                cleanAndProcessVoiceCommand(spokenText);
            }
        }
    }

    private void cleanAndProcessVoiceCommand(String spoken) {
        String clean = spoken.toLowerCase(Locale.ROOT)
                .replace("hey google", "")
                .replace("call", "")
                .replace("dial", "")
                .replace("please", "")
                .trim();

        if (TextUtils.isEmpty(clean)) {
            clean = spoken.trim();
        }

        processCallTarget(clean);
    }

    private void processCallTarget(String query) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestRequiredPermissions();
            return;
        }

        ContactInfo bestMatch = findClosestContact(query);

        cardResult.setVisibility(View.VISIBLE);
        if (bestMatch != null) {
            tvMatchStatus.setText("⚡ Calling " + bestMatch.name);
            tvMatchStatus.setTextColor(Color.parseColor("#1B5E20"));
            tvMatchDetails.setText("Matched Name: " + bestMatch.name + "\nNumber: " + bestMatch.number + "\nLine: SIM 1");

            // Execute Immediate SIM 1 Outgoing Call
            makeCallViaSim1(bestMatch.number);
        } else {
            tvMatchStatus.setText("❌ No Contact Found");
            tvMatchStatus.setTextColor(Color.parseColor("#B71C1C"));
            tvMatchDetails.setText("Could not find any contact matching \"" + query + "\"");
        }
    }

    private ContactInfo findClosestContact(String target) {
        String normalizedTarget = target.toLowerCase(Locale.ROOT).trim();
        ContactInfo bestMatch = null;
        int highestScore = -1;

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIdx);
                    String number = cursor.getString(numIdx);

                    if (name != null && number != null) {
                        int score = calculateSimilarity(normalizedTarget, name.toLowerCase(Locale.ROOT));
                        if (score > highestScore) {
                            highestScore = score;
                            bestMatch = new ContactInfo(name, number);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Return match if reasonable similarity exists
        return (highestScore > 30) ? bestMatch : null;
    }

    // Similarity scoring helper
    private int calculateSimilarity(String query, String contactName) {
        if (query.equals(contactName)) return 100;
        if (contactName.contains(query)) return 80;
        if (query.contains(contactName)) return 70;

        String[] qTokens = query.split("\\s+");
        String[] cTokens = contactName.split("\\s+");

        int matchCount = 0;
        for (String q : qTokens) {
            for (String c : cTokens) {
                if (c.startsWith(q) || q.startsWith(c)) {
                    matchCount++;
                    break;
                }
            }
        }

        return matchCount > 0 ? (matchCount * 40) : 0;
    }

    private void makeCallViaSim1(String rawPhoneNumber) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestRequiredPermissions();
            return;
        }

        String cleanedNumber = rawPhoneNumber.replaceAll("[^0-9+*#]", "");
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + Uri.encode(cleanedNumber)));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Bind directly to SIM 1 phone account handle
        try {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<PhoneAccountHandle> handles = telecomManager.getCallCapablePhoneAccounts();
                if (handles != null && !handles.isEmpty()) {
                    // Index 0 represents SIM 1
                    PhoneAccountHandle sim1Handle = handles.get(0);
                    callIntent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, sim1Handle);
                }
            }
        } catch (Exception ignored) {
        }

        // Standard Dual SIM extra keys for SIM 1 (Slot 0)
        callIntent.putExtra("com.android.phone.force.slot", true);
        callIntent.putExtra("Cdma_info_key", 0);
        callIntent.putExtra("simSlot", 0);
        callIntent.putExtra("slot", 0);
        callIntent.putExtra("com.android.phone.extra.slot", 0);
        callIntent.putExtra("phone", 0);

        startActivity(callIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
            }
        }
    }
            }
        
