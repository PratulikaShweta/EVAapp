package com.example.evaapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.evaapp.adapters.GuardianAdapter;
import com.example.evaapp.models.Guardian;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class GuardianActivity extends AppCompatActivity {

    private RecyclerView rvGuardians;
    private GuardianAdapter adapter;
    private List<Guardian> guardianList;
    private DatabaseReference guardianRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guardian_activity);

        rvGuardians = findViewById(R.id.rvGuardians);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddGuardian);

        rvGuardians.setLayoutManager(new LinearLayoutManager(this));
        guardianList = new ArrayList<>();
        adapter = new GuardianAdapter(guardianList);
        rvGuardians.setAdapter(adapter);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        guardianRef = FirebaseDatabase.getInstance().getReference("guardians").child(userId);

        fabAdd.setOnClickListener(view -> showAddGuardianDialog());

        loadGuardians();
    }

    private void showAddGuardianDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_guardian, null);
        EditText etName = dialogView.findViewById(R.id.etGuardianName);
        EditText etPhone = dialogView.findViewById(R.id.etGuardianPhone);

        new AlertDialog.Builder(this)
                .setTitle("Add Guardian")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                        Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                    } else {
                        addGuardian(name, phone);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addGuardian(String name, String phone) {
        String id = guardianRef.push().getKey();
        Guardian guardian = new Guardian(id, name, phone);
        guardianRef.child(id).setValue(guardian)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Guardian added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadGuardians() {
        guardianRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                guardianList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Guardian guardian = ds.getValue(Guardian.class);
                    if (guardian != null) {
                        guardianList.add(guardian);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuardianActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
