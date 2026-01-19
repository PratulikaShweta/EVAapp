package com.example.evaapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SOSActivity extends AppCompatActivity {

    private static final String TAG = "SOSActivity";
    private static final int REQ_SEND_SMS = 2001;

    private Button btnSendSOS;

    private FirebaseAuth auth;
    private DatabaseReference guardiansRef;
    private DatabaseReference liveLocationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        btnSendSOS = findViewById(R.id.btnSendSOS);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        guardiansRef = FirebaseDatabase.getInstance().getReference("guardians").child(uid);
        liveLocationRef = FirebaseDatabase.getInstance().getReference("live_locations").child(uid);

        btnSendSOS.setOnClickListener(v -> {
            if (hasSmsPermission()) {
                startSOSFlow();
            } else {
                requestSmsPermission();
            }
        });
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS},
                REQ_SEND_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SEND_SMS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) startSOSFlow();
            else Toast.makeText(this, "SMS permission is required to send SOS", Toast.LENGTH_LONG).show();
        }
    }

    private void startSOSFlow() {
        // 1) Fetch latest live location (if any)
        liveLocationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot locSnap) {
                Double lat = locSnap.child("latitude").getValue(Double.class);
                Double lng = locSnap.child("longitude").getValue(Double.class);

                String message;
                if (lat != null && lng != null) {
                    String maps = "https://maps.google.com/?q=" + lat + "," + lng;
                    message = "🚨 SOS! I need help.\nMy location: " + maps;
                } else {
                    message = "🚨 SOS! I need help. Location not available.";
                }

                // 2) Fetch guardians and send messages
                fetchGuardiansAndSend(message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Live location read failed: " + error.getMessage());
                fetchGuardiansAndSend("🚨 SOS! I need help. Location not available.");
            }
        });
    }

    private void fetchGuardiansAndSend(final String message) {
        guardiansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot guardSnap) {
                List<String> numbers = new ArrayList<>();

                for (DataSnapshot ds : guardSnap.getChildren()) {
                    String phone = null;

                    // Common schema: {id: {phone: "..."}} or {id: "..." }
                    if (ds.child("phone").exists()) {
                        phone = ds.child("phone").getValue(String.class);
                    } else {
                        String maybeNumber = ds.getValue(String.class);
                        if (maybeNumber != null) phone = maybeNumber;
                    }

                    if (phone != null) {
                        phone = phone.trim();
                        if (!phone.isEmpty()) numbers.add(phone);
                    }
                }

                if (numbers.isEmpty()) {
                    Toast.makeText(SOSActivity.this, "No guardian phone numbers found", Toast.LENGTH_LONG).show();
                    return;
                }

                sendSmsToAll(numbers, message);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SOSActivity.this, "Failed to load guardians", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendSmsToAll(List<String> numbers, String message) {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(message); // must be ArrayList for multipart API

        int attempts = 0;
        for (String number : numbers) {
            try {
                if (parts.size() > 1) {
                    sms.sendMultipartTextMessage(number, null, parts, null, null);
                } else {
                    sms.sendTextMessage(number, null, message, null, null);
                }
                attempts++;
            } catch (Exception e) {
                Log.e(TAG, "Failed to send to " + number + ": " + e.getMessage());
            }
        }

        Toast.makeText(this,
                "SOS message queued to " + attempts + " guardian(s)",
                Toast.LENGTH_LONG).show();
    }
}
