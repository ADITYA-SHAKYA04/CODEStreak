package com.example.codestreak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Example Activity showing Google AI Edge Gallery architecture in action
 * This demonstrates the exact same patterns Google uses for model downloads
 */
public class GoogleArchitectureExampleActivity extends AppCompatActivity {
    private static final String TAG = "GoogleArchExample";
    
    private AISolutionHelper_backup aiHelper;
    private Button authButton;
    private Button downloadButton;
    private Button detectModelsButton;
    private Button cancelButton;
    private TextView statusText;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_architecture_example);
        
        // Initialize AI helper with Google's architecture
        aiHelper = new AISolutionHelper_backup(this);
        
        // Initialize views
        authButton = findViewById(R.id.btnAuthenticateGoogle);
        downloadButton = findViewById(R.id.btnDownloadWithWorkManager);
        detectModelsButton = findViewById(R.id.btnDetectExistingModels);
        cancelButton = findViewById(R.id.btnCancelDownload);
        statusText = findViewById(R.id.tvGoogleStatus);
        progressBar = findViewById(R.id.progressBarGoogle);
        
        setupButtons();
        updateUI();
    }
    
    private void setupButtons() {
        // Gmail Authentication - same as before but integrated with Google's architecture
        authButton.setOnClickListener(v -> authenticateWithGmail());
        
        // Download using Google AI Edge Gallery WorkManager pattern
        downloadButton.setOnClickListener(v -> downloadWithGoogleArchitecture());
        
        // Detect existing Google AI Edge Gallery models
        detectModelsButton.setOnClickListener(v -> detectExistingModels());
        
        // Cancel using Google's pattern
        cancelButton.setOnClickListener(v -> cancelDownload());
    }
    
    private void authenticateWithGmail() {
        statusText.setText("🔐 Starting Gmail authentication...");
        
        aiHelper.authenticateWithGmail(this, new AISolutionHelper_backup.AuthenticationCallback() {
            @Override
            public void onAuthenticationResult(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        statusText.setText("✅ Gmail authentication successful! Ready to download.");
                        Toast.makeText(GoogleArchitectureExampleActivity.this, 
                            "Authentication successful - Now using Google's architecture!", 
                            Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Gmail authentication successful - models updated with auth token");
                    } else {
                        statusText.setText("❌ Authentication failed: " + message);
                        Toast.makeText(GoogleArchitectureExampleActivity.this, 
                            "Authentication failed: " + message, Toast.LENGTH_LONG).show();
                    }
                    updateUI();
                });
            }
        });
    }
    
    private void downloadWithGoogleArchitecture() {
        if (!aiHelper.isGmailAuthEnabled()) {
            Toast.makeText(this, "Please authenticate with Gmail first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        statusText.setText("🚀 Starting download with Google AI Edge Gallery architecture...");
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setProgress(0);
        
        // Use Google's WorkManager-based download system with Hugging Face auth
        aiHelper.downloadModelWithWorkManager(this, new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    statusText.setText("📥 " + status);
                    Log.d(TAG, "Download progress: " + progress + "% - " + status);
                });
            }
            
            @Override
            public void onComplete(String modelPath) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    statusText.setText("✅ Model downloaded successfully using Google's architecture!");
                    Toast.makeText(GoogleArchitectureExampleActivity.this, 
                        "Model downloaded to: " + modelPath, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Google architecture download completed successfully: " + modelPath);
                    updateUI();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    statusText.setText("❌ Download failed: " + error);
                    Toast.makeText(GoogleArchitectureExampleActivity.this, 
                        "Download failed: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Google architecture download failed: " + error);
                    updateUI();
                });
            }
        });
    }
    
    private void cancelDownload() {
        aiHelper.cancelModelDownload();
        progressBar.setVisibility(ProgressBar.GONE);
        statusText.setText("🛑 Download cancelled using Google's WorkManager");
        Toast.makeText(this, "Download cancelled", Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    private void detectExistingModels() {
        statusText.setText("🔍 Scanning for existing Google AI Edge Gallery models...");
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setProgress(0);
        
        aiHelper.detectExistingGoogleModels(this, new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    statusText.setText("🔍 " + status);
                    Log.d(TAG, "Detection progress: " + progress + "% - " + status);
                });
            }
            
            @Override
            public void onComplete(String modelPath) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if ("smart_fallback".equals(modelPath)) {
                        statusText.setText("⚡ Using Smart Fallback mode");
                        Toast.makeText(GoogleArchitectureExampleActivity.this, 
                            "Smart Fallback mode activated!", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("✅ Using existing model: " + new java.io.File(modelPath).getName());
                        Toast.makeText(GoogleArchitectureExampleActivity.this, 
                            "Successfully configured existing model!", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "Existing model setup completed: " + modelPath);
                    updateUI();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    statusText.setText("❌ " + error);
                    Toast.makeText(GoogleArchitectureExampleActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Model detection error: " + error);
                    updateUI();
                });
            }
        });
    }
    
    private void updateUI() {
        boolean isAuthenticated = aiHelper.isGmailAuthEnabled();
        
        authButton.setText(isAuthenticated ? "Re-authenticate Gmail" : "Authenticate with Gmail");
        downloadButton.setEnabled(isAuthenticated);
        cancelButton.setEnabled(isAuthenticated);
        
        if (!isAuthenticated) {
            statusText.setText("🔑 Ready to authenticate with Google's architecture");
            progressBar.setVisibility(ProgressBar.GONE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // Handle Gmail authentication result
        aiHelper.handleGmailAuthResult(requestCode, resultCode, data, this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiHelper != null) {
            aiHelper.cleanup();
        }
    }
}
