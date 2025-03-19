package com.example.mechtech001;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ClientActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private DatabaseReference requestsRef, mechanicsRef;
    private String clientId = "client123"; // Replace with dynamic client ID if needed
    private Button requestButton;
    private ListView mechanicListView;
    private ArrayList<String> mechanicList;
    private MechanicAdapter mechanicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        requestButton = findViewById(R.id.requestButton);
        mechanicListView = findViewById(R.id.MechanicListView);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        mechanicsRef = FirebaseDatabase.getInstance().getReference("mechanics");

        mechanicList = new ArrayList<>();
        mechanicAdapter = new MechanicAdapter(this, mechanicList);
        mechanicListView.setAdapter(mechanicAdapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        requestButton.setOnClickListener(view -> requestMechanic());
    }

    private void requestMechanic() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("ClientLocation", "Client Latitude: " + latitude + ", Longitude: " + longitude);

                // Save the request to Firebase
                requestsRef.child(clientId).setValue(new Request(clientId, latitude, longitude, "pending"))
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Firebase", "Request saved successfully: " + clientId);
                            Toast.makeText(ClientActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
                            fetchRequestStatus(clientId); // Start listening for status updates
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firebase", "Failed to save request: " + e.getMessage());
                            Toast.makeText(ClientActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(ClientActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRequestStatus(String clientId) {
        requestsRef.child(clientId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);

                    if ("accepted".equals(status)) {
                        // Mechanic accepted the request
                        Toast.makeText(ClientActivity.this, "Mechanic is on the way!", Toast.LENGTH_SHORT).show();
                        String mechanicId = snapshot.child("mechanicId").getValue(String.class);
                        if (mechanicId != null) {
                            trackMechanicMovement(mechanicId); // Track mechanic's movement
                        }
                    } else if ("declined".equals(status)) {
                        // Mechanic declined the request
                        Toast.makeText(ClientActivity.this, "Request declined. Find another alternative.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClientActivity.this, "Failed to fetch request status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void trackMechanicMovement(String mechanicId) {
        DatabaseReference mechanicLocationRef = FirebaseDatabase.getInstance()
                .getReference("mechanicLocations")
                .child(mechanicId);

        mechanicLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);

                    if (latitude != null && longitude != null) {
                        LatLng mechanicLocation = new LatLng(latitude, longitude);
                        updateMechanicLocationOnMap(mechanicLocation);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClientActivity.this, "Failed to track mechanic: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMechanicLocationOnMap(LatLng mechanicLocation) {
        if (mMap != null) {
            mMap.clear(); // Clear previous markers
            mMap.addMarker(new MarkerOptions().position(mechanicLocation).title("Mechanic Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 15));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        mMap.setMyLocationEnabled(true);
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng clientLocation = new LatLng(latitude, longitude);

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(clientLocation).title("Your Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clientLocation, 15));
            } else {
                Toast.makeText(ClientActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(ClientActivity.this, "Failed to get location", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}