package com.example.evaapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.*;

public class LiveLocationActivity extends AppCompatActivity {

    private MapView mapView;
    private IMapController mapController;
    private static final String TAG = "LiveLocationActivity";

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private final DatabaseReference guardianRef = FirebaseDatabase.getInstance().getReference("guardians");
    private final DatabaseReference sosRef = FirebaseDatabase.getInstance().getReference("sos_alerts");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        setContentView(R.layout.activity_live_location);

        mapView = findViewById(R.id.mapView);
        if (mapView == null) {
            Log.e(TAG, "❌ MapView is null!");
            Toast.makeText(this, "Map failed to load.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(16.0);

        requestPermissionsIfNecessary();
        loadGuardianMarkers();
        loadSOSMarkers();
    }

    private void requestPermissionsIfNecessary() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String perm : permissions) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void loadGuardianMarkers() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e(TAG, "⚠️ User not authenticated.");
            return;
        }

        guardianRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mapView != null) {
                    mapView.getOverlays().clear(); // Clear all old markers
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double lat = ds.child("latitude").getValue(Double.class);
                    Double lon = ds.child("longitude").getValue(Double.class);
                    String name = ds.child("name").getValue(String.class);
                    String phone = ds.child("phone").getValue(String.class);

                    if (lat != null && lon != null) {
                        addMarker(lat, lon, "Guardian: " + name, "Phone: " + phone, R.drawable.guardian_pin);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LiveLocationActivity.this, "Error loading guardians", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSOSMarkers() {
        sosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double lat = ds.child("latitude").getValue(Double.class);
                    Double lon = ds.child("longitude").getValue(Double.class);
                    String time = ds.child("timestamp").getValue(String.class);

                    if (lat != null && lon != null) {
                        addMarker(lat, lon, "🚨 SOS Triggered", "Time: " + time, R.drawable.sos_pin);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LiveLocationActivity.this, "Error loading SOS alerts", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(double lat, double lon, String title, String snippet, int iconRes) {
        if (mapView == null) {
            Log.e(TAG, "❌ mapView is null. Marker skipped.");
            return;
        }

        GeoPoint point = new GeoPoint(lat, lon);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setSubDescription(snippet + "\n" + getAddressFromLocation(lat, lon));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        try {
            marker.setIcon(getDrawable(iconRes));
        } catch (Exception e) {
            Log.e(TAG, "⚠️ Failed to set marker icon: " + e.getMessage());
        }

        mapView.getOverlays().add(marker);
        mapView.invalidate();

        mapController.setCenter(point);
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> list = geocoder.getFromLocation(latitude, longitude, 1);
            if (!list.isEmpty()) {
                Address address = list.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            return "Address not available";
        }
        return "Unknown";
    }
}
