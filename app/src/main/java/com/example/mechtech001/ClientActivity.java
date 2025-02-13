package com.example.mechtech001;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class ClientActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location currentLocation;
    private DatabaseReference requestsRef;
    private String clientId = "client123"; // Unique client ID (could be dynamic in a real app)
    private Spinner mechanicSpinner;
    private Button requestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        mechanicSpinner = findViewById(R.id.mechanicSpinner);
        requestButton = findViewById(R.id.requestButton);

        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        requestButton.setOnClickListener(view -> {
            String selectedMechanic = mechanicSpinner.getSelectedItem().toString();
            requestMechanic(selectedMechanic);
        });

        fetchLocation();
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
            }
        });
    }

    private void requestMechanic(String mechanicName) {
        if (currentLocation == null) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude = currentLocation.getLatitude();
        double longitude = currentLocation.getLongitude();

        HashMap<String, Object> requestData = new HashMap<>();
        requestData.put("mechanicName", mechanicName);
        requestData.put("clientLatitude", latitude);
        requestData.put("clientLongitude", longitude);
        requestData.put("status", "pending");

        requestsRef.child(clientId).setValue(requestData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Request sent to " + mechanicName, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
    }
}
