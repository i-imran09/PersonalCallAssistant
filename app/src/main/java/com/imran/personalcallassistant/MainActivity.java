package com.imran.personalcallassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private TextInputLayout inputLayoutPhone;
    private TextInputEditText etPhoneNumber;
    private MaterialButton btnCheckContact;
    private MaterialCardView cardResult;
    private TextView tvResultStatus;
    private TextView tvContactDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputLayoutPhone = findViewById(R.id.inputLayoutPhone);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnCheckContact = findViewById(R.id.btnCheckContact);
        cardResult = findViewById(R.id.cardResult);
        tvResultStatus = findViewById(R.id.tvResultStatus);
        tvContactDetails = findViewById(R.id.tvContactDetails);

        btnCheckContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndVerifyContact();
            }
        });
    }

    private void checkPermissionAndVerifyContact() {
        String phoneInput = etPhoneNumber.getText() != null ? etPhoneNumber.getText().toString().trim() : "";

        String digitsOnly = phoneInput.replaceAll("[^0-9+]", "");
        if (TextUtils.isEmpty(digitsOnly) || digitsOnly.length() < 6) {
            inputLayoutPhone.setError(getString(R.string.invalid_number));
            cardResult.setVisibility(View.GONE);
            return;
        } else {
            inputLayoutPhone.setError(null);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            searchContactInPhone(phoneInput);
        }
    }

    private void searchContactInPhone(String phoneNumber) {
        String contactName = null;
        Cursor cursor = null;

        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
                    Uri.encode(phoneNumber)
            );

            String[] projection = new String[]{
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER
            };

            cursor = getContentResolver().query(uri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                if (nameIndex != -1) {
                    contactName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        cardResult.setVisibility(View.VISIBLE);
        if (contactName != null) {
            tvResultStatus.setText("✅ Contact Found");
            tvResultStatus.setTextColor(Color.parseColor("#2E7D32"));
            tvContactDetails.setText("Name: " + contactName + "\nSearched: " + phoneNumber);
        } else {
            tvResultStatus.setText("❌ Contact Not Found");
            tvResultStatus.setTextColor(Color.parseColor("#C62828"));
            tvContactDetails.setText("This phone number is not saved in your contacts list.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String phoneInput = etPhoneNumber.getText() != null ? etPhoneNumber.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(phoneInput)) {
                    searchContactInPhone(phoneInput);
                }
            } else {
                cardResult.setVisibility(View.VISIBLE);
                tvResultStatus.setText("⚠️ Permission Required");
                tvResultStatus.setTextColor(Color.parseColor("#E65100"));
                tvContactDetails.setText(getString(R.string.permission_denied));
                Toast.makeText(this, R.string.permission_rationale, Toast.LENGTH_LONG).show();
            }
        }
    }
}
