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
                            fetchNearestMechanics(latitude, longitude);
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

    private void fetchNearestMechanics(double clientLat, double clientLon) {
        Log.d("FetchMechanics", "Fetching mechanics for client location: " + clientLat + ", " + clientLon);

        mechanicsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mechanicList.clear();
                mMap.clear();

                if (!snapshot.exists()) {
                    Log.d("FetchMechanics", "No mechanics found in Firebase.");
                    mechanicList.add("No mechanics available.");
                    mechanicAdapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot mechanicSnapshot : snapshot.getChildren()) {
                    String name = mechanicSnapshot.child("name").getValue(String.class);
                    Double latitude = mechanicSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = mechanicSnapshot.child("longitude").getValue(Double.class);
                    String status = mechanicSnapshot.child("status").getValue(String.class);

                    if (latitude != null && longitude != null && "available".equals(status)) {
                        Log.d("FetchMechanics", "Mechanic: " + name + ", Latitude: " + latitude + ", Longitude: " + longitude);

                        float[] results = new float[1];
                        Location.distanceBetween(clientLat, clientLon, latitude, longitude, results);
                        float distanceInMeters = results[0];

                        if (distanceInMeters <= 10000) { // 10 km radius
                            mechanicList.add(name + " (" + distanceInMeters / 1000 + " km away)");
                            LatLng mechanicLocation = new LatLng(latitude, longitude);
                            mMap.addMarker(new MarkerOptions().position(mechanicLocation).title(name));
                        }
                    }
                }

                if (mechanicList.isEmpty()) {
                    mechanicList.add("No mechanics available within 10 km.");
                }

                mechanicAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FetchMechanics", "Error loading mechanics: " + error.getMessage());
                Toast.makeText(ClientActivity.this, "Error loading mechanics", Toast.LENGTH_SHORT).show();
            }
        });
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