package com.example.evaapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    EditText e1;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgotpassword);

        mAuth = FirebaseAuth.getInstance();
        e1=findViewById(R.id.editTextResetEmailAddress);
        btn=findViewById(R.id.btnResetPassword);

        btn.setOnClickListener(view -> {
            String email = e1.getText().toString().trim();
            if (!email.isEmpty()) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity after sending the email
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Error sending reset email.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


