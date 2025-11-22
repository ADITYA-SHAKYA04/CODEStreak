package com.example.codestreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileActivity extends BaseActivity {

    private TextView usernameText, fullNameText, rankText, reputationText;
    private TextView totalSolvedText, easySolvedText, mediumSolvedText, hardSolvedText;
    private TextView acceptanceRateText, streakCountText, contestRatingText;
    private ImageView profileAvatar;
    private Chip easyChip, mediumChip, hardChip;
    private MaterialCardView statsCard, problemsCard, contestCard, badgesCard;
    private MaterialButton logoutButton;
    private ImageButton backButton;
    private ViewStub skeletonStub;
    private View skeletonView;
    private View profileContentScroll;
    private ProgressBar loadingProgress;

    private SharedPreferences sharedPreferences;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        // Header views
        backButton = findViewById(R.id.backButton);
        profileAvatar = findViewById(R.id.profileAvatar);
        usernameText = findViewById(R.id.usernameText);
        fullNameText = findViewById(R.id.fullNameText);
        rankText = findViewById(R.id.rankText);
        reputationText = findViewById(R.id.reputationText);

        // Stats views
        totalSolvedText = findViewById(R.id.totalSolvedText);
        easySolvedText = findViewById(R.id.easySolvedText);
        mediumSolvedText = findViewById(R.id.mediumSolvedText);
        hardSolvedText = findViewById(R.id.hardSolvedText);
        acceptanceRateText = findViewById(R.id.acceptanceRateText);
        streakCountText = findViewById(R.id.streakCountText);
        contestRatingText = findViewById(R.id.contestRatingText);

        // Chips
        easyChip = findViewById(R.id.easyChip);
        mediumChip = findViewById(R.id.mediumChip);
        hardChip = findViewById(R.id.hardChip);

        // Cards
        statsCard = findViewById(R.id.statsCard);
        problemsCard = findViewById(R.id.problemsCard);
        contestCard = findViewById(R.id.contestCard);
        badgesCard = findViewById(R.id.badgesCard);

        // Buttons and Progress
        logoutButton = findViewById(R.id.logoutButton);
        skeletonStub = findViewById(R.id.skeletonStub);
        profileContentScroll = findViewById(R.id.profileContentScroll);

        sharedPreferences = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
    }

    private void loadUserData() {
        // Check if guest mode
        boolean isGuestMode = sharedPreferences.getBoolean("is_guest_mode", false);
        
        if (isGuestMode) {
            // Show guest mode message
            username = "Guest";
            usernameText.setText("Guest Mode");
            fullNameText.setText("Login to view your profile");
            profileAvatar.setImageResource(R.drawable.ic_person);
            
            // Hide content and show prompt to login
            showGuestModePrompt();
            return;
        }
        
        // Try to get username from new key first, then fall back to old key
        username = sharedPreferences.getString("leetcode_username", "");
        if (username.isEmpty()) {
            username = sharedPreferences.getString("username", "");
        }
        
        if (username.isEmpty()) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usernameText.setText(username);
        
        // Load profile picture
        String profilePicUrl = "https://ui-avatars.com/api/?name=" + username + "&size=200&background=FFA116&color=fff&bold=true";
        try {
            Glide.with(this)
                .load(profilePicUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(profileAvatar);
        } catch (Exception e) {
            android.util.Log.e("ProfileActivity", "Error loading profile image: " + e.getMessage());
            profileAvatar.setImageResource(R.drawable.ic_person);
        }
        
        fetchLeetCodeData();
    }
    
    private void showGuestModePrompt() {
        showSkeleton(false);
        profileContentScroll.setVisibility(View.VISIBLE);
        
        // Hide stats
        totalSolvedText.setText("—");
        easySolvedText.setText("0");
        mediumSolvedText.setText("0");
        hardSolvedText.setText("0");
        acceptanceRateText.setText("—");
        streakCountText.setText("—");
        contestRatingText.setText("—");
        rankText.setVisibility(View.GONE);
        reputationText.setVisibility(View.GONE);
        
        // Update logout button to "Login"
        logoutButton.setText("Login with LeetCode");
    }

    private void fetchLeetCodeData() {
        showSkeleton(true);
        
        new Thread(() -> {
            try {
                String apiUrl = "https://leetcode-stats-api.herokuapp.com/" + username;
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                
                runOnUiThread(() -> {
                    showSkeleton(false);
                    updateUI(jsonResponse);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showSkeleton(false);
                    Toast.makeText(ProfileActivity.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                    loadLocalData();
                });
            }
        }).start();
    }

    private void updateUI(JSONObject data) {
        try {
            // Total solved
            if (data.has("totalSolved")) {
                int totalSolved = data.getInt("totalSolved");
                totalSolvedText.setText(String.valueOf(totalSolved));
            }

            // Easy, Medium, Hard
            if (data.has("easySolved")) {
                int easy = data.getInt("easySolved");
                easySolvedText.setText(String.valueOf(easy));
                easyChip.setText("Easy: " + easy);
            }
            if (data.has("mediumSolved")) {
                int medium = data.getInt("mediumSolved");
                mediumSolvedText.setText(String.valueOf(medium));
                mediumChip.setText("Medium: " + medium);
            }
            if (data.has("hardSolved")) {
                int hard = data.getInt("hardSolved");
                hardSolvedText.setText(String.valueOf(hard));
                hardChip.setText("Hard: " + hard);
            }

            // Acceptance rate
            if (data.has("acceptanceRate")) {
                double rate = data.getDouble("acceptanceRate");
                acceptanceRateText.setText(String.format("%.1f%%", rate));
            }

            // Ranking
            if (data.has("ranking")) {
                int rank = data.getInt("ranking");
                rankText.setText("Rank: #" + rank);
            }

            // Contest rating
            if (data.has("contributionPoints")) {
                int rating = data.getInt("contributionPoints");
                contestRatingText.setText(String.valueOf(rating));
                reputationText.setText(rating + " points");
            }

            // Streak from local data
            int streak = sharedPreferences.getInt("currentStreak", 0);
            streakCountText.setText(String.valueOf(streak));

        } catch (Exception e) {
            e.printStackTrace();
            loadLocalData();
        }
    }

    private void loadLocalData() {
        // Load from SharedPreferences if API fails
        int totalSolved = sharedPreferences.getInt("totalSolved", 0);
        int easySolved = sharedPreferences.getInt("easySolved", 0);
        int mediumSolved = sharedPreferences.getInt("mediumSolved", 0);
        int hardSolved = sharedPreferences.getInt("hardSolved", 0);
        int streak = sharedPreferences.getInt("currentStreak", 0);

        totalSolvedText.setText(String.valueOf(totalSolved));
        easySolvedText.setText(String.valueOf(easySolved));
        mediumSolvedText.setText(String.valueOf(mediumSolved));
        hardSolvedText.setText(String.valueOf(hardSolved));
        streakCountText.setText(String.valueOf(streak));

        easyChip.setText("Easy: " + easySolved);
        mediumChip.setText("Medium: " + mediumSolved);
        hardChip.setText("Hard: " + hardSolved);

        acceptanceRateText.setText("--");
        contestRatingText.setText("--");
        rankText.setText("Rank: --");
        reputationText.setText("-- points");
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        logoutButton.setOnClickListener(v -> {
            handleLogout();
        });

        problemsCard.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ProblemsActivity.class);
            startActivity(intent);
        });

        contestCard.setOnClickListener(v -> {
            Toast.makeText(this, "Contest details coming soon", Toast.LENGTH_SHORT).show();
        });

        badgesCard.setOnClickListener(v -> {
            Toast.makeText(this, "Badges & achievements coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void handleLogout() {
        // Check if in guest mode
        boolean isGuestMode = sharedPreferences.getBoolean("is_guest_mode", false);
        
        if (isGuestMode) {
            // If guest, navigate to login to allow them to sign in
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        // Normal logout
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh streak data when returning to profile
        int streak = sharedPreferences.getInt("currentStreak", 0);
        streakCountText.setText(String.valueOf(streak));
    }

    private void showSkeleton(boolean show) {
        if (show) {
            // Show skeleton loading
            if (skeletonView == null && skeletonStub != null) {
                try {
                    skeletonView = skeletonStub.inflate();
                    startSkeletonAnimation(skeletonView);
                } catch (Exception e) {
                    android.util.Log.e("ProfileActivity", "Failed to inflate skeleton", e);
                    return;
                }
            }
            
            if (profileContentScroll != null) {
                profileContentScroll.setVisibility(View.INVISIBLE);
            }
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide skeleton and show content
            if (skeletonView != null) {
                skeletonView.setVisibility(View.GONE);
                stopSkeletonAnimation(skeletonView);
            }
            if (profileContentScroll != null) {
                profileContentScroll.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    startSkeletonAnimation(child);
                } else {
                    // Apply shimmer animation to skeleton views
                    AlphaAnimation animation = new AlphaAnimation(0.3f, 1.0f);
                    animation.setDuration(1000);
                    animation.setRepeatMode(Animation.REVERSE);
                    animation.setRepeatCount(Animation.INFINITE);
                    child.startAnimation(animation);
                }
            }
        }
    }

    private void stopSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    stopSkeletonAnimation(child);
                } else {
                    child.clearAnimation();
                }
            }
        }
    }
}
