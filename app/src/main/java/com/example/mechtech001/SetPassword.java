package com.example.mechtech001;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SetPassword extends AppCompatActivity {

    private EditText inputPhone, inputPin;
    private Button buttonSetPin;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setpassword);

        inputPhone = findViewById(R.id.inputphone);
        inputPin = findViewById(R.id.inputpin);
        buttonSetPin = findViewById(R.id.setpin);
        progressBar = findViewById(R.id.progressBar);

        // ✅ Get phone number from intent
        phoneNumber = getIntent().getStringExtra("mobile");

        // ✅ Ensure phone number is not null
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Error: Phone number is missing!", Toast.LENGTH_SHORT).show();
            Log.e("SetPassword_Debug", "❌ phoneNumber is NULL or empty!");
            finish();
            return;
        }

        inputPhone.setText(phoneNumber);
        inputPhone.setEnabled(false);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        buttonSetPin.setOnClickListener(view -> setUserPIN());
    }

    private void setUserPIN() {
        String pin = inputPin.getText().toString().trim();

        // ✅ Validate PIN
        if (TextUtils.isEmpty(pin) || pin.length() < 4) {
            Toast.makeText(SetPassword.this, "Enter a valid 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSetPin.setVisibility(View.INVISIBLE);

        // ✅ Verify User Authentication
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getPhoneNumber() == null) {
            Toast.makeText(SetPassword.this, "Authentication error! Try again.", Toast.LENGTH_SHORT).show();
            Log.e("SetPassword_Debug", "❌ User authentication failed!");
            progressBar.setVisibility(View.GONE);
            buttonSetPin.setVisibility(View.VISIBLE);
            return;
        }

        // ✅ Ensure the authenticated user matches the phone number
        if (!user.getPhoneNumber().equals("+254" + phoneNumber)) {
            Toast.makeText(SetPassword.this, "Phone number mismatch!", Toast.LENGTH_SHORT).show();
            Log.e("SetPassword_Debug", "❌ Auth phone: " + user.getPhoneNumber() + ", Entered phone: " + phoneNumber);
            progressBar.setVisibility(View.GONE);
            buttonSetPin.setVisibility(View.VISIBLE);
            return;
        }

        // ✅ Save PIN to Firebase Database
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("phone", phoneNumber);
        userMap.put("pin", pin);

        databaseReference.child(phoneNumber).setValue(userMap).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            buttonSetPin.setVisibility(View.VISIBLE);

            if (task.isSuccessful()) {
                Log.d("SetPassword_Debug", "✅ PIN successfully saved!");
                Toast.makeText(SetPassword.this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                finish();
            } else {
                Log.e("SetPassword_Debug", "❌ Database error: " + task.getException().getMessage());
                Toast.makeText(SetPassword.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


