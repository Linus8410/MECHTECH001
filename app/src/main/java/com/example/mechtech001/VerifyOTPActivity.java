package com.example.mechtech001;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerifyOTPActivity extends AppCompatActivity {

    private EditText inputCode1, inputCode2, inputCode3, inputCode4, inputCode5, inputCode6;
    private String verificationId;
    private ProgressBar progressBar;
    private Button buttonVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpactivity);

        TextView textMobile = findViewById(R.id.textmobile);
        textMobile.setText(String.format("+254%s", getIntent().getStringExtra("mobile")));

        inputCode1 = findViewById(R.id.inputcode1);
        inputCode2 = findViewById(R.id.inputcode2);
        inputCode3 = findViewById(R.id.inputcode3);
        inputCode4 = findViewById(R.id.inputcode4);
        inputCode5 = findViewById(R.id.inputcode5);
        inputCode6 = findViewById(R.id.inputcode6);
        setupOTPInputs();

        progressBar = findViewById(R.id.progressBar);
        buttonVerify = findViewById(R.id.buttonverify);

        verificationId = getIntent().getStringExtra("verificationId");

        if (verificationId == null) {
            Toast.makeText(this, "Error: Verification ID is missing!", Toast.LENGTH_LONG).show();
            Log.e("OTP_Debug", "❌ verificationId is NULL!");
            finish();
            return;
        }

        buttonVerify.setOnClickListener(v -> {
            String code = inputCode1.getText().toString().trim()
                    + inputCode2.getText().toString().trim()
                    + inputCode3.getText().toString().trim()
                    + inputCode4.getText().toString().trim()
                    + inputCode5.getText().toString().trim()
                    + inputCode6.getText().toString().trim();

            Log.d("OTP_Debug", "Entered OTP: " + code);

            if (code.length() != 6) {
                Toast.makeText(VerifyOTPActivity.this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            buttonVerify.setVisibility(View.INVISIBLE);

            PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, code);
            FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);

                        if (task.isSuccessful()) {
                            Log.d("OTP_Debug", "OTP Verification Successful!");
                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                Log.d("OTP_Debug", "Firebase User: " + FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
                            }
                            runOnUiThread(() -> {
                                Intent intent = new Intent(getApplicationContext(), SetPassword.class);
                                intent.putExtra("mobile", getIntent().getStringExtra("mobile")); // Ensure phone number is passed
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();

                            });
                        } else {
                            Log.e("OTP_Debug", "OTP Verification Failed: " + task.getException().getMessage());
                            Toast.makeText(VerifyOTPActivity.this, "❌ Invalid OTP. Try Again!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        buttonVerify.setVisibility(View.VISIBLE);
                        Log.e("OTP_Debug", "🔥 Firebase Auth Error: " + e.getMessage());
                        Toast.makeText(VerifyOTPActivity.this, "🔥 Auth Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
        findViewById(R.id.resendOTP).setOnClickListener(v -> resendOTP());
    }

    private void resendOTP() {
        String mobileNumber = getIntent().getStringExtra("mobile");
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Toast.makeText(this, "Error: Mobile number is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+254" + mobileNumber,
                60,
                TimeUnit.SECONDS,
                VerifyOTPActivity.this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        Log.d("OTP_Debug", "✔ Auto Verification Completed!");
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(VerifyOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("OTP_Debug", "❌ Verification Failed: " + e.getMessage());
                    }

                    @Override
                    public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verificationId = newVerificationId;
                        Toast.makeText(VerifyOTPActivity.this, "🔁 OTP Resent", Toast.LENGTH_SHORT).show();
                        Log.d("OTP_Debug", "✔ OTP Code Resent!");
                    }
                }
        );
    }

    private void setupOTPInputs() {
        inputCode1.addTextChangedListener(new OTPTextWatcher(inputCode2));
        inputCode2.addTextChangedListener(new OTPTextWatcher(inputCode3));
        inputCode3.addTextChangedListener(new OTPTextWatcher(inputCode4));
        inputCode4.addTextChangedListener(new OTPTextWatcher(inputCode5));
        inputCode5.addTextChangedListener(new OTPTextWatcher(inputCode6));
    }

    private class OTPTextWatcher implements TextWatcher {
        private final EditText nextField;

        OTPTextWatcher(EditText nextField) {
            this.nextField = nextField;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!s.toString().trim().isEmpty() && nextField != null) {
                nextField.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
