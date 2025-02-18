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
    private String clientId = "client123";
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

                requestsRef.child(clientId).setValue(new Request(clientId, latitude, longitude, "pending"))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ClientActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
                            fetchNearestMechanics(latitude, longitude);
                        })
                        .addOnFailureListener(e -> Toast.makeText(ClientActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void fetchNearestMechanics(double clientLat, double clientLon) {
        mechanicsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mechanicList.clear();
                mMap.clear();

                for (DataSnapshot mechanicSnapshot : snapshot.getChildren()) {
                    String name = mechanicSnapshot.child("name").getValue(String.class);
                    double latitude = mechanicSnapshot.child("latitude").getValue(Double.class);
                    double longitude = mechanicSnapshot.child("longitude").getValue(Double.class);

                    float[] results = new float[1];
                    Location.distanceBetween(clientLat, clientLon, latitude, longitude, results);
                    float distanceInMeters = results[0];

                    if (distanceInMeters <= 5000) { // Mechanics within 5 km
                        mechanicList.add(name + " (" + distanceInMeters / 1000 + " km away)");
                        LatLng mechanicLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(mechanicLocation).title(name));
                    }
                }
                mechanicAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClientActivity.this, "Error loading mechanics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }
}
