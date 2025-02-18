package com.example.mechtech001;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        requestListView = findViewById(R.id.requestListView);
        refreshButton = findViewById(R.id.refreshButton);

        requestList = new ArrayList<>();
        adapter = new MechanicAdapter(this, requestList);
        requestListView.setAdapter(adapter);

        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchClientRequests();
        listenForRequests();  // Listen for new requests in real-time

        refreshButton.setOnClickListener(view -> fetchClientRequests());

        requestListView.setOnItemClickListener((parent, view, position, id) -> {
            String clientId = requestList.get(position);
            acceptRequest(clientId);
        });

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

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void updateMapWithCurrentLocation(Location location) {
        if (mMap != null && location != null) {
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
        } else {
            Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchClientRequests() {
        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                if (mMap != null) {
                    mMap.clear();
                }

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
                    Toast.makeText(Mechanic.this, "Request Accepted!", Toast.LENGTH_SHORT).show();
                    notifyClient(clientId);
                })
                .addOnFailureListener(e -> Toast.makeText(Mechanic.this, "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    private void notifyClient(String clientId) {
        requestsRef.child(clientId).child("message").setValue("Mechanic is on the way!");
    }

    private void listenForRequests() {
        requestsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String clientId = snapshot.getKey();
                Toast.makeText(Mechanic.this, "New request from " + clientId, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (checkLocationPermission()) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            updateMapWithCurrentLocation(location);
                        } else {
                            Toast.makeText(Mechanic.this, "Unable to fetch last known location", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            requestLocationPermission();
        }

        LatLng defaultLocation = new LatLng(37.7749, -122.4194);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}

