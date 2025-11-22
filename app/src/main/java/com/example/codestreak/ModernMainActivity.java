package com.example.codestreak;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ModernMainActivity extends BaseActivity {
    
    // UI Components  
    private SwipeRefreshLayout swipeRefreshLayout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout navHome, navProgress, navCards, navRevision;
    private ImageView homeIcon, progressIcon, cardsIcon, revisionIcon;
    private TextView homeText, progressText, cardsText, revisionText;
    private View homeIndicator, progressIndicator, cardsIndicator, revisionIndicator;
    private com.google.android.material.card.MaterialCardView bottomNavCard;
    
    // Skeleton loading
    private ViewStub skeletonStub;
    private View skeletonView;
    private androidx.core.widget.NestedScrollView mainContentContainer;
    private FrameLayout themeToggleContainer;
    private View toggleTrack, toggleThumb;
    private FrameLayout toggleThumbContainer;
    private ImageView toggleIcon, sunIcon, moonIcon;
    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private ImageButton menuButton;
    private TextView welcomeText;
    private TextView currentStreakText, longestStreakText;
    private TextView easyCountTableText, mediumCountTableText, hardCountTableText, totalCountText;
    private TextView goalCountBadge;
    private TextView todaysGoalTitle;
    private RecyclerView dailyGoalsRecyclerView;
    private RecyclerView contributionGrid;
    private PieChart pieChart;
    private com.google.android.material.card.MaterialCardView problemDistributionCard;
    private LinearLayout statsTableContainer;
    private TextView monthYearText;
    private ImageButton prevMonthButton, nextMonthButton;
    
    // Notification controls
    private SwitchCompat notificationSwitch;
    private MaterialButton testNotificationButton;
    private MaterialButton setMorningTimeButton;
    private MaterialButton setEveningTimeButton;
    private TextView notificationTimeInfo;
    private TextView cacheStatusText;
    
    // Data

    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    private int currentStreak = 15;
    private int longestStreak = 28;
    private Calendar currentCalendar;
    private UltraSimpleAdapter contributionAdapter;
    private LeetCodeAPI leetCodeAPI;
    
    // Date tracking for daily goals
    private String lastGoalsDate = "";
    
    // Cache manager for performance optimization
    private CacheManager cacheManager;
    
    // Add submission calendar data and caching like MainActivity
    private org.json.JSONObject submissionCalendarData;
    private java.util.Set<String> monthsWithDataCache = null;
    
    // Developer mode activation (tap welcome text 7 times)
    private int welcomeTextClickCount = 0;
    private long lastWelcomeClickTime = 0;
    private boolean developerModeEnabled = false;
    private static final int DEVELOPER_MODE_CLICKS = 7;
    private static final long CLICK_TIMEOUT = 2000; // 2 seconds
    
    // Popups
    private PopupWindow pieChartPopup;
    private PopupWindow calendarDayPopup;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Check if user is logged in before anything else
            SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
            boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
            boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
            
            // If not guest mode and not logged in, redirect to login
            if (!isGuestMode && !isLoggedIn) {
                String username = LoginActivity.getLeetCodeUsername(this);
                if (username == null || username.isEmpty()) {
                    // User not logged in, redirect to login
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(loginIntent);
                    finish();
                    return;
                }
            }
            
            setContentView(R.layout.activity_main_modern);
            
            // Make status bar transparent to show the actual activity background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
                getWindow().getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
                
                // Set status bar icon colors based on theme (light or dark mode)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    int flags = getWindow().getDecorView().getSystemUiVisibility();
                    // If light theme (isDarkTheme is false), use dark icons
                    if (!isDarkTheme) {
                        getWindow().getDecorView().setSystemUiVisibility(
                            flags | android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        );
                    } else {
                        // If dark theme, use light icons (remove the flag)
                        getWindow().getDecorView().setSystemUiVisibility(
                            flags & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        );
                    }
                }
            }
            
            // Initialize cache manager
            cacheManager = new CacheManager(this);
            
            // Load cached data on startup
            loadCachedDataOnStartup();
            
            // Initialize notification system
            initializeNotificationSystem();
            
            // Initialize views and setup
            initializeViews();
            setupThemeToggle();
            setupBottomNavigation();
            setupPieChart();
            setupCalendar();
            setupDailyGoals();
            setupNotificationControls();
            
            // Check if theme settings should be shown
            if (getIntent().getBooleanExtra("show_theme_settings", false)) {
                // Scroll to or highlight theme toggle
                if (themeToggleContainer != null) {
                    themeToggleContainer.requestFocus();
                    // You could also show a toast or animate the theme toggle
                }
            }
            
            // Load user preferences
            loadUserPreferences();
            
            // Initialize data
            leetCodeAPI = new LeetCodeAPI();
            fetchInitialData();
            
        } catch (Exception e) {
            android.util.Log.e("ModernMainActivity", "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
            // If there's an error, redirect to login
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish();
        }
    }
    
    private void initializeNotificationSystem() {
        // Create notification channel for API 26+
        NotificationHelper.createNotificationChannel(this);
        
        // Schedule daily notifications if not already scheduled
        NotificationScheduler.scheduleNotifications(this);
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, schedule notifications and update UI
                NotificationScheduler.scheduleNotifications(this);
                NotificationScheduler.setNotificationsEnabled(this, true);
                if (notificationSwitch != null) {
                    notificationSwitch.setChecked(true);
                }
                Toast.makeText(this, "Notification permission granted! Daily reminders enabled.", Toast.LENGTH_LONG).show();
            } else {
                // Permission denied, turn off switch
                if (notificationSwitch != null) {
                    notificationSwitch.setChecked(false);
                }
                Toast.makeText(this, "Notification permission required for daily reminders", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        
        // Set navigation view theme colors based on current theme
        if (isDarkTheme) {
            navigationView.setBackgroundColor(getResources().getColor(R.color.modern_surface_dark, null));
        } else {
            navigationView.setBackgroundColor(getResources().getColor(R.color.surface_primary, null));
        }
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Initialize skeleton loading
        skeletonStub = findViewById(R.id.skeletonStub);
        mainContentContainer = findViewById(R.id.nestedScrollView);
        
        // Ensure main content is visible by default
        if (mainContentContainer != null) {
            mainContentContainer.setVisibility(View.VISIBLE);
        }
        
        setupSwipeRefresh();
        
        initializeCustomBottomNavigation();
        bottomNavCard = findViewById(R.id.bottomNavCard);
        
        // Ensure bottom navigation stays fixed at bottom and is visible
        if (bottomNavCard != null) {
            bottomNavCard.setVisibility(View.VISIBLE);
            bottomNavCard.bringToFront();
            bottomNavCard.setTranslationZ(100f); // Ensure it's above other views
            
            // Post a runnable to ensure layout is complete before positioning
            bottomNavCard.post(() -> {
                bottomNavCard.bringToFront();
                bottomNavCard.setTranslationZ(100f);
            });
        }
        
        themeToggleContainer = findViewById(R.id.themeToggleContainer);
        
        // Initialize custom toggle components
        toggleTrack = findViewById(R.id.toggleTrack);
        toggleThumb = findViewById(R.id.toggleThumb);
        toggleThumbContainer = findViewById(R.id.toggleThumbContainer);
        toggleIcon = findViewById(R.id.toggleIcon);
        sunIcon = findViewById(R.id.sunIcon);
        moonIcon = findViewById(R.id.moonIcon);
        
        toolbar = findViewById(R.id.toolbar);
        menuButton = findViewById(R.id.menuButton);
        welcomeText = findViewById(R.id.welcomeText);
        
        // Set up developer mode activation (tap welcome text 7 times)
        setupDeveloperModeActivation();
        
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        easyCountTableText = findViewById(R.id.easyCountTableText);
        mediumCountTableText = findViewById(R.id.mediumCountTableText);
        hardCountTableText = findViewById(R.id.hardCountTableText);
        totalCountText = findViewById(R.id.totalCountText);
        goalCountBadge = findViewById(R.id.goalCountBadge);
        todaysGoalTitle = findViewById(R.id.todaysGoalTitle);
        dailyGoalsRecyclerView = findViewById(R.id.dailyGoalsRecyclerView);
        contributionGrid = findViewById(R.id.contributionGrid);
        pieChart = findViewById(R.id.pieChart);
        problemDistributionCard = findViewById(R.id.problemDistributionCard);
        statsTableContainer = findViewById(R.id.statsTableContainer);
        monthYearText = findViewById(R.id.monthYearText);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        
        // Notification controls
        notificationSwitch = findViewById(R.id.notificationSwitch);
        testNotificationButton = findViewById(R.id.testNotificationButton);
        setMorningTimeButton = findViewById(R.id.setMorningTimeButton);
        setEveningTimeButton = findViewById(R.id.setEveningTimeButton);
        notificationTimeInfo = findViewById(R.id.notificationTimeInfo);
        cacheStatusText = findViewById(R.id.cacheStatusText);
        
        // Setup menu button
        ImageButton menuButton = findViewById(R.id.menuButton);
        FrameLayout menuButtonContainer = (FrameLayout) menuButton.getParent();
        
        // Set click listeners on both button and container for better UX
        View.OnClickListener menuClickListener = v -> drawerLayout.openDrawer(GravityCompat.START);
        menuButton.setOnClickListener(menuClickListener);
        menuButtonContainer.setOnClickListener(menuClickListener);
        
        // Setup navigation drawer item clicks
        setupNavigationDrawer();
        
        // Update navigation header after a slight delay to ensure view is ready
        navigationView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    updateNavigationHeader();
                } catch (Exception e) {
                    android.util.Log.e("NavHeader", "Error updating navigation header: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void updateNavigationHeader() {
        try {
            View headerView = navigationView.getHeaderView(0);
            if (headerView == null) {
                android.util.Log.w("NavHeader", "Header view is null, skipping update");
                return;
            }
            TextView userNameText = headerView.findViewById(R.id.userNameText);
            TextView userEmailText = headerView.findViewById(R.id.userEmailText);
            ImageView profileImage = headerView.findViewById(R.id.profileImage);
            
            SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
            String username = prefs.getString("leetcode_username", null);
            String realName = prefs.getString("user_real_name", null);
            String email = prefs.getString("user_email", null);
            String avatarUrl = prefs.getString("user_avatar", null);
            
            // Debug logging
            android.util.Log.d("NavHeader", "Username: " + username);
            android.util.Log.d("NavHeader", "Real Name: " + realName);
            android.util.Log.d("NavHeader", "Email: " + email);
            android.util.Log.d("NavHeader", "Avatar URL: " + avatarUrl);
            
            // Set username or real name
            if (realName != null && !realName.isEmpty()) {
                userNameText.setText(realName);
            } else if (username != null && !username.isEmpty()) {
                userNameText.setText(username);
            } else {
                userNameText.setText("Guest User");
            }
            
            // Set email or username as subtitle
            if (email != null && !email.isEmpty()) {
                userEmailText.setText(email);
            } else if (username != null && !username.isEmpty()) {
                userEmailText.setText("@" + username);
            } else {
                userEmailText.setText("No email");
            }
            
            // Load profile image
            if (profileImage != null) {
                // Generate avatar URL from username
                String imageUrl;
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    // Use provided avatar URL
                    imageUrl = avatarUrl;
                    if (imageUrl.startsWith("http://")) {
                        imageUrl = imageUrl.replace("http://", "https://");
                    }
                } else if (username != null && !username.isEmpty()) {
                    // Generate UI Avatars URL from username
                    imageUrl = "https://ui-avatars.com/api/?name=" + username + "&size=200&background=FFA116&color=fff&bold=true";
                } else {
                    // Fallback to placeholder
                    profileImage.setImageResource(R.drawable.profile_placeholder);
                    return;
                }
                
                android.util.Log.d("NavHeader", "Loading image from: " + imageUrl);
                
                try {
                    Glide.with(ModernMainActivity.this)
                        .load(imageUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .circleCrop()
                        .into(profileImage);
                } catch (Exception e) {
                    android.util.Log.e("NavHeader", "Error loading image: " + e.getMessage());
                    e.printStackTrace();
                    profileImage.setImageResource(R.drawable.profile_placeholder);
                }
            } else {
                android.util.Log.d("NavHeader", "ImageView is null");
            }
        } catch (Exception e) {
            android.util.Log.e("NavHeader", "Error in updateNavigationHeader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_profile) {
                // Handle profile click
                Intent profileIntent = new Intent(ModernMainActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
            } else if (id == R.id.nav_stats) {
                // Handle stats click
                Intent statsIntent = new Intent(ModernMainActivity.this, StatisticsActivity.class);
                startActivity(statsIntent);
            } else if (id == R.id.nav_achievements) {
                // Handle achievements click
                // TODO: Navigate to achievements activity
            } else if (id == R.id.nav_settings) {
                // Handle settings click
                // TODO: Navigate to settings activity
            } else if (id == R.id.nav_help) {
                // Handle help click
                // TODO: Navigate to help activity
            } else if (id == R.id.nav_about) {
                // Handle about click
                Intent aboutIntent = new Intent(ModernMainActivity.this, AboutActivity.class);
                startActivity(aboutIntent);
            } else if (id == R.id.nav_logout) {
                // Handle logout click
                handleLogout();
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    
    private void handleLogout() {
        // Clear all user data and logout
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
        
        SharedPreferences newPref = getSharedPreferences("CodeStreakPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor newEditor = newPref.edit();
        newEditor.clear();
        newEditor.apply();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_secondary,
            R.color.easy_color,
            R.color.medium_color,
            R.color.hard_color
        );
        
        // Set custom background with shadow for refresh indicator
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.surface_primary);
        
        // Add shadow and elevation effects programmatically
        swipeRefreshLayout.setElevation(8f);
        
        // Set size of the refresh indicator
        swipeRefreshLayout.setSize(androidx.swiperefreshlayout.widget.SwipeRefreshLayout.LARGE);
        
        // Set the distance to trigger refresh
        swipeRefreshLayout.setDistanceToTriggerSync(120);
        
        // Set progress view offset for better visual feedback
        swipeRefreshLayout.setProgressViewOffset(false, 0, 200);
        
        // Add subtle shadow to nested scroll view
        View nestedScrollView = findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            nestedScrollView.setElevation(2f);
        }
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            System.out.println("DEBUG: Pull-to-refresh triggered - refreshing daily goals and clearing caches");
            
            // Clear caches for fresh data
            if (cacheManager != null) {
                cacheManager.clearAllApiCaches();
                System.out.println("DEBUG: Cleared API caches for fresh data");
            }
            
            // Show skeleton loading during refresh
            showRefreshSkeleton(true);
            
            // Add shadow animation during refresh
            animateRefreshShadow(true);
            
            // Update the title with current date
            updateTodaysGoalTitle();
            
            // Clear current goals and show loading message (will be replaced by skeleton)
            List<DailyGoal> loadingGoals = new ArrayList<>();
            loadingGoals.add(new DailyGoal("Refreshing daily goals...", "Please wait", "Easy", false));
            updateGoalsAdapter(loadingGoals);
            
            // Refresh all data
            fetchInitialData();
            fetchDailyGoalsFromAPI();
            
            // Stop refreshing after a delay to show completion
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                    // Hide skeleton and show content
                    showRefreshSkeleton(false);
                    // Remove shadow animation after refresh
                    animateRefreshShadow(false);
                    System.out.println("DEBUG: Pull-to-refresh completed");
                }
            }, 2000); // Reduced to 2 seconds for better UX
        });
    }
    
    /**
     * Animate shadow effect during refresh
     */
    private void animateRefreshShadow(boolean isRefreshing) {
        if (swipeRefreshLayout == null) return;
        
        ValueAnimator shadowAnimator;
        if (isRefreshing) {
            // Increase shadow when refreshing starts
            shadowAnimator = ValueAnimator.ofFloat(swipeRefreshLayout.getElevation(), 16f);
            shadowAnimator.addUpdateListener(animation -> {
                float elevation = (Float) animation.getAnimatedValue();
                swipeRefreshLayout.setElevation(elevation);
            });
        } else {
            // Decrease shadow when refreshing ends
            shadowAnimator = ValueAnimator.ofFloat(swipeRefreshLayout.getElevation(), 8f);
            shadowAnimator.addUpdateListener(animation -> {
                float elevation = (Float) animation.getAnimatedValue();
                swipeRefreshLayout.setElevation(elevation);
            });
        }
        
        shadowAnimator.setDuration(300);
        shadowAnimator.setInterpolator(new DecelerateInterpolator());
        shadowAnimator.start();
    }
    
    private void setupThemeToggle() {
        themeToggleContainer.setOnClickListener(v -> toggleTheme());
        
        // Post a runnable to ensure layout is complete before setting initial position
        themeToggleContainer.post(() -> {
            updateThemeToggleAppearance(false);
        });
    }
    
    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        updateThemeToggleAppearance(true);
        saveThemePreference();
        
        // Update colors immediately for visual feedback
        updateViewColors();
        
        // Apply theme change with recreation
        applyTheme();
    }
    
    private void updateThemeToggleAppearance(boolean animate) {
        // Set track state for proper background color
        toggleTrack.setSelected(isDarkTheme);
        
        // Simple positioning: Light mode = 0dp, Dark mode = 24dp translation
        // This moves the thumb from left edge to right edge within the 56dp container
        float targetX = isDarkTheme ? 60f : 0f;
        
        System.out.println("DEBUG: Toggle positioning - isDarkTheme: " + isDarkTheme + ", targetX: " + targetX + "dp");
        
        if (animate) {
            // Animate thumb sliding
            ValueAnimator thumbAnimator = ValueAnimator.ofFloat(toggleThumbContainer.getTranslationX(), targetX);
            thumbAnimator.setDuration(300);
            thumbAnimator.setInterpolator(new DecelerateInterpolator());
            thumbAnimator.addUpdateListener(animation -> {
                float value = (Float) animation.getAnimatedValue();
                toggleThumbContainer.setTranslationX(value);
                System.out.println("DEBUG: Animation value: " + value + "dp");
            });
            thumbAnimator.start();
            
            // Animate icon change with scale effect
            toggleIcon.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(150)
                .withEndAction(() -> {
                    // Change icon
                    toggleIcon.setImageResource(isDarkTheme ? R.drawable.ic_moon : R.drawable.ic_sun);
                    toggleIcon.setColorFilter(isDarkTheme ? 
                        getColor(R.color.text_secondary) : getColor(R.color.accent_secondary));
                    
                    // Scale back up
                    toggleIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start();
                })
                .start();
                
            // Animate background icons
            if (isDarkTheme) {
                sunIcon.animate().alpha(0.3f).setDuration(200);
                moonIcon.animate().alpha(1.0f).setDuration(200);
            } else {
                sunIcon.animate().alpha(1.0f).setDuration(200);
                moonIcon.animate().alpha(0.3f).setDuration(200);
            }
            
        } else {
            // Set positions immediately without animation
            toggleThumbContainer.setTranslationX(targetX);
            toggleIcon.setImageResource(isDarkTheme ? R.drawable.ic_moon : R.drawable.ic_sun);
            toggleIcon.setColorFilter(isDarkTheme ? 
                getColor(R.color.text_secondary) : getColor(R.color.accent_secondary));
            
            // Set background icons visibility
            sunIcon.setAlpha(isDarkTheme ? 0.3f : 1.0f);
            moonIcon.setAlpha(isDarkTheme ? 1.0f : 0.3f);
            
            System.out.println("DEBUG: Set immediate position to: " + targetX + "dp");
        }
    }
    
    private void applyTheme() {
        // Apply dynamic theme changes to views (same approach as ProblemsActivity)
        updateViewColors();
        
        // Refresh RecyclerView to apply theme to items (same as ProblemsActivity)
        if (dailyGoalsRecyclerView != null && dailyGoalsRecyclerView.getAdapter() != null) {
            dailyGoalsRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
    
    private void updateViewColors() {
        // Update background colors based on theme
        View rootView = findViewById(android.R.id.content);
        
        if (isDarkTheme) {
            // Dark theme colors
            rootView.setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            
            // Update drawer layout background
            drawerLayout.setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            
            // Update all major UI elements with dark backgrounds
            findViewById(R.id.appBarLayout).setBackgroundColor(android.graphics.Color.TRANSPARENT);
            findViewById(R.id.navigationView).setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            
            // Update toolbar background for dark theme - keep transparent
            if (toolbar != null) {
                toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            
            // Update toolbar icon colors for dark theme
            if (menuButton != null) {
                menuButton.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (toggleIcon != null) {
                toggleIcon.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            
            // This is the key fix - update the main CoordinatorLayout background
            View coordinatorLayout = drawerLayout.getChildAt(0); // First child is the CoordinatorLayout
            if (coordinatorLayout != null) {
                coordinatorLayout.setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            }
            
            // Update all MaterialCardView backgrounds to dark
            updateCardBackgrounds(rootView, getResources().getColor(R.color.leetcode_card_bg, getTheme()));
            
            // Fix specific problem distribution card background
            if (problemDistributionCard != null) {
                problemDistributionCard.setCardBackgroundColor(getResources().getColor(R.color.leetcode_card_bg, getTheme()));
            }
            
            // Refresh RecyclerView to apply dark theme to list items
            if (dailyGoalsRecyclerView != null && dailyGoalsRecyclerView.getAdapter() != null) {
                dailyGoalsRecyclerView.getAdapter().notifyDataSetChanged();
            }
            
            // Update section title text colors for dark theme
            updateSectionTitleColors(rootView, getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            
            // Update text colors that exist
            if (findViewById(R.id.currentStreakText) != null) {
                ((TextView) findViewById(R.id.currentStreakText)).setTextColor(getResources().getColor(R.color.leetcode_text_light, getTheme()));
            }
            if (findViewById(R.id.longestStreakText) != null) {
                ((TextView) findViewById(R.id.longestStreakText)).setTextColor(getResources().getColor(R.color.leetcode_text_light, getTheme()));
            }
            if (findViewById(R.id.welcomeText) != null) {
                ((TextView) findViewById(R.id.welcomeText)).setTextColor(getResources().getColor(R.color.leetcode_text_secondary, getTheme()));
            }
            
            // Fix calendar text colors
            if (monthYearText != null) {
                monthYearText.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            
            // Fix calendar button icon colors
            if (prevMonthButton != null) {
                prevMonthButton.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (nextMonthButton != null) {
                nextMonthButton.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            
            // Fix pie chart center text color for dark theme
            if (pieChart != null) {
                pieChart.setCenterTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            
            // Fix stats table text colors for dark theme
            if (easyCountTableText != null) {
                easyCountTableText.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (mediumCountTableText != null) {
                mediumCountTableText.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (hardCountTableText != null) {
                hardCountTableText.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (totalCountText != null) {
                totalCountText.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            
            // Fix stats table background for dark theme
            if (statsTableContainer != null) {
                statsTableContainer.setBackground(getResources().getDrawable(R.drawable.stats_table_background_dark, getTheme()));
            }
            
        } else {
            // Light theme colors - match ProblemsActivity exactly
            rootView.setBackgroundColor(getResources().getColor(R.color.background_primary, getTheme()));
            
            // Update drawer layout background
            drawerLayout.setBackgroundColor(getResources().getColor(R.color.background_primary, getTheme()));
            
            // Update all major UI elements with light backgrounds
            findViewById(R.id.appBarLayout).setBackgroundColor(android.graphics.Color.TRANSPARENT);
            findViewById(R.id.navigationView).setBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            
            // Update toolbar background for light theme - keep transparent
            if (toolbar != null) {
                toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
            
            // Update toolbar icon colors for light theme
            if (menuButton != null) {
                menuButton.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (toggleIcon != null) {
                toggleIcon.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // This is the key fix - update the main CoordinatorLayout background
            View coordinatorLayout = drawerLayout.getChildAt(0); // First child is the CoordinatorLayout
            if (coordinatorLayout != null) {
                coordinatorLayout.setBackgroundColor(getResources().getColor(R.color.background_primary, getTheme()));
            }
            
            // Update all MaterialCardView backgrounds to light
            updateCardBackgrounds(rootView, getResources().getColor(R.color.surface_primary, getTheme()));
            
            // Fix specific problem distribution card background
            if (problemDistributionCard != null) {
                problemDistributionCard.setCardBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            }
            
            // Refresh RecyclerView to apply light theme to list items
            if (dailyGoalsRecyclerView != null && dailyGoalsRecyclerView.getAdapter() != null) {
                dailyGoalsRecyclerView.getAdapter().notifyDataSetChanged();
            }
            
            // Update section title text colors for light theme
            updateSectionTitleColors(rootView, getResources().getColor(R.color.text_primary, getTheme()));
            
            // Update text colors that exist
            if (findViewById(R.id.currentStreakText) != null) {
                ((TextView) findViewById(R.id.currentStreakText)).setTextColor(getResources().getColor(R.color.leetcode_text_dark, getTheme()));
            }
            if (findViewById(R.id.longestStreakText) != null) {
                ((TextView) findViewById(R.id.longestStreakText)).setTextColor(getResources().getColor(R.color.leetcode_text_dark, getTheme()));
            }
            if (findViewById(R.id.welcomeText) != null) {
                ((TextView) findViewById(R.id.welcomeText)).setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            }
            
            // Fix calendar text colors
            if (monthYearText != null) {
                monthYearText.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // Fix calendar button icon colors
            if (prevMonthButton != null) {
                prevMonthButton.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (nextMonthButton != null) {
                nextMonthButton.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // Fix pie chart center text color for light theme
            if (pieChart != null) {
                pieChart.setCenterTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // Fix stats table text colors for light theme
            if (easyCountTableText != null) {
                easyCountTableText.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (mediumCountTableText != null) {
                mediumCountTableText.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (hardCountTableText != null) {
                hardCountTableText.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (totalCountText != null) {
                totalCountText.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // Fix stats table background for light theme
            if (statsTableContainer != null) {
                statsTableContainer.setBackground(getResources().getDrawable(R.drawable.stats_table_background, getTheme()));
            }
        }
    }
    
    private void updateCardBackgrounds(View parent, int color) {
        if (parent instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) parent).setCardBackgroundColor(color);
        }
        
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateCardBackgrounds(group.getChildAt(i), color);
            }
        }
    }
    
    private void updateSectionTitleColors(View parent, int color) {
        if (parent instanceof TextView) {
            TextView textView = (TextView) parent;
            String text = textView.getText().toString();
            // Update specific section titles and the "Total" label (but not Easy/Medium/Hard which should keep their difficulty colors)
            if ("Today's Goal".equals(text) || "Problem Distribution".equals(text) || "Total Problems".equals(text) ||
                "Total".equals(text)) {
                textView.setTextColor(color);
            }
        }
        
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                updateSectionTitleColors(group.getChildAt(i), color);
            }
        }
    }
    
    private void initializeCustomBottomNavigation() {
        // Find navigation items
        navHome = findViewById(R.id.nav_home);
        navProgress = findViewById(R.id.nav_progress);
        navCards = findViewById(R.id.nav_cards);
        navRevision = findViewById(R.id.nav_revision);
        
        // Find icons
        homeIcon = findViewById(R.id.home_icon);
        progressIcon = findViewById(R.id.progress_icon);
        cardsIcon = findViewById(R.id.cards_icon);
        revisionIcon = findViewById(R.id.revision_icon);
        
        // Find text labels
        homeText = findViewById(R.id.home_text);
        progressText = findViewById(R.id.progress_text);
        cardsText = findViewById(R.id.cards_text);
        revisionText = findViewById(R.id.revision_text);
        
        // Find indicators
        homeIndicator = findViewById(R.id.home_indicator);
        progressIndicator = findViewById(R.id.progress_indicator);
        cardsIndicator = findViewById(R.id.cards_indicator);
        revisionIndicator = findViewById(R.id.revision_indicator);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> selectNavItem(0));
        navProgress.setOnClickListener(v -> {
            selectNavItem(1);
            Intent intent = new Intent(ModernMainActivity.this, ProblemsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navCards.setOnClickListener(v -> {
            Intent intent = new Intent(ModernMainActivity.this, CompanyProblemsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        navRevision.setOnClickListener(v -> {
            Intent intent = new Intent(ModernMainActivity.this, RevisionActivity.class);
            startActivity(intent);
        });
        
        // Set home as default selected
        selectNavItem(0);
    }
    
    private void selectNavItem(int index) {
        // Reset all items to inactive state
        resetAllNavItems();
        
        int activeColor = getColor(R.color.accent_secondary);
        int inactiveColor = getColor(R.color.text_secondary);
        
        switch (index) {
            case 0: // Home
                homeIndicator.setVisibility(View.VISIBLE);
                homeIcon.setColorFilter(activeColor);
                homeText.setTextColor(activeColor);
                homeText.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 1: // Progress
                progressIndicator.setVisibility(View.VISIBLE);
                progressIcon.setColorFilter(activeColor);
                progressText.setTextColor(activeColor);
                progressText.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 2: // Companies
                cardsIndicator.setVisibility(View.VISIBLE);
                cardsIcon.setColorFilter(activeColor);
                cardsText.setTextColor(activeColor);
                cardsText.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 3: // Revision
                revisionIndicator.setVisibility(View.VISIBLE);
                revisionIcon.setColorFilter(activeColor);
                revisionText.setTextColor(activeColor);
                revisionText.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }
    
    private void resetAllNavItems() {
        int inactiveColor = getColor(R.color.text_secondary);
        
        // Hide all indicators
        homeIndicator.setVisibility(View.INVISIBLE);
        progressIndicator.setVisibility(View.INVISIBLE);
        cardsIndicator.setVisibility(View.INVISIBLE);
        revisionIndicator.setVisibility(View.INVISIBLE);
        
        // Set all icons to inactive color
        homeIcon.setColorFilter(inactiveColor);
        progressIcon.setColorFilter(inactiveColor);
        cardsIcon.setColorFilter(inactiveColor);
        revisionIcon.setColorFilter(inactiveColor);
        
        // Set all text to inactive color and normal weight
        homeText.setTextColor(inactiveColor);
        progressText.setTextColor(inactiveColor);
        cardsText.setTextColor(inactiveColor);
        revisionText.setTextColor(inactiveColor);
        
        homeText.setTypeface(null, android.graphics.Typeface.NORMAL);
        progressText.setTypeface(null, android.graphics.Typeface.NORMAL);
        cardsText.setTypeface(null, android.graphics.Typeface.NORMAL);
        revisionText.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
    
    private void setupPieChart() {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(false);
        
        pieChart.setTouchEnabled(true);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        
        // Setup click listener
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pieEntry = (PieEntry) e;
                    int totalProblems = easyProblems + mediumProblems + hardProblems;
                    float percentage = (pieEntry.getValue() / totalProblems) * 100;
                    showPieChartPopup(pieEntry, percentage, h);
                }
            }
            
            @Override
            public void onNothingSelected() {
                // Handle deselection
            }
        });
        
        updatePieChart();
    }
    
    private void updatePieChart() {
        // Check if guest mode
        SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        
        if (isGuestMode) {
            // Show empty/placeholder data for guests
            entries.add(new PieEntry(1, "Easy"));
            entries.add(new PieEntry(1, "Medium"));
            entries.add(new PieEntry(1, "Hard"));
        } else {
            // Show real data for logged in users
            entries.add(new PieEntry(easyProblems, "Easy"));
            entries.add(new PieEntry(mediumProblems, "Medium"));
            entries.add(new PieEntry(hardProblems, "Hard"));
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "Problems");
        
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.easy_color, getTheme()));
        colors.add(getResources().getColor(R.color.medium_color, getTheme()));
        colors.add(getResources().getColor(R.color.hard_color, getTheme()));
        dataSet.setColors(colors);
        
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (isGuestMode) {
                    return "0";
                }
                return String.valueOf((int) value);
            }
        });
        
        dataSet.setSelectionShift(5f);
        dataSet.setHighlightEnabled(true);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        
        int totalProblems = isGuestMode ? 0 : (easyProblems + mediumProblems + hardProblems);
        pieChart.setCenterText("Total\n" + totalProblems);
        
        // Update center text color based on current theme
        if (isDarkTheme) {
            pieChart.setCenterTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
        } else {
            pieChart.setCenterTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        }
        
        pieChart.animateXY(1200, 1200);
        pieChart.invalidate();
    }
    
    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        updateCalendarView();
        
        prevMonthButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });
        
        nextMonthButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });
        
        updateContributionGrid();
    }
    
    private void updateCalendarView() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year = currentCalendar.get(Calendar.YEAR);
        
        // Dynamically check if current month has real data
        boolean hasRealData = hasSubmissionsForMonth(month, year);
        
        String monthYear = months[month] + " " + year;
        if (hasRealData) {
            monthYear += " ★"; // Add star to indicate real data
        }
        
        monthYearText.setText(monthYear);
        
        // Update the contribution grid to reflect the new month and highlight current day
        updateContributionGrid();
    }
    
    private void updateContributionGrid() {
        List<Integer> monthData = generateMonthlyContributionData(currentCalendar);
        
        if (contributionAdapter == null) {
            ArrayList<Integer> arrayListData = new ArrayList<>(monthData);
            contributionAdapter = new UltraSimpleAdapter(arrayListData, currentCalendar);
            contributionAdapter.setOnDayClickListener((dayNumber, problemCount, clickedView) -> 
                showCalendarDayPopup(dayNumber, problemCount, clickedView));
            contributionGrid.setLayoutManager(new GridLayoutManager(this, 7));
            contributionGrid.setAdapter(contributionAdapter);
        } else {
            contributionAdapter.updateData(monthData);
        }
    }
    
    private void setupDailyGoals() {
        dailyGoalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Check if we need to refresh goals due to date change
        checkAndRefreshDailyGoals();
    }
    
    /**
     * Check if the date has changed since last goals generation and refresh if needed
     */
    private void checkAndRefreshDailyGoals() {
        String currentDate = getCurrentDateString();
        
        // Load the last date from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("daily_goals", Context.MODE_PRIVATE);
        lastGoalsDate = prefs.getString("last_goals_date", "");
        
        System.out.println("DEBUG: Current date: " + currentDate + ", Last goals date: " + lastGoalsDate);
        
        // Always update the title with current date first
        updateTodaysGoalTitle();
        
        if (!currentDate.equals(lastGoalsDate)) {
            System.out.println("DEBUG: Date changed - refreshing daily goals");
            
            // Save the new date
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_goals_date", currentDate);
            editor.apply();
            lastGoalsDate = currentDate;
            
            // Fetch fresh daily goals
            fetchDailyGoalsFromAPI();
        } else {
            System.out.println("DEBUG: Same date - checking if goals exist");
            
            // Same date, but check if we have goals cached
            List<DailyGoal> cachedGoals = loadCachedGoals();
            if (cachedGoals.isEmpty()) {
                System.out.println("DEBUG: No cached goals found - fetching fresh goals");
                fetchDailyGoalsFromAPI();
            } else {
                System.out.println("DEBUG: Using cached goals for today");
                updateGoalsAdapter(cachedGoals);
            }
        }
    }
    
    /**
     * Get current date as a string in YYYY-MM-DD format
     */
    private String getCurrentDateString() {
        Calendar today = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", 
            today.get(Calendar.YEAR), 
            today.get(Calendar.MONTH) + 1,  // Month is 0-based
            today.get(Calendar.DAY_OF_MONTH));
    }
    
    /**
     * Get formatted date for display (e.g., "Aug 28, 2025")
     */
    private String getFormattedDateString() {
        Calendar today = Calendar.getInstance();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                          "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        return months[today.get(Calendar.MONTH)] + " " + 
               today.get(Calendar.DAY_OF_MONTH) + ", " + 
               today.get(Calendar.YEAR);
    }
    
    /**
     * Get ordinal date format for daily goals (e.g., "28th August")
     */
    private String getOrdinalDateString() {
        Calendar today = Calendar.getInstance();
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        
        int day = today.get(Calendar.DAY_OF_MONTH);
        String ordinalSuffix;
        
        if (day >= 11 && day <= 13) {
            ordinalSuffix = "th";
        } else {
            switch (day % 10) {
                case 1: ordinalSuffix = "st"; break;
                case 2: ordinalSuffix = "nd"; break;
                case 3: ordinalSuffix = "rd"; break;
                default: ordinalSuffix = "th"; break;
            }
        }
        
        return day + ordinalSuffix + " " + months[today.get(Calendar.MONTH)];
    }
    
    /**
     * Update the Today's Goal title
     */
    private void updateTodaysGoalTitle() {
        if (todaysGoalTitle != null) {
            todaysGoalTitle.setText("Today's Goal");
            System.out.println("DEBUG: Updated Today's Goal title");
        }
    }
    
    /**
     * Load cached goals from SharedPreferences
     */
    private List<DailyGoal> loadCachedGoals() {
        SharedPreferences prefs = getSharedPreferences("daily_goals", Context.MODE_PRIVATE);
        String cachedGoalsJson = prefs.getString("cached_goals", "");
        
        List<DailyGoal> goals = new ArrayList<>();
        
        if (!cachedGoalsJson.isEmpty()) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(cachedGoalsJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject goalJson = jsonArray.getJSONObject(i);
                    goals.add(new DailyGoal(
                        goalJson.getString("title"),
                        goalJson.getString("category"),
                        goalJson.getString("difficulty"),
                        goalJson.getBoolean("completed")
                    ));
                }
                System.out.println("DEBUG: Loaded " + goals.size() + " cached goals");
            } catch (Exception e) {
                System.err.println("DEBUG: Error loading cached goals: " + e.getMessage());
            }
        }
        
        return goals;
    }
    
    /**
     * Save goals to cache
     */
    private void saveCachedGoals(List<DailyGoal> goals) {
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (DailyGoal goal : goals) {
                org.json.JSONObject goalJson = new org.json.JSONObject();
                goalJson.put("title", goal.getTitle());
                goalJson.put("category", goal.getCategory());
                goalJson.put("difficulty", goal.getDifficulty());
                goalJson.put("completed", goal.isCompleted());
                jsonArray.put(goalJson);
            }
            
            SharedPreferences prefs = getSharedPreferences("daily_goals", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("cached_goals", jsonArray.toString());
            editor.apply();
            
            System.out.println("DEBUG: Cached " + goals.size() + " goals for today");
        } catch (Exception e) {
            System.err.println("DEBUG: Error caching goals: " + e.getMessage());
        }
    }
    
    private void fetchDailyGoalsFromAPI() {
        System.out.println("DEBUG: Starting API fetch for daily goals");
        
        // Check cache first
        List<?> cachedGoalsRaw = cacheManager.getCachedDailyGoals();
        if (cachedGoalsRaw != null && !cachedGoalsRaw.isEmpty()) {
            try {
                // Cast the cached data back to DailyGoal list
                @SuppressWarnings("unchecked")
                List<DailyGoal> cachedGoals = (List<DailyGoal>) cachedGoalsRaw;
                System.out.println("DEBUG: Using cached daily goals");
                updateGoalsAdapter(cachedGoals);
                updateCacheStatus();
                return;
            } catch (ClassCastException e) {
                System.out.println("DEBUG: Cache data type mismatch, fetching fresh data");
                cacheManager.clearCache("daily_goals");
            }
        }
        
        // Show loading state
        runOnUiThread(() -> {
            List<DailyGoal> loadingGoals = new ArrayList<>();
            loadingGoals.add(new DailyGoal("Loading daily goals...", "Please wait", "Easy", false));
            updateGoalsAdapter(loadingGoals);
        });
        
        LeetCodeAPI api = new LeetCodeAPI();
        
        // Try to get daily challenge first
        api.getDailyCodingChallenge(new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                System.out.println("DEBUG: Daily challenge API success");
                runOnUiThread(() -> {
                    try {
                        List<DailyGoal> apiGoals = new ArrayList<>();
                        
                        // Parse daily challenge
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                        System.out.println("DEBUG: API Response: " + response.substring(0, Math.min(200, response.length())) + "...");
                        
                        if (jsonResponse.has("data")) {
                            org.json.JSONObject data = jsonResponse.getJSONObject("data");
                            if (data.has("activeDailyCodingChallengeQuestion") && 
                                !data.isNull("activeDailyCodingChallengeQuestion")) {
                                
                                org.json.JSONObject dailyChallenge = data.getJSONObject("activeDailyCodingChallengeQuestion");
                                org.json.JSONObject question = dailyChallenge.getJSONObject("question");
                                
                                String title = question.getString("title");
                                String difficulty = question.getString("difficulty");
                                String category = "Daily Challenge";
                                
                                // Get the primary topic tag as category if available
                                if (question.has("topicTags") && question.getJSONArray("topicTags").length() > 0) {
                                    category = question.getJSONArray("topicTags").getJSONObject(0).getString("name");
                                }
                                
                                apiGoals.add(new DailyGoal(title, category, difficulty, false));
                                System.out.println("DEBUG: Added daily challenge - " + title + " (" + difficulty + ")");
                                
                                // Update UI with daily challenge immediately
                                updateGoalsAdapter(new ArrayList<>(apiGoals));
                            } else {
                                System.out.println("DEBUG: No active daily challenge found");
                            }
                        } else {
                            System.out.println("DEBUG: No data field in response");
                        }
                        
                        // Add practice problems for today
                        addTodaysPracticeProblems(apiGoals);
                        
                    } catch (Exception e) {
                        System.err.println("DEBUG: Error parsing daily challenge: " + e.getMessage());
                        e.printStackTrace();
                        // Fall back to sample goals on error
                        loadFallbackGoals();
                    }
                });
            }
            
            @Override
            public void onError(Exception error) {
                System.err.println("DEBUG: Error fetching daily challenge: " + error.getMessage());
                error.printStackTrace();
                // Fall back to sample goals on API error
                runOnUiThread(() -> loadFallbackGoals());
            }
        });
    }
    
    private void addTodaysPracticeProblems(List<DailyGoal> existingGoals) {
        System.out.println("DEBUG: Adding today's practice problems");
        
        LeetCodeAPI api = new LeetCodeAPI();
        
        // Fetch an easy problem for warm-up
        api.getRandomProblem("Easy", new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                        if (jsonResponse.has("data")) {
                            org.json.JSONObject data = jsonResponse.getJSONObject("data");
                            if (data.has("problemsetQuestionList")) {
                                org.json.JSONObject problemList = data.getJSONObject("problemsetQuestionList");
                                if (problemList.has("questions")) {
                                    org.json.JSONArray questions = problemList.getJSONArray("questions");
                                    if (questions.length() > 0) {
                                        org.json.JSONObject question = questions.getJSONObject(0);
                                        
                                        String title = question.getString("title");
                                        String difficulty = question.getString("difficulty");
                                        String category = "Practice";
                                        
                                        if (question.has("topicTags") && question.getJSONArray("topicTags").length() > 0) {
                                            category = question.getJSONArray("topicTags").getJSONObject(0).getString("name");
                                        }
                                        
                                        existingGoals.add(new DailyGoal(title + " (Warm-up)", category, difficulty, false));
                                        System.out.println("DEBUG: Added practice problem - " + title);
                                        
                                        // Add a medium problem too
                                        addMediumPracticeProblems(existingGoals);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("DEBUG: Error parsing easy practice problem: " + e.getMessage());
                        // Add medium problems anyway
                        addMediumPracticeProblems(existingGoals);
                    }
                });
            }
            
            @Override
            public void onError(Exception error) {
                System.err.println("DEBUG: Error fetching easy practice problem: " + error.getMessage());
                runOnUiThread(() -> addMediumPracticeProblems(existingGoals));
            }
        });
    }
    
    private void addMediumPracticeProblems(List<DailyGoal> existingGoals) {
        System.out.println("DEBUG: Adding medium practice problems");
        
        LeetCodeAPI api = new LeetCodeAPI();
        
        // Fetch a medium problem for challenge
        api.getRandomProblem("Medium", new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                        if (jsonResponse.has("data")) {
                            org.json.JSONObject data = jsonResponse.getJSONObject("data");
                            if (data.has("problemsetQuestionList")) {
                                org.json.JSONObject problemList = data.getJSONObject("problemsetQuestionList");
                                if (problemList.has("questions")) {
                                    org.json.JSONArray questions = problemList.getJSONArray("questions");
                                    if (questions.length() > 0) {
                                        org.json.JSONObject question = questions.getJSONObject(0);
                                        
                                        String title = question.getString("title");
                                        String difficulty = question.getString("difficulty");
                                        String category = "Challenge";
                                        
                                        if (question.has("topicTags") && question.getJSONArray("topicTags").length() > 0) {
                                            category = question.getJSONArray("topicTags").getJSONObject(0).getString("name");
                                        }
                                        
                                        existingGoals.add(new DailyGoal(title + " (Challenge)", category, difficulty, false));
                                        System.out.println("DEBUG: Added medium challenge - " + title);
                                        
                                        // Update UI with all goals
                                        updateGoalsAdapter(new ArrayList<>(existingGoals));
                                    }
                                }
                            }
                        }
                        
                        // If no medium problem found, at least update UI with existing goals
                        if (existingGoals.size() > 0) {
                            updateGoalsAdapter(new ArrayList<>(existingGoals));
                        }
                        
                    } catch (Exception e) {
                        System.err.println("DEBUG: Error parsing medium practice problem: " + e.getMessage());
                        // Update UI with existing goals anyway
                        if (existingGoals.size() > 0) {
                            updateGoalsAdapter(new ArrayList<>(existingGoals));
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception error) {
                System.err.println("DEBUG: Error fetching medium practice problem: " + error.getMessage());
                runOnUiThread(() -> {
                    // Update UI with existing goals anyway
                    if (existingGoals.size() > 0) {
                        updateGoalsAdapter(new ArrayList<>(existingGoals));
                    } else {
                        // If no goals at all, load fallback
                        loadFallbackGoals();
                    }
                });
            }
        });
    }
    
    private void updateGoalsAdapter(List<DailyGoal> goals) {
        System.out.println("DEBUG: updateGoalsAdapter called with " + goals.size() + " goals");
        DailyGoalsAdapter adapter = new DailyGoalsAdapter(goals);
        dailyGoalsRecyclerView.setAdapter(adapter);
        goalCountBadge.setText(String.valueOf(goals.size()));
        
        // Cache the goals for today (but not if it's just a loading message)
        if (!goals.isEmpty() && !goals.get(0).getTitle().contains("Loading") && !goals.get(0).getTitle().contains("Refreshing")) {
            saveCachedGoals(goals);
            // Also cache using CacheManager for improved performance
            cacheManager.cacheDailyGoals(goals);
            
            // Update cache status
            updateCacheStatus();
        }
        
        System.out.println("DEBUG: Adapter updated and badge set to " + goals.size());
    }
    
    private void loadFallbackGoals() {
        System.out.println("DEBUG: Loading fallback goals (API unavailable)");
        
        // Generate date-specific goals for today
        java.util.Calendar today = java.util.Calendar.getInstance();
        int dayOfWeek = today.get(java.util.Calendar.DAY_OF_WEEK);
        
        List<DailyGoal> goals = new ArrayList<>();
        
        // Add goals based on day of week for variety
        switch (dayOfWeek) {
            case java.util.Calendar.SUNDAY:
                goals.add(new DailyGoal("Two Sum", "Array", "Easy", false));
                goals.add(new DailyGoal("Valid Parentheses", "Stack", "Easy", false));
                goals.add(new DailyGoal("Add Two Numbers", "Linked List", "Medium", false));
                break;
            case java.util.Calendar.MONDAY:
                goals.add(new DailyGoal("Palindrome Number", "Math", "Easy", false));
                goals.add(new DailyGoal("Maximum Subarray", "Array", "Medium", false));
                goals.add(new DailyGoal("Merge Two Sorted Lists", "Linked List", "Easy", false));
                break;
            case java.util.Calendar.TUESDAY:
                goals.add(new DailyGoal("Roman to Integer", "String", "Easy", false));
                goals.add(new DailyGoal("3Sum", "Array", "Medium", false));
                goals.add(new DailyGoal("Remove Duplicates from Sorted Array", "Array", "Easy", false));
                break;
            case java.util.Calendar.WEDNESDAY:
                goals.add(new DailyGoal("Longest Common Prefix", "String", "Easy", false));
                goals.add(new DailyGoal("Container With Most Water", "Two Pointers", "Medium", false));
                goals.add(new DailyGoal("Search Insert Position", "Binary Search", "Easy", false));
                break;
            case java.util.Calendar.THURSDAY:
                goals.add(new DailyGoal("Length of Last Word", "String", "Easy", false));
                goals.add(new DailyGoal("Letter Combinations of Phone Number", "Backtracking", "Medium", false));
                goals.add(new DailyGoal("Plus One", "Array", "Easy", false));
                break;
            case java.util.Calendar.FRIDAY:
                goals.add(new DailyGoal("Climbing Stairs", "Dynamic Programming", "Easy", false));
                goals.add(new DailyGoal("Generate Parentheses", "Backtracking", "Medium", false));
                goals.add(new DailyGoal("Remove Element", "Array", "Easy", false));
                break;
            case java.util.Calendar.SATURDAY:
                goals.add(new DailyGoal("Merge Sorted Array", "Array", "Easy", false));
                goals.add(new DailyGoal("Longest Substring Without Repeating Characters", "String", "Medium", false));
                goals.add(new DailyGoal("Binary Tree Inorder Traversal", "Tree", "Easy", false));
                break;
        }
        
        // Add a weekend bonus challenge
        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            goals.add(new DailyGoal("Weekend Challenge: Median of Two Sorted Arrays", "Array", "Hard", false));
        }
        
        DailyGoalsAdapter adapter = new DailyGoalsAdapter(goals);
        dailyGoalsRecyclerView.setAdapter(adapter);
        goalCountBadge.setText(String.valueOf(goals.size()));
        
        // Cache these fallback goals for today
        saveCachedGoals(goals);
        
        System.out.println("DEBUG: Loaded " + goals.size() + " fallback goals for " + 
                         (dayOfWeek == java.util.Calendar.SUNDAY ? "Sunday" :
                          dayOfWeek == java.util.Calendar.MONDAY ? "Monday" :
                          dayOfWeek == java.util.Calendar.TUESDAY ? "Tuesday" :
                          dayOfWeek == java.util.Calendar.WEDNESDAY ? "Wednesday" :
                          dayOfWeek == java.util.Calendar.THURSDAY ? "Thursday" :
                          dayOfWeek == java.util.Calendar.FRIDAY ? "Friday" : "Saturday"));
    }
    
    private List<DailyGoal> generateSampleGoals() {
        List<DailyGoal> goals = new ArrayList<>();
        
        // Today's essentials - always good practice
        goals.add(new DailyGoal("Two Sum", "Array", "Easy", false));
        goals.add(new DailyGoal("Valid Parentheses", "Stack", "Easy", false));
        goals.add(new DailyGoal("Maximum Subarray", "Array", "Medium", false));
        goals.add(new DailyGoal("Merge Two Sorted Lists", "Linked List", "Easy", false));
        
        return goals;
    }
    
    /**
     * Public method to force refresh daily goals
     * Can be called when user wants fresh goals
     */
    public void refreshDailyGoals() {
        System.out.println("DEBUG: Force refreshing daily goals");
        
        // Update the title with current date
        updateTodaysGoalTitle();
        
        // Clear cached date to force refresh
        String currentDate = getCurrentDateString();
        SharedPreferences prefs = getSharedPreferences("daily_goals", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_goals_date", currentDate);
        editor.remove("cached_goals"); // Clear cached goals to force fresh fetch
        editor.apply();
        lastGoalsDate = currentDate;
        
        fetchDailyGoalsFromAPI();
    }
    
    private List<Integer> generateMonthlyContributionData(Calendar monthCalendar) {
        List<Integer> data = new ArrayList<>();
        
        Calendar cal = (Calendar) monthCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        
        // Add empty days at the beginning
        for (int i = 0; i < firstDayOfWeek; i++) {
            data.add(-1);
        }
        
        // Add days of the month with submission data
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            int submissions = getSubmissionsForDay(cal, day);
            data.add(submissions);
        }
        
        // Fill remaining cells to complete the grid
        int totalCells = 42; // 6 rows × 7 columns
        while (data.size() < totalCells) {
            data.add(-1);
        }
        
        return data;
    }
    
    private int getSubmissionsForDay(Calendar dayCalendar, int day) {
        // Use real data if available
        if (submissionCalendarData != null) {
            try {
                // Create timestamp for the day
                Calendar localCal = (Calendar) dayCalendar.clone();
                localCal.set(Calendar.DAY_OF_MONTH, day);
                localCal.set(Calendar.HOUR_OF_DAY, 0);
                localCal.set(Calendar.MINUTE, 0);
                localCal.set(Calendar.SECOND, 0);
                localCal.set(Calendar.MILLISECOND, 0);
                
                long localTimestamp = localCal.getTimeInMillis() / 1000;
                String localKey = String.valueOf(localTimestamp);
                
                // Try multiple timezone variations
                String[] possibleKeys = {
                    localKey,
                    String.valueOf(localTimestamp + 19800),   // +5:30 hours (IST timezone)
                    String.valueOf(localTimestamp - 19800),   // -5:30 hours
                    String.valueOf(localTimestamp + 86400),   // +1 day
                    String.valueOf(localTimestamp - 86400),   // -1 day
                };
                
                for (String key : possibleKeys) {
                    if (submissionCalendarData.has(key)) {
                        int submissions = submissionCalendarData.getInt(key);
                        if (submissions > 0) {
                            return submissions;
                        }
                    }
                }
                
            } catch (Exception e) {
                // Error getting submissions for day
            }
        }
        
        return 0;
    }
    
    /**
     * Dynamically check if the given month has any submission data
     * Uses caching to improve performance and reduce iterations
     */
    private boolean hasSubmissionsForMonth(int month, int year) {
        if (submissionCalendarData == null) {
            return false;
        }
        
        // Use cached data if available
        if (monthsWithDataCache == null) {
            buildMonthsWithDataCache();
        }
        
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};
        String monthKey = monthNames[month] + " " + year;
        
        return monthsWithDataCache.contains(monthKey);
    }
    
    /**
     * Build cache of months that have submission data
     * This reduces the number of iterations through submission data
     */
    private void buildMonthsWithDataCache() {
        monthsWithDataCache = new java.util.HashSet<>();
        
        if (submissionCalendarData == null) {
            return;
        }
        
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};
        
        try {
            java.util.Iterator<String> keys = submissionCalendarData.keys();
            while (keys.hasNext()) {
                String timestampStr = keys.next();
                long timestamp = Long.parseLong(timestampStr);
                int submissions = submissionCalendarData.getInt(timestampStr);
                
                if (submissions > 0) {
                    java.util.Date date = new java.util.Date(timestamp * 1000);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int year = cal.get(Calendar.YEAR);
                    int month = cal.get(Calendar.MONTH);
                    
                    String monthKey = monthNames[month] + " " + year;
                    monthsWithDataCache.add(monthKey);
                }
            }
        } catch (Exception e) {
            // Error building cache, use empty set
            monthsWithDataCache = new java.util.HashSet<>();
        }
    }
    
    // Popup methods (similar to existing implementation)
    private void showPieChartPopup(PieEntry pieEntry, float percentage, Highlight highlight) {
        // Hide existing popups
        hideCalendarDayPopup();
        hidePieChartPopup();
        
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.pie_chart_popup, null);
        
        // Get references to popup views
        TextView titleText = popupView.findViewById(R.id.popup_title);
        TextView countText = popupView.findViewById(R.id.popup_count);
        TextView percentageText = popupView.findViewById(R.id.popup_percentage);
        
        // Set the data
        String label = pieEntry.getLabel();
        int count = (int) pieEntry.getValue();
        
        titleText.setText(label + " Problems");
        countText.setText(count + " problems");
        percentageText.setText(String.format("%.1f%%", percentage));
        
        // Set title color based on difficulty
        int titleColor = getResources().getColor(R.color.leetcode_text_primary, getTheme());
        if (label.equals("Easy")) {
            titleColor = getResources().getColor(R.color.easy_color, getTheme());
        } else if (label.equals("Medium")) {
            titleColor = getResources().getColor(R.color.medium_color, getTheme());
        } else if (label.equals("Hard")) {
            titleColor = getResources().getColor(R.color.hard_color, getTheme());
        }
        titleText.setTextColor(titleColor);
        
        // Create popup window
        pieChartPopup = new PopupWindow(popupView, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            true);
        
        // Set popup background and enable outside touch
        pieChartPopup.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background, getTheme()));
        pieChartPopup.setOutsideTouchable(true);
        pieChartPopup.setFocusable(true);
        
        // Add dismiss listener to clear pie chart highlight when popup is dismissed
        pieChartPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                // Clear the pie chart selection to merge segment back into pie with smooth animation
                if (pieChart != null) {
                    pieChart.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pieChart.highlightValues(null);
                        }
                    }, 200);
                }
            }
        });
        
        // Calculate position around the selected segment
        int[] location = new int[2];
        pieChart.getLocationOnScreen(location);
        
        // Get pie chart center
        int centerX = location[0] + pieChart.getWidth() / 2;
        int centerY = location[1] + pieChart.getHeight() / 2;
        
        // Show popup near the segment
        int popupX = centerX - 75; // Offset to center popup
        int popupY = centerY - 100; // Offset to show above segment
        
        // Ensure popup stays within screen bounds
        popupX = Math.max(20, Math.min(popupX, getResources().getDisplayMetrics().widthPixels - 200));
        popupY = Math.max(100, Math.min(popupY, getResources().getDisplayMetrics().heightPixels - 200));
        
        pieChartPopup.showAtLocation(pieChart, Gravity.NO_GRAVITY, popupX, popupY);
    }
    
    private void showCalendarDayPopup(int dayNumber, int problemCount, View clickedView) {
        // Hide existing popups
        hidePieChartPopup();
        hideCalendarDayPopup();
        
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.calendar_day_popup, null);
        
        // Get references to popup views
        TextView dateText = popupView.findViewById(R.id.popup_date);
        TextView problemsCountText = popupView.findViewById(R.id.popup_problems_count);
        
        // Format the date
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, dayNumber);
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                              "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        String dateString = monthNames[cal.get(Calendar.MONTH)] + " " + dayNumber + ", " + cal.get(Calendar.YEAR);
        
        // Set the data
        dateText.setText(dateString);
        problemsCountText.setText(problemCount + " problem" + (problemCount != 1 ? "s" : ""));
        
        // Create popup window
        calendarDayPopup = new PopupWindow(popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true);
        
        // Set popup background and enable outside touch
        calendarDayPopup.setBackgroundDrawable(getResources().getDrawable(R.drawable.popup_background, getTheme()));
        calendarDayPopup.setOutsideTouchable(true);
        calendarDayPopup.setFocusable(true);
        
        // Show popup above the clicked view
        int[] location = new int[2];
        clickedView.getLocationOnScreen(location);
        
        int popupX = location[0] + clickedView.getWidth() / 2 - 75; // Center horizontally
        int popupY = location[1] - 120; // Show above the clicked view
        
        calendarDayPopup.showAtLocation(clickedView, Gravity.NO_GRAVITY, popupX, popupY);
    }
    
    private void hidePieChartPopup() {
        if (pieChartPopup != null && pieChartPopup.isShowing()) {
            pieChartPopup.dismiss();
            pieChartPopup = null;
        }
    }
    
    private void hideCalendarDayPopup() {
        if (calendarDayPopup != null && calendarDayPopup.isShowing()) {
            calendarDayPopup.dismiss();
            calendarDayPopup = null;
        }
    }
    
    // Preference methods
    private void loadUserPreferences() {
        // Theme preference is loaded in loadThemePreference() before onCreate
        // Update UI to match loaded theme
        updateThemeToggleAppearance(false);
        updateViewColors();
    }
    
    private void saveThemePreference() {
        // Call the parent method to ensure consistent saving
        super.saveThemePreference(isDarkTheme);
    }
    
    private void fetchInitialData() {
        // Fetch data from LeetCode API
        fetchLeetCodeData();
        updateStats();
    }
    
    private void fetchLeetCodeData() {
        // Check if guest mode
        SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        
        if (isGuestMode) {
            // For guest mode, just update the contribution grid without fetching data
            System.out.println("DEBUG: Guest mode - skipping LeetCode data fetch");
            runOnUiThread(() -> {
                updateStats();
                updateContributionGrid();
            });
            return;
        }
        
        // Get the user's LeetCode username 
        String username = LoginActivity.getLeetCodeUsername(this);
        
        if (username == null || username.isEmpty()) {
            System.out.println("DEBUG: No username found - skipping LeetCode data fetch");
            runOnUiThread(() -> {
                updateStats();
                updateContributionGrid();
            });
            return;
        }
        
        System.out.println("DEBUG: Fetching LeetCode data for username: " + username);
        
        leetCodeAPI.getUserSubmissionStats(username, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                System.out.println("DEBUG: LeetCode API Success! Response length: " + response.length());
                System.out.println("DEBUG: Response preview: " + response.substring(0, Math.min(300, response.length())) + "...");
                
                // Move heavy data processing to background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            parseAndUpdateData(response);
                            
                            // Update UI on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateStats(); // This will now show the real LeetCode username
                                    updatePieChart();
                                    updateContributionGrid();
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("DEBUG: Error parsing LeetCode data: " + e.getMessage());
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateContributionGrid();
                                }
                            });
                        }
                    }
                }).start();
            }
            
            @Override
            public void onError(Exception error) {
                System.err.println("DEBUG: LeetCode API Error: " + error.getMessage());
                error.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateContributionGrid();
                    }
                });
                error.printStackTrace();
            }
        });
    }
    
    private void parseAndUpdateData(String jsonResponse) throws Exception {
        System.out.println("DEBUG: Starting to parse LeetCode response...");
        
        // Clear cache for fresh data
        monthsWithDataCache = null;
        
        org.json.JSONObject response = new org.json.JSONObject(jsonResponse);
        org.json.JSONObject data = response.getJSONObject("data");
        org.json.JSONObject matchedUser = data.getJSONObject("matchedUser");
        
        // Extract and save the real LeetCode username
        String realUsername = matchedUser.getString("username");
        System.out.println("DEBUG: Found LeetCode username: " + realUsername);
        
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("leetcode_username", realUsername);
        editor.apply();
        System.out.println("DEBUG: Saved username to SharedPreferences");
        
        // Update welcome text immediately with the real username
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (welcomeText != null) {
                    welcomeText.setText("Hi " + realUsername);
                    System.out.println("DEBUG: Updated welcome text to: Hi " + realUsername);
                } else {
                    System.err.println("DEBUG: welcomeText is null!");
                }
            }
        });
        
        // Parse submissionCalendar
        String submissionCalendarString = matchedUser.getString("submissionCalendar");
        submissionCalendarData = new org.json.JSONObject(submissionCalendarString);
        System.out.println("DEBUG: Parsed submission calendar with " + submissionCalendarData.length() + " entries");
        
        // Calculate streaks from submission calendar
        calculateStreaksFromCalendar();
        
        // Parse problem counts
        org.json.JSONObject submitStats = matchedUser.getJSONObject("submitStats");
        org.json.JSONArray acSubmissionNum = submitStats.getJSONArray("acSubmissionNum");
        
        int newEasy = 0, newMedium = 0, newHard = 0;
        
        for (int i = 0; i < acSubmissionNum.length(); i++) {
            org.json.JSONObject submission = acSubmissionNum.getJSONObject(i);
            String difficulty = submission.getString("difficulty");
            int count = submission.getInt("count");
            
            System.out.println("DEBUG: Found " + count + " " + difficulty + " problems");
            
            if ("Easy".equals(difficulty)) {
                newEasy = count;
            } else if ("Medium".equals(difficulty)) {
                newMedium = count;
            } else if ("Hard".equals(difficulty)) {
                newHard = count;
            }
        }
        
        System.out.println("DEBUG: Final counts - Easy: " + newEasy + ", Medium: " + newMedium + ", Hard: " + newHard);
        
        // Update stats if we got valid data
        if (newEasy > 0 || newMedium > 0 || newHard > 0) {
            easyProblems = newEasy;
            mediumProblems = newMedium;
            hardProblems = newHard;
            
            // Cache the problem counts
            cacheManager.cacheProblemCounts(easyProblems, mediumProblems, hardProblems);
            
            System.out.println("DEBUG: Updated problem counts with real data");
        } else {
            System.out.println("DEBUG: No valid problem counts found, keeping default values");
        }
        
        // Navigate to current month or first month with data
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        boolean currentMonthHasData = hasSubmissionsForMonth(currentMonth, currentYear);
        
        if (currentMonthHasData) {
            // Current month has data, stay here
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentCalendar.set(Calendar.YEAR, currentYear);
                    currentCalendar.set(Calendar.MONTH, currentMonth);
                    updateCalendarView();
                }
            });
        }
    }
    
    /**
     * Calculate current and longest streak from submission calendar
     */
    private void calculateStreaksFromCalendar() {
        if (submissionCalendarData == null || submissionCalendarData.length() == 0) {
            System.out.println("DEBUG: No submission data available for streak calculation");
            currentStreak = 0;
            longestStreak = 0;
            return;
        }
        
        try {
            // Get all timestamps and sort them
            java.util.List<Long> timestamps = new java.util.ArrayList<>();
            java.util.Iterator<String> keys = submissionCalendarData.keys();
            
            while (keys.hasNext()) {
                String key = keys.next();
                long timestamp = Long.parseLong(key);
                timestamps.add(timestamp);
            }
            
            if (timestamps.isEmpty()) {
                currentStreak = 0;
                longestStreak = 0;
                return;
            }
            
            // Sort in descending order (most recent first)
            java.util.Collections.sort(timestamps, java.util.Collections.reverseOrder());
            
            // Calculate current streak
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long todayTimestamp = today.getTimeInMillis() / 1000;
            
            Calendar yesterday = (Calendar) today.clone();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            long yesterdayTimestamp = yesterday.getTimeInMillis() / 1000;
            
            currentStreak = 0;
            boolean streakActive = false;
            
            // Check if there's a submission today or yesterday to start the streak
            for (long ts : timestamps) {
                Calendar subDate = Calendar.getInstance();
                subDate.setTimeInMillis(ts * 1000);
                subDate.set(Calendar.HOUR_OF_DAY, 0);
                subDate.set(Calendar.MINUTE, 0);
                subDate.set(Calendar.SECOND, 0);
                subDate.set(Calendar.MILLISECOND, 0);
                long subTimestamp = subDate.getTimeInMillis() / 1000;
                
                if (subTimestamp == todayTimestamp || subTimestamp == yesterdayTimestamp) {
                    streakActive = true;
                    break;
                }
            }
            
            if (streakActive) {
                // Count consecutive days
                java.util.Set<Long> uniqueDays = new java.util.HashSet<>();
                for (long ts : timestamps) {
                    Calendar subDate = Calendar.getInstance();
                    subDate.setTimeInMillis(ts * 1000);
                    subDate.set(Calendar.HOUR_OF_DAY, 0);
                    subDate.set(Calendar.MINUTE, 0);
                    subDate.set(Calendar.SECOND, 0);
                    subDate.set(Calendar.MILLISECOND, 0);
                    uniqueDays.add(subDate.getTimeInMillis() / 1000);
                }
                
                java.util.List<Long> sortedDays = new java.util.ArrayList<>(uniqueDays);
                java.util.Collections.sort(sortedDays, java.util.Collections.reverseOrder());
                
                Calendar checkDate = Calendar.getInstance();
                checkDate.set(Calendar.HOUR_OF_DAY, 0);
                checkDate.set(Calendar.MINUTE, 0);
                checkDate.set(Calendar.SECOND, 0);
                checkDate.set(Calendar.MILLISECOND, 0);
                
                for (long dayTimestamp : sortedDays) {
                    long expectedTimestamp = checkDate.getTimeInMillis() / 1000;
                    if (dayTimestamp == expectedTimestamp) {
                        currentStreak++;
                        checkDate.add(Calendar.DAY_OF_YEAR, -1);
                    } else {
                        break;
                    }
                }
            }
            
            // Calculate longest streak
            java.util.Set<Long> uniqueDays = new java.util.HashSet<>();
            for (long ts : timestamps) {
                Calendar subDate = Calendar.getInstance();
                subDate.setTimeInMillis(ts * 1000);
                subDate.set(Calendar.HOUR_OF_DAY, 0);
                subDate.set(Calendar.MINUTE, 0);
                subDate.set(Calendar.SECOND, 0);
                subDate.set(Calendar.MILLISECOND, 0);
                uniqueDays.add(subDate.getTimeInMillis() / 1000);
            }
            
            java.util.List<Long> allDays = new java.util.ArrayList<>(uniqueDays);
            java.util.Collections.sort(allDays);
            
            int tempStreak = 1;
            longestStreak = 1;
            
            for (int i = 1; i < allDays.size(); i++) {
                long dayDiff = (allDays.get(i) - allDays.get(i - 1)) / 86400; // Convert to days
                
                if (dayDiff == 1) {
                    tempStreak++;
                    longestStreak = Math.max(longestStreak, tempStreak);
                } else {
                    tempStreak = 1;
                }
            }
            
            // Cache the streak data
            cacheManager.cacheStreakData(currentStreak, longestStreak);
            
            System.out.println("DEBUG: Calculated streaks - Current: " + currentStreak + ", Longest: " + longestStreak);
            
        } catch (Exception e) {
            System.err.println("DEBUG: Error calculating streaks: " + e.getMessage());
            e.printStackTrace();
            currentStreak = 0;
            longestStreak = 0;
        }
    }
    
    private void updateStats() {
        // Check if guest mode
        SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
        boolean isGuestMode = prefs.getBoolean("is_guest_mode", false);
        
        if (isGuestMode) {
            // Show placeholder data for guest
            currentStreakText.setText("0");
            longestStreakText.setText("0");
            easyCountTableText.setText("0");
            mediumCountTableText.setText("0");
            hardCountTableText.setText("0");
            totalCountText.setText("0");
            welcomeText.setText("Hi Guest");
            return;
        }
        
        // Normal user - show real data
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
        
        // Update problem stats table
        easyCountTableText.setText(String.valueOf(easyProblems));
        mediumCountTableText.setText(String.valueOf(mediumProblems));
        hardCountTableText.setText(String.valueOf(hardProblems));
        totalCountText.setText(String.valueOf(easyProblems + mediumProblems + hardProblems));
        
        // Get LeetCode username from SharedPreferences, fallback to regular username if not available
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String leetcodeUsername = sharedPref.getString("leetcode_username", null);
        String username;
        
        if (leetcodeUsername != null && !leetcodeUsername.isEmpty()) {
            // Use the real LeetCode username
            username = leetcodeUsername;
        } else {
            // Fallback to regular username or default
            username = sharedPref.getString("username", "Champ");
        }
        
        welcomeText.setText("Hi " + username);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure home navigation is selected when returning to main activity
        // This handles cases where user presses back button from other activities
        if (navHome != null) {
            selectNavItem(0);
        }
        
        // Check if the date has changed since last time
        checkAndRefreshDailyGoals();
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Instead of finishing the activity, move task to background
            // This prevents creating multiple instances when user returns
            moveTaskToBack(true);
        }
    }
    
    // Inner classes for adapters would go here
    private class DailyGoalsAdapter extends RecyclerView.Adapter<DailyGoalsAdapter.ViewHolder> {
        private List<DailyGoal> goals;
        
        public DailyGoalsAdapter(List<DailyGoal> goals) {
            this.goals = goals;
            System.out.println("DEBUG: DailyGoalsAdapter created with " + goals.size() + " goals");
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            System.out.println("DEBUG: onCreateViewHolder called");
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_goal, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            System.out.println("DEBUG: onBindViewHolder called for position " + position);
            DailyGoal goal = goals.get(position);
            holder.goalTitle.setText(goal.getTitle());
            holder.goalCategory.setText(goal.getCategory());
            holder.difficultyBadge.setText(goal.getDifficulty());
            
            // Set current date in ordinal format (e.g., "28th August")
            holder.goalDate.setText(getOrdinalDateString());
            
            System.out.println("DEBUG: Binding goal - " + goal.getTitle() + " (" + goal.getDifficulty() + ")");
            
            // Apply theme-based colors to the card and text
            if (isDarkTheme) {
                // Dark theme colors for list items - using lighter background
                if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(getResources().getColor(R.color.leetcode_dark_card_light, getTheme()));
                }
                holder.goalTitle.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
                holder.goalCategory.setTextColor(getResources().getColor(R.color.leetcode_text_secondary, getTheme()));
                holder.goalDate.setTextColor(getResources().getColor(R.color.leetcode_text_secondary, getTheme()));
            } else {
                // Light theme colors for list items
                if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(getResources().getColor(R.color.background_secondary, getTheme()));
                }
                holder.goalTitle.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                holder.goalCategory.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
                holder.goalDate.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            }
            
            // Set difficulty badge background and text color
            if ("Medium".equals(goal.getDifficulty())) {
                holder.difficultyBadge.setBackgroundResource(R.drawable.difficulty_badge_medium);
            } else if ("Hard".equals(goal.getDifficulty())) {
                holder.difficultyBadge.setBackgroundResource(R.drawable.difficulty_badge_hard);
            } else {
                holder.difficultyBadge.setBackgroundResource(R.drawable.difficulty_badge_easy);
            }
            // Set text color to white for all difficulty badges
            holder.difficultyBadge.setTextColor(Color.WHITE);
            
            // Set click listener to navigate to problem detail
            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(ModernMainActivity.this, ProblemDetailActivity.class);
                    intent.putExtra("problem_title", goal.getTitle());
                    intent.putExtra("problem_difficulty", goal.getDifficulty());
                    intent.putExtra("problem_title_slug", goal.getTitleSlug());
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("DailyGoalsAdapter", "Error navigating to problem: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(ModernMainActivity.this, "Error opening problem details", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        @Override
        public int getItemCount() {
            System.out.println("DEBUG: getItemCount called - returning " + goals.size());
            return goals.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView goalTitle, goalCategory, difficultyBadge, goalDate;
            View statusIndicator;
            
            ViewHolder(View itemView) {
                super(itemView);
                goalTitle = itemView.findViewById(R.id.goalTitle);
                goalCategory = itemView.findViewById(R.id.goalCategory);
                difficultyBadge = itemView.findViewById(R.id.difficultyBadge);
                goalDate = itemView.findViewById(R.id.goalDate);
//                statusIndicator = itemView.findViewById(R.id.statusIndicator);
            }
        }
    }
    
    private class DailyGoal {
        private String title;
        private String category;
        private String difficulty;
        private boolean completed;
        private String titleSlug;
        
        public DailyGoal(String title, String category, String difficulty, boolean completed) {
            this.title = title;
            this.category = category;
            this.difficulty = difficulty;
            this.completed = completed;
            // Generate titleSlug from title (convert to lowercase and replace spaces with hyphens)
            this.titleSlug = title.toLowerCase().replace(" ", "-").replace(":", "").replace("'", "");
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getDifficulty() { return difficulty; }
        public boolean isCompleted() { return completed; }
        public String getTitleSlug() { return titleSlug; }
    }
    
    private void showRefreshSkeleton(boolean show) {
        if (show) {
            // Show skeleton loading during refresh
            if (skeletonView == null && skeletonStub != null) {
                try {
                    skeletonView = skeletonStub.inflate();
                    
                    // Start shimmer animation for all skeleton views
                    startRefreshSkeletonAnimation(skeletonView);
                } catch (Exception e) {
                    android.util.Log.e("ModernMainActivity", "Failed to inflate skeleton", e);
                    return;
                }
            }
            
            if (mainContentContainer != null) {
                mainContentContainer.setVisibility(View.INVISIBLE); // Use INVISIBLE instead of GONE to maintain layout
            }
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide skeleton and show content
            if (skeletonView != null) {
                skeletonView.setVisibility(View.GONE);
                stopRefreshSkeletonAnimation(skeletonView);
            }
            if (mainContentContainer != null) {
                mainContentContainer.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void startRefreshSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    startRefreshSkeletonAnimation(child);
                } else {
                    // Apply shimmer animation to skeleton elements
                    if (child.getBackground() != null) {
                        android.view.animation.Animation shimmer = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.skeleton_shimmer);
                        child.startAnimation(shimmer);
                    }
                }
            }
        }
    }
    
    private void stopRefreshSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    stopRefreshSkeletonAnimation(child);
                } else {
                    child.clearAnimation();
                }
            }
        }
    }
    
    private void setupNotificationControls() {
        // Set initial switch state
        boolean notificationsEnabled = NotificationScheduler.areNotificationsEnabled(this);
        notificationSwitch.setChecked(notificationsEnabled);
        
        // Update notification time info text
        updateNotificationTimeInfo();
        
        // Set up switch listener
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Check and request notification permission if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, 
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                                NOTIFICATION_PERMISSION_REQUEST_CODE);
                        return;
                    }
                }
                
                NotificationScheduler.setNotificationsEnabled(this, true);
                Toast.makeText(this, "Daily notifications enabled!", Toast.LENGTH_SHORT).show();
            } else {
                NotificationScheduler.setNotificationsEnabled(this, false);
                Toast.makeText(this, "Daily notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up morning time picker
        setMorningTimeButton.setOnClickListener(v -> showTimePickerDialog(true));
        
        // Set up evening time picker
        setEveningTimeButton.setOnClickListener(v -> showTimePickerDialog(false));
        
        // Set up test notification button
        testNotificationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationHelper.showDailyProblemNotification(this, true);
                Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enable notification permission first", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showTimePickerDialog(boolean isMorning) {
        int currentHour = isMorning ? NotificationScheduler.getMorningHour(this) : NotificationScheduler.getEveningHour(this);
        int currentMinute = isMorning ? NotificationScheduler.getMorningMinute(this) : NotificationScheduler.getEveningMinute(this);
        
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                if (isMorning) {
                    NotificationScheduler.setMorningTime(this, hourOfDay, minute);
                    Toast.makeText(this, String.format("Morning notification set to %02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
                } else {
                    NotificationScheduler.setEveningTime(this, hourOfDay, minute);
                    Toast.makeText(this, String.format("Evening notification set to %02d:%02d", hourOfDay, minute), Toast.LENGTH_SHORT).show();
                }
                updateNotificationTimeInfo();
            },
            currentHour,
            currentMinute,
            false // Use 12-hour format
        );
        
        timePickerDialog.setTitle(isMorning ? "Set Morning Notification Time" : "Set Evening Notification Time");
        timePickerDialog.show();
    }
    
    private void updateNotificationTimeInfo() {
        int morningHour = NotificationScheduler.getMorningHour(this);
        int morningMinute = NotificationScheduler.getMorningMinute(this);
        int eveningHour = NotificationScheduler.getEveningHour(this);
        int eveningMinute = NotificationScheduler.getEveningMinute(this);
        
        String morningTime = formatTime(morningHour, morningMinute);
        String eveningTime = formatTime(eveningHour, eveningMinute);
        
        String infoText = String.format("Get reminded twice daily: morning (%s) and evening (%s) to maintain your coding streak!", 
                                       morningTime, eveningTime);
        notificationTimeInfo.setText(infoText);
    }
    
    private String formatTime(int hour, int minute) {
        String period = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%d:%02d %s", displayHour, minute, period);
    }
    
    private void loadCachedDataOnStartup() {
        // Load cached problem counts
        int[] cachedCounts = cacheManager.getCachedProblemCounts();
        if (cachedCounts != null) {
            easyProblems = cachedCounts[0];
            mediumProblems = cachedCounts[1];
            hardProblems = cachedCounts[2];
            System.out.println("DEBUG: Loaded cached problem counts - Easy: " + easyProblems + 
                             ", Medium: " + mediumProblems + ", Hard: " + hardProblems);
        }
        
        // Load cached streak data
        int[] cachedStreaks = cacheManager.getCachedStreakData();
        if (cachedStreaks != null) {
            currentStreak = cachedStreaks[0];
            longestStreak = cachedStreaks[1];
            System.out.println("DEBUG: Loaded cached streak data - Current: " + currentStreak + 
                             ", Longest: " + longestStreak);
        }
        
        // Load cached daily goals
        List<?> cachedGoalsRaw = cacheManager.getCachedDailyGoals();
        if (cachedGoalsRaw != null && !cachedGoalsRaw.isEmpty()) {
            // Check if the cached goals are for today
            String currentDate = getCurrentDateString();
            SharedPreferences prefs = getSharedPreferences("daily_goals", Context.MODE_PRIVATE);
            String lastGoalsDate = prefs.getString("last_goals_date", "");
            
            if (currentDate.equals(lastGoalsDate)) {
                System.out.println("DEBUG: Loaded cached daily goals for today");
                // Will be used when the UI is ready
            }
        }
        
        // Update cache status after loading
        new Handler(Looper.getMainLooper()).postDelayed(() -> updateCacheStatus(), 1000);
    }
    
    // Cache management utility methods
    
    /**
     * Clear all caches - useful for troubleshooting or user logout
     */
    public void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.clearAllCaches();
            System.out.println("DEBUG: All caches cleared");
        }
    }
    
    /**
     * Get cache information for debugging
     */
    public String getCacheInfo() {
        if (cacheManager != null) {
            return cacheManager.getCacheInfo();
        }
        return "Cache manager not initialized";
    }
    
    /**
     * Update streak data and cache it
     */
    public void updateStreakData(int newCurrentStreak, int newLongestStreak) {
        currentStreak = newCurrentStreak;
        longestStreak = newLongestStreak;
        
        // Cache the updated streak data
        if (cacheManager != null) {
            cacheManager.cacheStreakData(currentStreak, longestStreak);
        }
        
        // Update UI
        runOnUiThread(() -> {
            currentStreakText.setText(String.valueOf(currentStreak));
            longestStreakText.setText(String.valueOf(longestStreak));
        });
        
        System.out.println("DEBUG: Updated and cached streak data - Current: " + currentStreak + 
                         ", Longest: " + longestStreak);
    }
    
    /**
     * Force refresh all cached data
     */
    public void refreshAllCaches() {
        if (cacheManager != null) {
            cacheManager.clearAllCaches();
            System.out.println("DEBUG: Refreshing all caches");
            
            // Trigger fresh data fetch
            fetchDailyGoalsFromAPI();
            
            // Update cache status
            updateCacheStatus();
            
            // You can add other data refresh calls here
            // For example, refresh user stats, submission calendar, etc.
        }
    }
    
    /**
     * Update cache status display
     */
    private void updateCacheStatus() {
        if (cacheManager != null && cacheStatusText != null) {
            runOnUiThread(() -> {
                String cacheInfo = cacheManager.getCacheInfo();
                boolean dailyGoalsValid = cacheManager.isCacheValid("daily_goals");
                
                String status = "Cache: " + cacheInfo;
                if (dailyGoalsValid) {
                    status += " • Goals cached ✓";
                } else {
                    status += " • Goals expired";
                }
                
                cacheStatusText.setText(status);
            });
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle new intent when activity is brought to front
        setIntent(intent);
        
        // Always reset navigation to home when returning to main activity
        // This includes both explicit navigation and implicit returns
        selectNavItem(0);
        
        // Check if theme settings should be shown
        if (intent.getBooleanExtra("show_theme_settings", false)) {
            if (themeToggleContainer != null) {
                themeToggleContainer.requestFocus();
            }
        }
        
        // Check if we're navigating to home explicitly
        if (intent.getBooleanExtra("navigate_to_home", false)) {
            // Scroll to top of main content if needed
            if (mainContentContainer != null) {
                mainContentContainer.smoothScrollTo(0, 0);
            }
        }
    }
    
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    
    // Notification management methods
    public void toggleNotifications(boolean enabled) {
        NotificationScheduler.setNotificationsEnabled(this, enabled);
        
        if (enabled) {
            // Show a test notification to confirm it's working
            NotificationHelper.showDailyProblemNotification(this, true);
        }
    }
    
    public void setNotificationTimes(int morningHour, int morningMinute, int eveningHour, int eveningMinute) {
        NotificationScheduler.setMorningTime(this, morningHour, morningMinute);
        NotificationScheduler.setEveningTime(this, eveningHour, eveningMinute);
    }
    
    public boolean areNotificationsEnabled() {
        return NotificationScheduler.areNotificationsEnabled(this);
    }
    
    public void showTestNotification() {
        NotificationHelper.showDailyProblemNotification(this, true);
    }
    
    // Developer mode activation methods
    private void setupDeveloperModeActivation() {
        if (welcomeText != null) {
            welcomeText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long currentTime = System.currentTimeMillis();
                    
                    // Reset count if too much time has passed since last click
                    if (currentTime - lastWelcomeClickTime > CLICK_TIMEOUT) {
                        welcomeTextClickCount = 0;
                    }
                    
                    welcomeTextClickCount++;
                    lastWelcomeClickTime = currentTime;
                    
                    if (welcomeTextClickCount >= DEVELOPER_MODE_CLICKS) {
                        activateDeveloperMode();
                        welcomeTextClickCount = 0; // Reset counter
                    }
                }
            });
        }
    }
    
    private void activateDeveloperMode() {
        if (developerModeEnabled) {
            Toast.makeText(this, "🔧 Developer Mode Already Active", Toast.LENGTH_SHORT).show();
            return;
        }
        
        developerModeEnabled = true;
        
        // Show developer mode dialog with Google AI Edge Gallery options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🚀 Developer Mode Activated")
            .setMessage("Welcome to Developer Mode!\n\n" +
                       "Available Options:\n" +
                       "• 🏗️ Google Architecture Demo\n" +
                       "• 📧 Gmail Auth Example\n" +
                       "• 🤖 Enhanced AI Chat\n" +
                       "• 🔍 Detect Existing Models")
            .setPositiveButton("🏗️ Architecture", (dialog, which) -> {
                // Launch GoogleArchitectureExampleActivity
                Intent intent = new Intent(this, GoogleArchitectureExampleActivity.class);
                startActivity(intent);
            })
            .setNeutralButton("🤖 AI Chat", (dialog, which) -> {
                // Launch enhanced AI Chat with Google models
                Intent intent = new Intent(this, AIChatActivity.class);
                intent.putExtra("problem_title", "Developer Mode Chat");
                intent.putExtra("problem_description", "Enhanced AI chat with Google model downloads");
                startActivity(intent);
            })
            .setNegativeButton("🔍 Models", (dialog, which) -> {
                // Detect existing Google AI Edge models
                detectExistingGoogleModels();
            })
            .show();
        
        // Visual feedback
        Toast.makeText(this, "🚀 Developer Mode Activated!", Toast.LENGTH_LONG).show();
        
        // Add subtle visual indicator
        if (welcomeText != null) {
            welcomeText.setText(welcomeText.getText() + " 🔧");
        }
    }
    
    private void detectExistingGoogleModels() {
        // Use the AISolutionHelper_backup method to detect existing models
        try {
            AISolutionHelper_backup helper = new AISolutionHelper_backup(this);
            helper.detectExistingGoogleModels(this, new AISolutionHelper_backup.GoogleDownloadCallback() {
                @Override
                public void onProgress(int progress, String status) {
                    // Progress updates during detection
                }
                
                @Override
                public void onComplete(String modelPath) {
                    Toast.makeText(ModernMainActivity.this, "Model ready: " + modelPath, Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(ModernMainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error detecting models: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
