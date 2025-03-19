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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic);

        initializeUI();
        initializeFirebase();
        initializeMap();
        checkAndRequestLocationPermission();
    }

    private void initializeUI() {
        requestListView = findViewById(R.id.requestListView);
        refreshButton = findViewById(R.id.refreshButton);
        requestList = new ArrayList<>();
        adapter = new MechanicAdapter(this, requestList);
        requestListView.setAdapter(adapter);

        refreshButton.setOnClickListener(view -> fetchClientRequests());
    }

    private void initializeFirebase() {
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error: Map fragment not found!", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchClientRequests() {
        if (mMap != null) {
            mMap.clear();
        }
        requestList.clear();
        adapter.notifyDataSetChanged();

        // Listen for real-time updates
        requestsRef.orderByChild("status").equalTo("pending").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                mMap.clear();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String clientId = requestSnapshot.getKey();
                    Double latitude = requestSnapshot.child("clientLatitude").getValue(Double.class);
                    Double longitude = requestSnapshot.child("clientLongitude").getValue(Double.class);

                    if (latitude != null && longitude != null) {
                        LatLng clientLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(clientLocation).title("Client: " + clientId));
                        requestList.add("Client Request from: " + clientId);
                    }
                }

                if (requestList.isEmpty()) {
                    requestList.add("No service requests available.");
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Mechanic.this, "Error loading requests: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
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
        if (checkLocationPermission()) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10000) // 10 seconds
                    .setFastestInterval(5000); // 5 seconds

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        updateMapWithCurrentLocation(location);
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkLocationPermission()) {
            requestLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private void updateMapWithCurrentLocation(Location location) {
        LatLng mechanicLocation = new LatLng(location.getLatitude(), location.getLongitude());
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(mechanicLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 15));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}