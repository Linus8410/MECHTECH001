package com.example.mechtech001;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.*;

import java.util.ArrayList;

public class Mechanic extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference requestsRef;
    private ListView requestListView;
    private ArrayList<String> requestList;
    private MechanicAdapter adapter;
    private Button refreshButton;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic);

        initializeUI();
        initializeFirebase();
        initializeMap();
        setListeners();
        checkAndRequestLocationPermission();
    }

    private void initializeUI() {
        requestListView = findViewById(R.id.requestListView);
        refreshButton = findViewById(R.id.refreshButton);
        requestList = new ArrayList<>();
        adapter = new MechanicAdapter(this, requestList);
        requestListView.setAdapter(adapter);
    }

    private void initializeFirebase() {
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 🔥 Listen for real-time updates 🔥
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                if (mMap != null) mMap.clear();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String clientId = requestSnapshot.getKey();
                    Double latitude = requestSnapshot.child("clientLatitude").getValue(Double.class);
                    Double longitude = requestSnapshot.child("clientLongitude").getValue(Double.class);
                    String status = requestSnapshot.child("status").getValue(String.class);

                    if (latitude != null && longitude != null) {
                        LatLng clientLocation = new LatLng(latitude, longitude);
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions().position(clientLocation).title("Client: " + clientId));
                        }
                    }

                    if ("pending".equals(status)) {
                        requestList.add(clientId);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Mechanic.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setListeners() {
        refreshButton.setOnClickListener(view -> fetchClientRequests());

        requestListView.setOnItemClickListener((parent, view, position, id) -> {
            String clientId = requestList.get(position);

            new AlertDialog.Builder(Mechanic.this)
                    .setTitle("Request Action")
                    .setMessage("Do you want to accept or decline this request?")
                    .setPositiveButton("Accept", (dialog, which) -> acceptRequest(clientId))
                    .setNegativeButton("Decline", (dialog, which) -> declineRequest(clientId))
                    .setNeutralButton("Cancel", null)
                    .show();
        });
    }

    private void checkAndRequestLocationPermission() {
        if (checkLocationPermission()) {
            requestLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateMapWithCurrentLocation(location);
            } else {
                Toast.makeText(Mechanic.this, "Failed to get location. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchClientRequests() {
        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                if (mMap != null) mMap.clear();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String clientId = requestSnapshot.getKey();
                    Double latitude = requestSnapshot.child("clientLatitude").getValue(Double.class);
                    Double longitude = requestSnapshot.child("clientLongitude").getValue(Double.class);
                    String status = requestSnapshot.child("status").getValue(String.class);

                    if (latitude != null && longitude != null) {
                        LatLng clientLocation = new LatLng(latitude, longitude);
                        if (mMap != null) {
                            mMap.addMarker(new MarkerOptions().position(clientLocation).title("Client: " + clientId));
                        }
                    }

                    if ("pending".equals(status)) {
                        requestList.add(clientId);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Mechanic.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptRequest(String clientId) {
        requestsRef.child(clientId).child("status").setValue("accepted")
                .addOnSuccessListener(aVoid -> {
                    notifyClient(clientId, "Mechanic is on the way!");
                    Toast.makeText(Mechanic.this, "Request Accepted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(Mechanic.this, "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    private void declineRequest(String clientId) {
        requestsRef.child(clientId).child("status").setValue("declined")
                .addOnSuccessListener(aVoid -> {
                    notifyClient(clientId, "Mechanic is unavailable. Please request again later.");
                    Toast.makeText(Mechanic.this, "Request Declined!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(Mechanic.this, "Failed to decline request", Toast.LENGTH_SHORT).show());
    }

    private void notifyClient(String clientId, String message) {
        requestsRef.child(clientId).child("message").setValue(message);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkLocationPermission()) {
            requestLocationUpdates();
        } else {
            requestLocationPermission();
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.7749, -122.4194), 10));
    }

    private void updateMapWithCurrentLocation(Location location) {
        LatLng mechanicLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(mechanicLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 15));
        }
    }
}
