package com.example.codestreak.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.browser.customtabs.CustomTabsIntent;

/**
 * HuggingFaceAuthManager - Handles OAuth authentication with HuggingFace
 * Follows the same pattern as Google AI Edge Gallery
 */
public class HuggingFaceAuthManager {
    private static final String TAG = "HFAuthManager";
    private static final String PREFS_NAME = "huggingface_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    
    // HuggingFace OAuth endpoints
    private static final String HF_AUTH_URL = "https://huggingface.co/oauth/authorize";
    private static final String HF_TOKEN_URL = "https://huggingface.co/oauth/token";
    
    // OAuth configuration - You'll need to register your app at https://huggingface.co/settings/applications
    private static final String CLIENT_ID = "your_client_id"; // TODO: Register app and add client ID
    private static final String REDIRECT_URI = "codestreak://huggingface/callback";
    private static final String SCOPE = "inference-api"; // Scope for model access
    
    private final Context context;
    private final SharedPreferences prefs;
    
    public interface AuthCallback {
        void onAuthSuccess(String accessToken);
        void onAuthError(String error);
    }
    
    public HuggingFaceAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        long expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0);
        
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        // Check if token is expired
        return System.currentTimeMillis() < expiry;
    }
    
    /**
     * Get stored access token
     */
    public String getAccessToken() {
        if (isAuthenticated()) {
            return prefs.getString(KEY_ACCESS_TOKEN, null);
        }
        return null;
    }
    
    /**
     * Start OAuth authentication flow
     * Opens HuggingFace login in Custom Chrome Tab (like Google AI Edge Gallery does)
     */
    public void startAuthentication(Activity activity, AuthCallback callback) {
        Log.d(TAG, "Starting HuggingFace OAuth flow");
        
        // Build OAuth URL
        Uri.Builder authUriBuilder = Uri.parse(HF_AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPE)
                .appendQueryParameter("state", generateState());
        
        Uri authUri = authUriBuilder.build();
        
        // Open in Custom Chrome Tab (same as Gallery app)
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setShowTitle(true);
        builder.setUrlBarHidingEnabled(true);
        
        CustomTabsIntent customTabsIntent = builder.build();
        
        try {
            customTabsIntent.launchUrl(activity, authUri);
            Log.d(TAG, "Launched auth URL: " + authUri.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch auth", e);
            callback.onAuthError("Failed to open authentication: " + e.getMessage());
        }
    }
    
    /**
     * Handle OAuth callback with authorization code
     * Call this from your deep link handler
     */
    public void handleAuthCallback(Uri callbackUri, AuthCallback callback) {
        Log.d(TAG, "Handling auth callback: " + callbackUri.toString());
        
        String code = callbackUri.getQueryParameter("code");
        String error = callbackUri.getQueryParameter("error");
        
        if (error != null) {
            Log.e(TAG, "Auth error: " + error);
            callback.onAuthError("Authentication failed: " + error);
            return;
        }
        
        if (code == null) {
            Log.e(TAG, "No authorization code received");
            callback.onAuthError("No authorization code received");
            return;
        }
        
        // Exchange code for access token
        exchangeCodeForToken(code, callback);
    }
    
    /**
     * Exchange authorization code for access token
     */
    private void exchangeCodeForToken(String code, AuthCallback callback) {
        // This should be done in a background thread
        new Thread(() -> {
            try {
                // TODO: Implement token exchange
                // For now, we'll use a simpler approach - direct token from HuggingFace settings
                Log.d(TAG, "Token exchange not implemented - guide user to create token manually");
                
                // Notify that manual token setup is needed
                callback.onAuthError("Please create a token manually at https://huggingface.co/settings/tokens");
                
            } catch (Exception e) {
                Log.e(TAG, "Token exchange failed", e);
                callback.onAuthError("Token exchange failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Manually set access token (simpler approach for now)
     * User can create token at: https://huggingface.co/settings/tokens
     */
    public void setAccessToken(String token) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)) // 1 year
                .apply();
        Log.d(TAG, "Access token stored");
    }
    
    /**
     * Clear authentication
     */
    public void logout() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Logged out");
    }
    
    /**
     * Generate random state for OAuth security
     */
    private String generateState() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    /**
     * Simplified flow: Guide user to create token manually
     * This is easier than full OAuth and works just as well
     */
    public static void showTokenSetupGuide(Activity activity, AuthCallback callback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("🔑 HuggingFace Authentication Required");
        builder.setMessage(
            "To download models, you need a HuggingFace token:\n\n" +
            "1. Visit: https://huggingface.co/settings/tokens\n" +
            "2. Click 'Create new token'\n" +
            "3. Name it 'CODEStreak'\n" +
            "4. Select 'Read' permission\n" +
            "5. Copy the token\n" +
            "6. Paste it in the next dialog\n\n" +
            "Don't have an account? It's free!"
        );
        
        builder.setPositiveButton("Open HuggingFace", (dialog, which) -> {
            // Open HuggingFace token settings
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://huggingface.co/settings/tokens"));
            activity.startActivity(browserIntent);
            
            // After opening, show input dialog
            showTokenInputDialog(activity, callback);
        });
        
        builder.setNeutralButton("I Have a Token", (dialog, which) -> {
            // User already has token, skip directly to input
            showTokenInputDialog(activity, callback);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            callback.onAuthError("Authentication cancelled");
        });
        
        builder.show();
    }
    
    /**
     * Show dialog to input token manually
     */
    private static void showTokenInputDialog(Activity activity, AuthCallback callback) {
        // Wait a bit for user to potentially create token
        activity.runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
            builder.setTitle("📋 Paste Your Token");
            
            final android.widget.EditText input = new android.widget.EditText(activity);
            input.setHint("hf_xxxxxxxxxxxxx");
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            
            // Add padding
            int padding = 50;
            input.setPadding(padding, padding, padding, padding);
            
            builder.setView(input);
            
            builder.setPositiveButton("Save & Continue", (dialog, which) -> {
                String token = input.getText().toString().trim();
                if (token.isEmpty() || !token.startsWith("hf_")) {
                    callback.onAuthError("Invalid token format. Token should start with 'hf_'");
                    return;
                }
                
                // Save token
                HuggingFaceAuthManager authManager = new HuggingFaceAuthManager(activity);
                authManager.setAccessToken(token);
                
                callback.onAuthSuccess(token);
            });
            
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                callback.onAuthError("Authentication cancelled");
            });
            
            builder.show();
        });
    }
}
