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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    EditText phoneNumberEditText, pinEditText;
    Button btnLoginClient, btnLoginMech;
    TextView txtRegister;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = findViewById(R.id.phoneNumber);
        pinEditText = findViewById(R.id.pin);
        btnLoginClient = findViewById(R.id.loginclient);
        btnLoginMech = findViewById(R.id.loginmech);
        txtRegister = findViewById(R.id.registernew);

        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SendOTPActivity.class));
            finish();
        });

        btnLoginClient.setOnClickListener(v -> handleLogin(ClientActivity.class));
        btnLoginMech.setOnClickListener(v -> handleLogin(Mechanic.class));

    }

    private void handleLogin(Class<?> targetActivity) {
        String phoneNumber = phoneNumberEditText.getText().toString();
        String enteredPin = pinEditText.getText().toString();

        if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(enteredPin)) {
            Toast.makeText(MainActivity.this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check Firebase for stored PIN
        databaseReference.child(phoneNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPin = snapshot.child("pin").getValue(String.class);
                    if (storedPin != null && storedPin.equals(enteredPin)) {
                        Toast.makeText(getApplicationContext(), "Login Success", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, targetActivity);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Invalid PIN", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
