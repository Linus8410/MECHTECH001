package com.example.mechtech001;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

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

public class Mechanic extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference requestsRef;
    private ListView requestListView;
    private ArrayList<String> requestList;
    private MechanicAdapter adapter;
    private Button refreshButton;
    private String mechanicId = "mechanic123"; // Unique mechanic ID (could be dynamic in a real app)

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchClientRequests();

        refreshButton.setOnClickListener(view -> fetchClientRequests());

        requestListView.setOnItemClickListener((parent, view, position, id) -> {
            String clientId = requestList.get(position);
            acceptRequest(clientId);
        });
    }

    private void fetchClientRequests() {
        requestsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                mMap.clear();

                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String clientId = requestSnapshot.getKey();
                    Double latitude = requestSnapshot.child("clientLatitude").getValue(Double.class);
                    Double longitude = requestSnapshot.child("clientLongitude").getValue(Double.class);
                    String status = requestSnapshot.child("status").getValue(String.class);

                    if (latitude != null && longitude != null) {
                        LatLng clientLocation = new LatLng(latitude, longitude);
                        mMap.addMarker(new MarkerOptions().position(clientLocation).title("Client: " + clientId));
                    }

                    if (status != null && status.equals("pending")) {
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng defaultLocation = new LatLng(37.7749, -122.4194); // Default location (San Francisco)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
    }
}
