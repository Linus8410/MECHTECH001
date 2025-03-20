package com.example.mechtech001;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private String googleMapsApiKey = "AIzaSyDJxV4xHmi1G8NpvohTWm2UCWCIDei_gAM"; // Replace with your API key
    private Polyline currentRoutePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic);

        initializeUI();
        initializeFirebase();
        initializeMap();
        checkAndRequestLocationPermission();
    }

    // Initialize UI components
    private void initializeUI() {
        requestListView = findViewById(R.id.requestListView);
        refreshButton = findViewById(R.id.refreshButton);
        requestList = new ArrayList<>();
        adapter = new MechanicAdapter(this, requestList);
        requestListView.setAdapter(adapter);

        refreshButton.setOnClickListener(view -> fetchClientRequests());
    }

    // Initialize Firebase references
    private void initializeFirebase() {
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    // Initialize Google Map
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error: Map fragment not found!", Toast.LENGTH_LONG).show();
        }
    }

    // Fetch client requests from Firebase
    private void fetchClientRequests() {
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

                        // Show dialog for new requests
                        showRequestDialog(clientId, clientLocation);
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

    // Show a dialog to accept or decline a request
    private void showRequestDialog(String clientId, LatLng clientLocation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Request");
        builder.setMessage("You have a new request from Client: " + clientId);

        // Inflate the custom layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_accept_decline, null);
        builder.setView(dialogView);

        // Initialize buttons
        Button acceptButton = dialogView.findViewById(R.id.acceptButton);
        Button declineButton = dialogView.findViewById(R.id.declineButton);

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Handle Accept button click
        acceptButton.setOnClickListener(v -> {
            acceptRequest(clientId, clientLocation);
            dialog.dismiss();
        });

        // Handle Decline button click
        declineButton.setOnClickListener(v -> {
            declineRequest(clientId);
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }

    // Accept the request and start sharing location
    private void acceptRequest(String clientId, LatLng clientLocation) {
        requestsRef.child(clientId).child("status").setValue("accepted")
                .addOnSuccessListener(aVoid -> {
                    // Start sharing the mechanic's location
                    startSharingLocation("mechanicId"); // Replace with actual mechanic ID

                    // Fetch mechanic's current location
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                            if (location != null) {
                                LatLng mechanicLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                // Draw route and show ETA
                                fetchAndDrawRoute(mechanicLocation, clientLocation, googleMapsApiKey);
                            }
                        });
                    }

                    Toast.makeText(Mechanic.this, "Request accepted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Mechanic.this, "Failed to accept request", Toast.LENGTH_SHORT).show();
                });
    }

    // Decline the request
    private void declineRequest(String clientId) {
        requestsRef.child(clientId).child("status").setValue("declined")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Mechanic.this, "Request declined!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Mechanic.this, "Failed to decline request", Toast.LENGTH_SHORT).show();
                });
    }

    // Start sharing the mechanic's location
    private void startSharingLocation(String mechanicId) {
        if (checkLocationPermission()) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000) // Update every 5 seconds
                    .setFastestInterval(3000); // Fastest update interval

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        // Update Firebase with the mechanic's location
                        DatabaseReference mechanicLocationRef = FirebaseDatabase.getInstance()
                                .getReference("mechanicLocations")
                                .child(mechanicId);

                        mechanicLocationRef.child("latitude").setValue(location.getLatitude());
                        mechanicLocationRef.child("longitude").setValue(location.getLongitude());
                        mechanicLocationRef.child("timestamp").setValue(System.currentTimeMillis());
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
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
                            if (currentRoutePolyline != null) {
                                currentRoutePolyline.remove(); // Remove the old polyline
                            }
                            currentRoutePolyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(10)
                                    .color(Color.BLUE));
                        }

                        // Show ETA
                        if (!route.getLegs().isEmpty()) {
                            String eta = route.getLegs().get(0).getDuration().getText();
                            Toast.makeText(Mechanic.this, "ETA: " + eta, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(Mechanic.this, "Failed to fetch route: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    // Check and request location permission
    private void checkAndRequestLocationPermission() {
        if (checkLocationPermission()) {
            requestLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    // Check if location permission is granted
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Request location permission
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    // Request location updates
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

    // Update the map with the current location
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