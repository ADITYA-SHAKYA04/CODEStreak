package com.example.codestreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "CodeStreakPrefs";
    private static final String PREF_LEETCODE_USERNAME = "leetcode_username";
    private static final String PREF_USER_EMAIL = "user_email";
    private static final String PREF_USER_NAME = "user_name";
    private static final String PREF_USER_AVATAR = "user_avatar";
    private static final String PREF_USER_REAL_NAME = "user_real_name";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    
    private MaterialButton manualLoginButton;
    private MaterialButton skipLoginButton;
    private TextInputEditText leetcodeUsernameInput;
    private ProgressBar loginProgressBar;
    private SharedPreferences preferences;
    private LeetCodeAPI leetCodeAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_login);
            
            // Remove action bar for cleaner look
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }
            
            // Initialize SharedPreferences
            preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            
            // Initialize LeetCode API
            leetCodeAPI = new LeetCodeAPI();
            
            // Check if user is already logged in
            if (isUserLoggedIn()) {
                navigateToMainActivity();
                return;
            }
            
            // Initialize only essential views
            initializeEssentialViews();
            setupSimpleClickListeners();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing login. Please try again.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void initializeEssentialViews() {
        try {
            manualLoginButton = findViewById(R.id.manualLoginButton);
            skipLoginButton = findViewById(R.id.skipLoginButton);
            leetcodeUsernameInput = findViewById(R.id.leetcodeUsernameInput);
            loginProgressBar = findViewById(R.id.loginProgressBar);
            
            // Hide Google Sign-In button for now to avoid Google Play Services issues
            View googleButton = findViewById(R.id.googleSignInButton);
            if (googleButton != null) {
                googleButton.setVisibility(View.GONE);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding views", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupSimpleClickListeners() {
        if (skipLoginButton != null) {
            skipLoginButton.setOnClickListener(v -> {
                try {
                    android.util.Log.d("LoginActivity", "Skip button clicked");
                    
                    // Enable guest mode - save flag to SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("is_guest_mode", true);
                    editor.putString("username", "Guest");
                    editor.putString("leetcode_username", "");
                    editor.putBoolean(PREF_IS_LOGGED_IN, true);
                    boolean saved = editor.commit();
                    
                    android.util.Log.d("LoginActivity", "Guest mode saved: " + saved);
                    
                    Toast.makeText(this, "Continuing as Guest", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to main activity
                    Intent intent = new Intent(LoginActivity.this, ModernMainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    
                    android.util.Log.d("LoginActivity", "Navigation started");
                } catch (Exception e) {
                    android.util.Log.e("LoginActivity", "Error in skip button: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            android.util.Log.e("LoginActivity", "Skip button is null!");
        }
        
        if (manualLoginButton != null) {
            manualLoginButton.setOnClickListener(v -> {
                String username = "";
                if (leetcodeUsernameInput != null) {
                    username = leetcodeUsernameInput.getText().toString().trim();
                }
                
                if (!username.isEmpty()) {
                    validateAndLogin(username);
                } else {
                    Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void validateAndLogin(String username) {
        showLoading(true);
        
        // Validate username with LeetCode API
        leetCodeAPI.getUserProfile(username, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        
                        // Check if matchedUser exists
                        if (jsonResponse.has("data")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            
                            if (data.has("matchedUser") && !data.isNull("matchedUser")) {
                                JSONObject matchedUser = data.getJSONObject("matchedUser");
                                String validatedUsername = matchedUser.getString("username");
                                
                                // Extract profile information
                                String realName = null;
                                String avatarUrl = null;
                                String email = null;
                                
                                if (matchedUser.has("profile") && !matchedUser.isNull("profile")) {
                                    JSONObject profile = matchedUser.getJSONObject("profile");
                                    if (profile.has("realName") && !profile.isNull("realName")) {
                                        realName = profile.getString("realName");
                                    }
                                    if (profile.has("userAvatar") && !profile.isNull("userAvatar")) {
                                        avatarUrl = profile.getString("userAvatar");
                                    }
                                }
                                
                                // Debug logging
                                android.util.Log.d("LoginActivity", "Username: " + validatedUsername);
                                android.util.Log.d("LoginActivity", "Real Name: " + realName);
                                android.util.Log.d("LoginActivity", "Avatar URL: " + avatarUrl);
                                android.util.Log.d("LoginActivity", "Full Response: " + response);
                                
                                if (matchedUser.has("emails") && !matchedUser.isNull("emails")) {
                                    org.json.JSONArray emails = matchedUser.getJSONArray("emails");
                                    if (emails.length() > 0) {
                                        JSONObject emailObj = emails.getJSONObject(0);
                                        if (emailObj.has("email")) {
                                            email = emailObj.getString("email");
                                        }
                                    }
                                }
                                
                                // Username is valid, proceed with login
                                saveUserData(email, realName, validatedUsername, avatarUrl);
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                navigateToMainActivity();
                            } else {
                                // Username doesn't exist
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, 
                                    "Username '" + username + "' not found on LeetCode. Please check and try again.", 
                                    Toast.LENGTH_LONG).show();
                            }
                        } else {
                            showLoading(false);
                            Toast.makeText(LoginActivity.this, "Invalid response from LeetCode", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        showLoading(false);
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, 
                            "Error validating username: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, 
                        "Failed to validate username. Please check your internet connection and try again.", 
                        Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                });
            }
        });
    }
    
    private void saveUserData(String email, String name, String leetcodeUsername, String avatarUrl) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_IS_LOGGED_IN, true);
            editor.putString(PREF_LEETCODE_USERNAME, leetcodeUsername);
            if (email != null) editor.putString(PREF_USER_EMAIL, email);
            if (name != null) {
                editor.putString(PREF_USER_NAME, name);
                editor.putString(PREF_USER_REAL_NAME, name);
            }
            if (avatarUrl != null) editor.putString(PREF_USER_AVATAR, avatarUrl);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isUserLoggedIn() {
        try {
            return preferences.getBoolean(PREF_IS_LOGGED_IN, false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static String getLeetCodeUsername(android.content.Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            return prefs.getString(PREF_LEETCODE_USERNAME, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void logout(android.content.Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showLoading(boolean show) {
        if (loginProgressBar != null) {
            loginProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (manualLoginButton != null) {
            manualLoginButton.setEnabled(!show);
        }
        if (skipLoginButton != null) {
            skipLoginButton.setEnabled(!show);
        }
    }
    
    private void navigateToMainActivity() {
        try {
            Intent intent = new Intent(LoginActivity.this, ModernMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error navigating to main screen", Toast.LENGTH_SHORT).show();
        }
    }
}
