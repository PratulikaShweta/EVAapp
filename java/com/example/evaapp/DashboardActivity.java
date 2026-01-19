package com.example.evaapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {

    private MaterialCardView cardSOS, cardVoiceAssistant, cardGuardians, cardLocation, cardTrackingControl;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize cards
        cardSOS = findViewById(R.id.cardSOS);
        cardVoiceAssistant = findViewById(R.id.cardVoice);
        cardGuardians = findViewById(R.id.cardGuardians);
        cardLocation = findViewById(R.id.cardLocation);
        cardTrackingControl = findViewById(R.id.cardTrackingControl);

        // Set up click actions
        cardSOS.setOnClickListener(v -> startActivity(new Intent(this, SOSActivity.class)));
        cardVoiceAssistant.setOnClickListener(v -> startActivity(new Intent(this, VoiceAssistantActivity.class)));
        cardGuardians.setOnClickListener(v -> startActivity(new Intent(this, GuardianActivity.class)));
        cardLocation.setOnClickListener(v -> startActivity(new Intent(this, LiveLocationActivity.class)));
        cardTrackingControl.setOnClickListener(v -> startActivity(new Intent(this, TrackControlActivity.class)));

        // ✅ Create Notification Channel safely (API 26+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sos_channel",
                    "SOS Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // ✅ Request POST_NOTIFICATIONS permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Optional: Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "🔔 Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
