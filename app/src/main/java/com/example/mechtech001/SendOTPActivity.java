// SendOTPActivity.java
package com.example.mechtech001;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SendOTPActivity extends AppCompatActivity {

    private EditText inputMobile;
    private Button buttonGetOTP;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_otpactivity);

        inputMobile = findViewById(R.id.inputmobile);
        buttonGetOTP = findViewById(R.id.getotp);
        TextView txtRegister = findViewById(R.id.existingaccount);
        progressBar = findViewById(R.id.progressBar);

        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(SendOTPActivity.this, MainActivity.class));
            finish();
        });

        buttonGetOTP.setOnClickListener(view -> {
            final String mobileNumber = inputMobile.getText().toString().trim(); // Final variable

            if (mobileNumber.isEmpty()) {
                Toast.makeText(SendOTPActivity.this, "Enter Mobile Number", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            buttonGetOTP.setVisibility(View.INVISIBLE);

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    "+254" + mobileNumber,
                    60,
                    TimeUnit.SECONDS,
                    SendOTPActivity.this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                            progressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            progressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);
                            Toast.makeText(SendOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            progressBar.setVisibility(View.GONE);
                            buttonGetOTP.setVisibility(View.VISIBLE);

                            Intent intent = new Intent(getApplicationContext(), VerifyOTPActivity.class);
                            intent.putExtra("mobile", mobileNumber); // Now works!
                            intent.putExtra("verificationId", verificationId);
                            startActivity(intent);
                        }
                    }
            );
        });

    }
}
