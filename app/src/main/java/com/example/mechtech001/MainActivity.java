package com.example.mechtech001;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    EditText phoneNumberEditText, pinEditText;
    TextView txtregister;
    Button btnlogin, btnlogin1;
    FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = findViewById(R.id.phonenNumber);
        pinEditText = findViewById(R.id.pin);
        txtregister = findViewById(R.id.registernew);
        btnlogin = findViewById(R.id.loginclient);
        btnlogin1 = findViewById(R.id.loginmech);
        mAuth = FirebaseAuth.getInstance();

        txtregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SendOTPActivity.class));
                finish();
            }
        });

        // Logic for btnlogin
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin("1234", SendOTPActivity.class);
            }
        });

        // Logic for btnlogin1
        btnlogin1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin("5678", Mechanic.class);
            }
        });
    }

    private void handleLogin(String expectedPin, Class<?> targetActivity) {
        String phoneNumber = phoneNumberEditText.getText().toString(); // Get phone number
        String pin = pinEditText.getText().toString();                // Get PIN

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(pin)) {
            Toast.makeText(MainActivity.this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initiate phone number authentication
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(MainActivity.this)    // Activity (for callback binding)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-retrieval or instant verification has completed, sign in the user
                        verifyPinAndSignIn(credential, pin, expectedPin, targetActivity);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(getApplicationContext(), "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPinAndSignIn(PhoneAuthCredential credential, String enteredPin, String expectedPin, Class<?> targetActivity) {
        if (!enteredPin.equals(expectedPin)) {
            Toast.makeText(getApplicationContext(), "Invalid PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sign in with credential
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(getApplicationContext(), targetActivity);
                            startActivity(intent);
                            finish();
                            Toast.makeText(getApplicationContext(), "Login Success", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
