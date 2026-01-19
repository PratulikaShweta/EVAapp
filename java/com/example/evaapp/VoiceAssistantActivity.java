package com.example.evaapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class VoiceAssistantActivity extends AppCompatActivity {

    private static final int REQ_AUDIO = 200;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;

    private TextView tvVoiceStatus;
    private ImageView ivMic;
    private Button btnStart;

    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        ivMic = findViewById(R.id.ivMic);
        btnStart = findViewById(R.id.btnStartListening);

        checkMicPermission();
        initSpeech();
        initTTS();

        btnStart.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });
    }

    private void checkMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
    }

    private void initSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
    }

    private void initTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void startListening() {
        isListening = true;
        tvVoiceStatus.setText("Listening... Say 'help' or 'sos'");
        btnStart.setText("Stop Voice Assistant");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                tvVoiceStatus.setText("Error. Try again.");
                restartListening();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0).toLowerCase();
                    tvVoiceStatus.setText("Heard: " + spoken);

                    if (spoken.contains("help") || spoken.contains("sos")) {
                        speak("SOS triggered. Help is on the way!");
                        triggerSOSInFirebase();
                        stopListening();
                    } else {
                        tvVoiceStatus.setText("Waiting for 'help' or 'sos'...");
                        restartListening();
                    }
                } else {
                    restartListening();
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void stopListening() {
        isListening = false;
        try {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
        } catch (Exception ignored) {}

        btnStart.setText("Start Voice Assistant");
        tvVoiceStatus.setText("Voice assistant stopped");
    }

    private void restartListening() {
        if (isListening) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}

            initSpeech();
            startListening();
        }
    }

    private void speak(String msg) {
        textToSpeech.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void triggerSOSInFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown_user";

        HashMap<String, Object> sosMap = new HashMap<>();
        sosMap.put("userId", userId);
        sosMap.put("timestamp", System.currentTimeMillis());
        sosMap.put("triggeredBy", "Voice Command");
        sosMap.put("status", "active");

        FirebaseDatabase.getInstance().getReference("sos")
                .push().setValue(sosMap)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "SOS sent!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send SOS", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) textToSpeech.shutdown();
        super.onDestroy();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_AUDIO && (grantResults.length == 0 ||
                grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Mic permission needed", Toast.LENGTH_SHORT).show();
        }
    }
}
