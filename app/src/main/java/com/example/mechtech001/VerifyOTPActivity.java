package com.example.mechtech001;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerifyOTPActivity extends AppCompatActivity {
    private EditText inputcode1,inputcode2,inputcode3,inputcode4;
    private String verificationId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otpactivity);
        TextView textmobile=findViewById(R.id.textmobile);
        textmobile.setText(String.format(
                "+254%s",getIntent().getStringExtra("mobile")
        ));
        inputcode1=findViewById(R.id.inputcode1);
        inputcode2=findViewById(R.id.inputcode2);
        inputcode3=findViewById(R.id.inputcode3);
        inputcode4=findViewById(R.id.inputcode4);
        setupOTPInputs();
        final ProgressBar progressBar=findViewById(R.id.progressBar);
        final Button buttonverify=findViewById(R.id.buttonverify);
        verificationId=getIntent().getStringExtra("verificationId");
        buttonverify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inputcode1.getText().toString().trim().isEmpty()
                  || inputcode2.getText().toString().trim().isEmpty()
                || inputcode3.getText().toString().trim().isEmpty()
                || inputcode4.getText().toString().trim().isEmpty()) {
                    Toast.makeText(VerifyOTPActivity.this, "Please enter valid code",Toast.LENGTH_SHORT).show();
                    return;
                }
                String code= inputcode1.getText().toString() +
                        inputcode2.getText().toString() +
                        inputcode3.getText().toString() +
                        inputcode4.getText().toString() ;
                if (verificationId != null){
                    progressBar.setVisibility(View.VISIBLE);
                    buttonverify.setVisibility(View.INVISIBLE);
                    PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(
                            verificationId,
                            code
                    );
                    FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    progressBar.setVisibility(View.GONE);
                                    buttonverify.setVisibility(View.VISIBLE);
                                    if(task.isSuccessful()){
                                        Intent intent= new Intent(getApplicationContext(), Clienthome.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK) ;
                            startActivity(intent);
                                    }else {
                Toast.makeText(VerifyOTPActivity.this,"Invalid Code!",Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                }
            }
        });
        findViewById(R.id.resendOTP).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        "+254" + getIntent().getStringExtra("mobile"),
                        60,
                        TimeUnit.SECONDS,
                        VerifyOTPActivity.this,
                        new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {


                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {

                                Toast.makeText(VerifyOTPActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onCodeSent(@NonNull String newVerificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                              verificationId=newVerificationId;
                              Toast.makeText(VerifyOTPActivity.this,"OTP sent",Toast.LENGTH_SHORT).show();
                            }
                        }
                );

            }
        });
    }
    private void  setupOTPInputs(){
        inputcode1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().isEmpty()){
                    inputcode2.requestFocus();

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        inputcode2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().isEmpty()){
                    inputcode3.requestFocus();

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        inputcode3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().isEmpty()){
                    inputcode4.requestFocus();

                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



    }


}