
package com.example.mechtech001;

import android.os.Bundle;

        import androidx.fragment.app.FragmentActivity;

        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.MarkerOptions;

        import java.util.ArrayList;
        import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set user's current location (mock location for example)
        LatLng userLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

        // Mock list of nearby drivers
        List<LatLng> driverLocations = getNearbyDrivers();

        // Add markers for each driver
        for (LatLng driverLocation : driverLocations) {
            mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver"));
        }
    }

    private List<LatLng> getNearbyDrivers() {
        // This would usually come from an API call to your backend
        List<LatLng> driverLocations = new ArrayList<>();
        driverLocations.add(new LatLng(-34.01, 151.02));
        driverLocations.add(new LatLng(-34.02, 151.04));
        driverLocations.add(new LatLng(-34.03, 151.06));
        return driverLocations;
    }
}
