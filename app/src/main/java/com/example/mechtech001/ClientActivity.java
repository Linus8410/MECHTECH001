package com.example.mechtech001;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.*;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class ClientActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private DatabaseReference requestsRef, mechanicLocationsRef;
    private String clientId = "client123"; // Replace with dynamic client ID if needed
    private Button requestButton;
    private String googleMapsApiKey = "AIzaSyDJxV4xHmi1G8NpvohTWm2UCWCIDei_gAM"; // Replace with your API key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        requestButton = findViewById(R.id.requestButton);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        mechanicLocationsRef = FirebaseDatabase.getInstance().getReference("mechanicLocations");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        requestButton.setOnClickListener(view -> requestMechanic());
    }

    // Request a mechanic
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

    // Fetch the status of the request
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

    // Track the mechanic's movement
    private void trackMechanicMovement(String mechanicId) {
        mechanicLocationsRef.child(mechanicId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double latitude = snapshot.child("latitude").getValue(Double.class);
                    Double longitude = snapshot.child("longitude").getValue(Double.class);

                    if (latitude != null && longitude != null) {
                        LatLng mechanicLocation = new LatLng(latitude, longitude);
                        updateMechanicLocationOnMap(mechanicLocation);

                        // Fetch client's current location
                        if (ActivityCompat.checkSelfPermission(ClientActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                                if (location != null) {
                                    LatLng clientLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                    // Draw route and show ETA
                                    fetchAndDrawRoute(mechanicLocation, clientLocation, googleMapsApiKey);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClientActivity.this, "Failed to track mechanic: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update the map with the mechanic's location
    private void updateMechanicLocationOnMap(LatLng mechanicLocation) {
        if (mMap != null) {
            mMap.clear(); // Clear previous markers
            mMap.addMarker(new MarkerOptions().position(mechanicLocation).title("Mechanic Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mechanicLocation, 15));
        }
    }

    // Fetch and draw the route between two locations
    private void fetchAndDrawRoute(LatLng origin, LatLng destination, String apiKey) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DirectionsApiService service = retrofit.create(DirectionsApiService.class);
        String originStr = origin.latitude + "," + origin.longitude;
        String destinationStr = destination.latitude + "," + destination.longitude;

        Call<DirectionsResponse> call = service.getDirections(originStr, destinationStr, apiKey);
        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    if (!directionsResponse.getRoutes().isEmpty()) {
                        DirectionsResponse.Route route = directionsResponse.getRoutes().get(0);
                        String polylinePoints = route.getOverviewPolyline().getPoints();
                        List<LatLng> decodedPath = decodePolyline(polylinePoints);

                        // Draw the polyline on the map
                        if (mMap != null) {
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(10)
                                    .color(Color.BLUE));
                        }

                        // Show ETA
                        if (!route.getLegs().isEmpty()) {
                            String eta = route.getLegs().get(0).getDuration().getText();
                            Toast.makeText(ClientActivity.this, "ETA: " + eta, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(ClientActivity.this, "Failed to fetch route: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Decode the polyline points
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        enableMyLocation();
    }

    // Enable location on the map
    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        mMap.setMyLocationEnabled(true);
        getCurrentLocation();
    }

    // Get the client's current location
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

    // Directions API Service Interface
    public interface DirectionsApiService {
        @GET("maps/api/directions/json")
        Call<DirectionsResponse> getDirections(
                @Query("origin") String origin,
                @Query("destination") String destination,
                @Query("key") String apiKey
        );
    }

    // Directions API Response Model
    public static class DirectionsResponse {
        @SerializedName("routes")
        private List<Route> routes;

        public List<Route> getRoutes() {
            return routes;
        }

        public static class Route {
            @SerializedName("overview_polyline")
            private OverviewPolyline overviewPolyline;

            @SerializedName("legs")
            private List<Leg> legs;

            public OverviewPolyline getOverviewPolyline() {
                return overviewPolyline;
            }

            public List<Leg> getLegs() {
                return legs;
            }
        }

        public static class OverviewPolyline {
            @SerializedName("points")
            private String points;

            public String getPoints() {
                return points;
            }
        }

        public static class Leg {
            @SerializedName("duration")
            private Duration duration;

            public Duration getDuration() {
                return duration;
            }
        }

        public static class Duration {
            @SerializedName("text")
            private String text;

            public String getText() {
                return text;
            }
        }
    }
}