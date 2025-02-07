package com.example.mechtech001;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class ClientActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private EditText edtProblemDetails;
    private RecyclerView recyclerAvailableMechanics;
    private Button btnRequestMechanic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapsclient);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        edtProblemDetails = findViewById(R.id.edtProblemDetails);
        recyclerAvailableMechanics = findViewById(R.id.recyclerAvailableMechanics);
        btnRequestMechanic = findViewById(R.id.btnRequestMechanic);

        // Initialize the map
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Set up the RecyclerView for available mechanics
        recyclerAvailableMechanics.setLayoutManager(new LinearLayoutManager(this));
        // Assume we have a list of available mechanics here, replace with actual data source
        ArrayList<String> availableMechanics = new ArrayList<>();
        availableMechanics.add("Mechanic 1");
        availableMechanics.add("Mechanic 2");
        // Set adapter for RecyclerView (assuming we have a suitable adapter)
        recyclerAvailableMechanics.setAdapter(new MechanicAdapter(availableMechanics));

        // Handle the Request Mechanic button click
        btnRequestMechanic.setOnClickListener(v -> {
            String problemDetails = edtProblemDetails.getText().toString();
            if (!problemDetails.isEmpty()) {
                // Send request to the mechanic (this should include data like the client's location, problem details, etc.)
                Toast.makeText(ClientActivity.this, "Request sent to mechanic", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ClientActivity.this, "Please describe the problem", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        MapsInitializer.initialize(this);

        // Sample client location (replace with actual location fetching code)
        LatLng clientLocation = new LatLng(40.748817, -73.985428); // Example coordinates (New York)
        googleMap.addMarker(new MarkerOptions().position(clientLocation).title("Client Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clientLocation, 15));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}


