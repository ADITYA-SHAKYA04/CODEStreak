package com.example.codestreak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Example Activity showing how to use Gmail authentication for AI model downloads
 */
public class GmailAuthExampleActivity extends AppCompatActivity {
    private static final String TAG = "GmailAuthExample";
    
    private AISolutionHelper_backup aiHelper;
    private Button authButton;
    private Button downloadButton;
    private Button detectModelsButton;
    private TextView statusText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gmail_auth_example); // You'll need to create this layout
        
        // Initialize AI helper
        aiHelper = new AISolutionHelper_backup(this);
        
        // Initialize views
        authButton = findViewById(R.id.btnAuthenticate);
        downloadButton = findViewById(R.id.btnDownloadModel);
        detectModelsButton = findViewById(R.id.btnDetectModels);
        statusText = findViewById(R.id.tvStatus);
        
        setupButtons();
        updateUI();
    }
    
    private void setupButtons() {
        authButton.setOnClickListener(v -> authenticateWithGmail());
        downloadButton.setOnClickListener(v -> downloadModel());
        detectModelsButton.setOnClickListener(v -> detectExistingModels());
    }
    
    private void authenticateWithGmail() {
        statusText.setText("Starting Gmail authentication...");
        
        aiHelper.authenticateWithGmail(this, new AISolutionHelper_backup.AuthenticationCallback() {
            @Override
            public void onAuthenticationResult(boolean success, String message) {
                runOnUiThread(() -> {
                    if (success) {
                        statusText.setText("✅ Authentication successful!");
                        Toast.makeText(GmailAuthExampleActivity.this, "Gmail authentication successful", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Gmail authentication successful");
                    } else {
                        statusText.setText("❌ Authentication failed: " + message);
                        Toast.makeText(GmailAuthExampleActivity.this, "Authentication failed: " + message, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Gmail authentication failed: " + message);
                    }
                    updateUI();
                });
            }
        });
    }
    
    private void downloadModel() {
        if (!aiHelper.isGmailAuthEnabled()) {
            Toast.makeText(this, "Please authenticate with Gmail first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        statusText.setText("Downloading AI model with Gmail authentication...");
        
        // Use the existing downloadModelWithRetry method which now supports Gmail auth
        aiHelper.downloadModelWithRetry(new AISolutionHelper_backup.ModelDownloadCallback() {
            @Override
            public void onDownloadProgress(int progress) {
                runOnUiThread(() -> {
                    statusText.setText("Downloading: " + progress + "%");
                });
            }
            
            @Override
            public void onDownloadComplete(String modelPath) {
                runOnUiThread(() -> {
                    statusText.setText("✅ Model downloaded successfully!");
                    Toast.makeText(GmailAuthExampleActivity.this, "Model downloaded to: " + modelPath, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Model downloaded successfully to: " + modelPath);
                });
            }
            
            @Override
            public void onDownloadError(String error) {
                runOnUiThread(() -> {
                    statusText.setText("❌ Download failed: " + error);
                    Toast.makeText(GmailAuthExampleActivity.this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Model download failed: " + error);
                });
            }
        });
    }
    
    private void detectExistingModels() {
        statusText.setText("🔍 Scanning for existing Google AI Edge Gallery models...");
        
        aiHelper.detectExistingGoogleModels(this, new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                runOnUiThread(() -> {
                    statusText.setText("🔍 " + status);
                    Log.d(TAG, "Detection progress: " + progress + "% - " + status);
                });
            }
            
            @Override
            public void onComplete(String modelPath) {
                runOnUiThread(() -> {
                    if ("smart_fallback".equals(modelPath)) {
                        statusText.setText("⚡ Using Smart Fallback mode");
                        Toast.makeText(GmailAuthExampleActivity.this, 
                            "Smart Fallback mode activated!", Toast.LENGTH_SHORT).show();
                    } else {
                        statusText.setText("✅ Using existing model: " + new java.io.File(modelPath).getName());
                        Toast.makeText(GmailAuthExampleActivity.this, 
                            "Successfully configured existing model!", Toast.LENGTH_SHORT).show();
                    }
                    Log.d(TAG, "Existing model setup completed: " + modelPath);
                    updateUI();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    statusText.setText("❌ " + error);
                    Toast.makeText(GmailAuthExampleActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Model detection error: " + error);
                    updateUI();
                });
            }
        });
    }
    
    private void updateUI() {
        boolean isAuthenticated = aiHelper.isGmailAuthEnabled();
        authButton.setText(isAuthenticated ? "Re-authenticate" : "Authenticate with Gmail");
        downloadButton.setEnabled(isAuthenticated);
        
        if (!isAuthenticated) {
            statusText.setText("Ready to authenticate");
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
