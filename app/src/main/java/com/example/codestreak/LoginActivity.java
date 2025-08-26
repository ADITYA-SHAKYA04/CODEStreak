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

public class LoginActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "CodeStreakPrefs";
    private static final String PREF_LEETCODE_USERNAME = "leetcode_username";
    private static final String PREF_USER_EMAIL = "user_email";
    private static final String PREF_USER_NAME = "user_name";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    
    private MaterialButton manualLoginButton;
    private MaterialButton skipLoginButton;
    private TextInputEditText leetcodeUsernameInput;
    private ProgressBar loginProgressBar;
    private SharedPreferences preferences;

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
            Toast.makeText(this, "Error initializing login", Toast.LENGTH_SHORT).show();
            // Create a default user and proceed
            saveUserData(null, "User", "adityashak04");
            navigateToMainActivity();
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
                saveUserData(null, "User", "adityashak04");
                navigateToMainActivity();
            });
        }
        
        if (manualLoginButton != null) {
            manualLoginButton.setOnClickListener(v -> {
                String username = "";
                if (leetcodeUsernameInput != null) {
                    username = leetcodeUsernameInput.getText().toString().trim();
                }
                
                if (!username.isEmpty()) {
                    final String finalUsername = username; // Make it final for lambda
                    showLoading(true);
                    // Simple delay to show loading, then proceed
                    new android.os.Handler().postDelayed(() -> {
                        saveUserData(null, "User", finalUsername);
                        navigateToMainActivity();
                    }, 500);
                } else {
                    Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void saveUserData(String email, String name, String leetcodeUsername) {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_IS_LOGGED_IN, true);
            editor.putString(PREF_LEETCODE_USERNAME, leetcodeUsername);
            if (email != null) editor.putString(PREF_USER_EMAIL, email);
            if (name != null) editor.putString(PREF_USER_NAME, name);
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
            return prefs.getString(PREF_LEETCODE_USERNAME, "adityashak04");
        } catch (Exception e) {
            e.printStackTrace();
            return "adityashak04"; // Default username
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
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error navigating to main screen", Toast.LENGTH_SHORT).show();
        }
    }
}
