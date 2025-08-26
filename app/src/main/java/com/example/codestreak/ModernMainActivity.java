package com.example.codestreak;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ModernMainActivity extends AppCompatActivity {
    
    // UI Components
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout navHome, navProgress, navCards, navRevision;
    private ImageView homeIcon, progressIcon, cardsIcon, revisionIcon;
    private TextView homeText, progressText, cardsText, revisionText;
    private View homeIndicator, progressIndicator, cardsIndicator, revisionIndicator;
    private com.google.android.material.card.MaterialCardView bottomNavCard;
    private FrameLayout themeToggleContainer;
    private com.google.android.material.appbar.MaterialToolbar toolbar;
    private ImageButton menuButton;
    private View toggleThumb;
    private ImageView themeIcon, sunIcon, moonIcon;
    private TextView welcomeText;
    private TextView currentStreakText, longestStreakText;
    private TextView easyCountTableText, mediumCountTableText, hardCountTableText, totalCountText;
    private TextView goalCountBadge;
    private RecyclerView dailyGoalsRecyclerView;
    private RecyclerView contributionGrid;
    private PieChart pieChart;
    private com.google.android.material.card.MaterialCardView problemDistributionCard;
    private LinearLayout statsTableContainer;
    private TextView monthYearText;
    private ImageButton prevMonthButton, nextMonthButton;
    
    // Data
    private boolean isDarkTheme = true;
    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    private int currentStreak = 15;
    private int longestStreak = 28;
    private Calendar currentCalendar;
    private UltraSimpleAdapter contributionAdapter;
    private LeetCodeAPI leetCodeAPI;
    
    // Add submission calendar data and caching like MainActivity
    private org.json.JSONObject submissionCalendarData;
    private java.util.Set<String> monthsWithDataCache = null;
    
    // Popups
    private PopupWindow pieChartPopup;
    private PopupWindow calendarDayPopup;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before calling super.onCreate()
        loadThemePreference();
        
        // Apply theme before setting content view
        if (isDarkTheme) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
        } else {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_modern);
        
        // Initialize views and setup
        initializeViews();
        setupThemeToggle();
        setupNavigationDrawer();
        setupBottomNavigation();
        setupPieChart();
        setupCalendar();
        setupDailyGoals();
        
        // Load user preferences
        loadUserPreferences();
        
        // Initialize data
        leetCodeAPI = new LeetCodeAPI();
        fetchInitialData();
    }
    
    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        initializeCustomBottomNavigation();
        bottomNavCard = findViewById(R.id.bottomNavCard);
        themeToggleContainer = findViewById(R.id.themeToggleContainer);
        toolbar = findViewById(R.id.toolbar);
        menuButton = findViewById(R.id.menuButton);
        toggleThumb = findViewById(R.id.toggleThumb);
        themeIcon = findViewById(R.id.themeIcon);
        sunIcon = findViewById(R.id.sunIcon);
        moonIcon = findViewById(R.id.moonIcon);
        welcomeText = findViewById(R.id.welcomeText);
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        easyCountTableText = findViewById(R.id.easyCountTableText);
        mediumCountTableText = findViewById(R.id.mediumCountTableText);
        hardCountTableText = findViewById(R.id.hardCountTableText);
        totalCountText = findViewById(R.id.totalCountText);
        goalCountBadge = findViewById(R.id.goalCountBadge);
        dailyGoalsRecyclerView = findViewById(R.id.dailyGoalsRecyclerView);
        contributionGrid = findViewById(R.id.contributionGrid);
        pieChart = findViewById(R.id.pieChart);
        problemDistributionCard = findViewById(R.id.problemDistributionCard);
        statsTableContainer = findViewById(R.id.statsTableContainer);
        monthYearText = findViewById(R.id.monthYearText);
        prevMonthButton = findViewById(R.id.prevMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        
        // Setup menu button
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }
    
    private void setupThemeToggle() {
        themeToggleContainer.setOnClickListener(v -> toggleTheme());
        updateThemeToggleAppearance(false);
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
        // Calculate complete slide positions
        // Light mode: thumb on left (2dp), Dark mode: thumb on right (26dp)
        float lightModeThumbX = 2f;
        float darkModeThumbX = 26f;
        
        // Active icon positions (centered on thumb)
        float lightModeIconX = 6f;  // 2 + 4dp margin for centering
        float darkModeIconX = 30f;  // 26 + 4dp margin for centering
        
        float targetThumbX = isDarkTheme ? darkModeThumbX : lightModeThumbX;
        float targetIconX = isDarkTheme ? darkModeIconX : lightModeIconX;
        
        if (animate) {
            // Get current positions
            float currentThumbX = toggleThumb.getTranslationX();
            float currentIconX = themeIcon.getTranslationX();
            
            // Animate thumb sliding completely across
            ValueAnimator thumbAnimator = ValueAnimator.ofFloat(currentThumbX, targetThumbX);
            thumbAnimator.setDuration(300);
            thumbAnimator.setInterpolator(new DecelerateInterpolator());
            thumbAnimator.addUpdateListener(animation -> {
                float value = (Float) animation.getAnimatedValue();
                toggleThumb.setTranslationX(value);
            });
            thumbAnimator.start();
            
            // Animate active icon moving with thumb
            ValueAnimator iconMoveAnimator = ValueAnimator.ofFloat(currentIconX, targetIconX);
            iconMoveAnimator.setDuration(300);
            iconMoveAnimator.setInterpolator(new DecelerateInterpolator());
            iconMoveAnimator.addUpdateListener(animation -> {
                float value = (Float) animation.getAnimatedValue();
                themeIcon.setTranslationX(value);
            });
            iconMoveAnimator.start();
            
            // Animate track background color
            int currentColor = isDarkTheme ? 
                getResources().getColor(R.color.toggle_track_light, getTheme()) :
                getResources().getColor(R.color.toggle_track_dark, getTheme());
            int targetColor = isDarkTheme ? 
                getResources().getColor(R.color.toggle_track_dark, getTheme()) :
                getResources().getColor(R.color.toggle_track_light, getTheme());
                
            ValueAnimator colorAnimator = ValueAnimator.ofArgb(currentColor, targetColor);
            colorAnimator.setDuration(300);
            colorAnimator.addUpdateListener(animation -> {
                int color = (Integer) animation.getAnimatedValue();
                themeToggleContainer.setBackgroundColor(color);
            });
            colorAnimator.start();
            
            // Animate background icons visibility
            if (isDarkTheme) {
                // Switching to dark mode - highlight moon, dim sun
                sunIcon.animate().alpha(0.3f).setDuration(200);
                moonIcon.animate().alpha(1.0f).setDuration(200);
            } else {
                // Switching to light mode - highlight sun, dim moon
                sunIcon.animate().alpha(1.0f).setDuration(200);
                moonIcon.animate().alpha(0.3f).setDuration(200);
            }
            
            // Animate active icon change with scale effect
            themeIcon.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .withEndAction(() -> {
                    // Change icon
                    themeIcon.setImageResource(isDarkTheme ? R.drawable.ic_moon : R.drawable.ic_sun);
                    themeIcon.setColorFilter(android.graphics.Color.WHITE);
                    
                    // Scale back up
                    themeIcon.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start();
                })
                .start();
                
        } else {
            // Set positions immediately without animation
            toggleThumb.setTranslationX(targetThumbX);
            themeIcon.setTranslationX(targetIconX);
            themeIcon.setImageResource(isDarkTheme ? R.drawable.ic_moon : R.drawable.ic_sun);
            themeIcon.setColorFilter(android.graphics.Color.WHITE);
            
            // Set track color
            int trackColor = isDarkTheme ? 
                getResources().getColor(R.color.toggle_track_dark, getTheme()) :
                getResources().getColor(R.color.toggle_track_light, getTheme());
            themeToggleContainer.setBackgroundColor(trackColor);
            
            // Set background icons visibility
            if (isDarkTheme) {
                sunIcon.setAlpha(0.3f);
                moonIcon.setAlpha(1.0f);
            } else {
                sunIcon.setAlpha(1.0f);
                moonIcon.setAlpha(0.3f);
            }
        }
    }
    
    private void applyTheme() {
        // Apply proper Material Design theme
        if (isDarkTheme) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
        } else {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        }
        
        // Apply dynamic theme changes to views
        updateViewColors();
        
        // Recreate activity to apply theme properly
        new Handler(Looper.getMainLooper()).postDelayed(this::recreate, 100);
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
            findViewById(R.id.appBarLayout).setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            findViewById(R.id.navigationView).setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            
            // Update floating bottom navigation card for dark theme
            if (bottomNavCard != null) {
                bottomNavCard.setCardBackgroundColor(getResources().getColor(R.color.leetcode_card_bg, getTheme()));
            }
            
            // Update toolbar background for dark theme
            if (toolbar != null) {
                toolbar.setBackgroundColor(getResources().getColor(R.color.leetcode_card_bg, getTheme()));
            }
            
            // Update toolbar icon colors for dark theme
            if (menuButton != null) {
                menuButton.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
            }
            if (themeIcon != null) {
                themeIcon.setColorFilter(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
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
            // Light theme colors
            rootView.setBackgroundColor(getResources().getColor(R.color.modern_background, getTheme()));
            
            // Update drawer layout background
            drawerLayout.setBackgroundColor(getResources().getColor(R.color.modern_background, getTheme()));
            
            // Update all major UI elements with light backgrounds
            findViewById(R.id.appBarLayout).setBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            findViewById(R.id.navigationView).setBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            
            // Update floating bottom navigation card for light theme
            if (bottomNavCard != null) {
                bottomNavCard.setCardBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            }
            
            // Update toolbar background for light theme
            if (toolbar != null) {
                toolbar.setBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            }
            
            // Update toolbar icon colors for light theme
            if (menuButton != null) {
                menuButton.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            if (themeIcon != null) {
                themeIcon.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }
            
            // This is the key fix - update the main CoordinatorLayout background
            View coordinatorLayout = drawerLayout.getChildAt(0); // First child is the CoordinatorLayout
            if (coordinatorLayout != null) {
                coordinatorLayout.setBackgroundColor(getResources().getColor(R.color.modern_background, getTheme()));
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
    
    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        isDarkTheme = prefs.getBoolean("dark_theme", false); // Default to light theme
    }
    
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_profile) {
                // Handle profile
            } else if (id == R.id.nav_stats) {
                // Handle statistics
            } else if (id == R.id.nav_achievements) {
                // Handle achievements
            } else if (id == R.id.nav_settings) {
                // Handle settings
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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
        navProgress.setOnClickListener(v -> selectNavItem(1));
        navCards.setOnClickListener(v -> selectNavItem(2));
        navRevision.setOnClickListener(v -> selectNavItem(3));
        
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
            case 2: // Cards
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
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(easyProblems, "Easy"));
        entries.add(new PieEntry(mediumProblems, "Medium"));
        entries.add(new PieEntry(hardProblems, "Hard"));
        
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
                return String.valueOf((int) value);
            }
        });
        
        dataSet.setSelectionShift(5f);
        dataSet.setHighlightEnabled(true);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        
        int totalProblems = easyProblems + mediumProblems + hardProblems;
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
        
        // Sample goals data
        List<DailyGoal> goals = generateSampleGoals();
        DailyGoalsAdapter adapter = new DailyGoalsAdapter(goals);
        dailyGoalsRecyclerView.setAdapter(adapter);
        
        goalCountBadge.setText(String.valueOf(goals.size()));
    }
    
    private List<DailyGoal> generateSampleGoals() {
        List<DailyGoal> goals = new ArrayList<>();
        goals.add(new DailyGoal("Reverse An Array", "Array", "Easy", false));
        goals.add(new DailyGoal("Min And Max In Array", "Array", "Easy", false));
        goals.add(new DailyGoal("Max Consecutive Ones", "Array", "Easy", false));
        goals.add(new DailyGoal("Find Numbers With Even Number Of Digits", "Array", "Easy", false));
        goals.add(new DailyGoal("Duplicate Zeros", "Array", "Easy", false));
        goals.add(new DailyGoal("Squares of Sorted Array", "Array", "Easy", false));
        return goals;
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
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_theme", isDarkTheme).apply();
    }
    
    private void fetchInitialData() {
        // Fetch data from LeetCode API
        fetchLeetCodeData();
        updateStats();
    }
    
    private void fetchLeetCodeData() {
        // Get the user's LeetCode username 
        String username = "adityashak04"; // Default username
        
        leetCodeAPI.getUserSubmissionStats(username, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
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
                                    updateStats();
                                    updatePieChart();
                                    updateContributionGrid();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateContributionGrid();
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            
            @Override
            public void onError(Exception error) {
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
        // Clear cache for fresh data
        monthsWithDataCache = null;
        
        org.json.JSONObject response = new org.json.JSONObject(jsonResponse);
        org.json.JSONObject data = response.getJSONObject("data");
        org.json.JSONObject matchedUser = data.getJSONObject("matchedUser");
        
        // Parse submissionCalendar
        String submissionCalendarString = matchedUser.getString("submissionCalendar");
        submissionCalendarData = new org.json.JSONObject(submissionCalendarString);
        
        // Parse problem counts
        org.json.JSONObject submitStats = matchedUser.getJSONObject("submitStats");
        org.json.JSONArray acSubmissionNum = submitStats.getJSONArray("acSubmissionNum");
        
        int newEasy = 0, newMedium = 0, newHard = 0;
        
        for (int i = 0; i < acSubmissionNum.length(); i++) {
            org.json.JSONObject submission = acSubmissionNum.getJSONObject(i);
            String difficulty = submission.getString("difficulty");
            int count = submission.getInt("count");
            
            if ("Easy".equals(difficulty)) {
                newEasy = count;
            } else if ("Medium".equals(difficulty)) {
                newMedium = count;
            } else if ("Hard".equals(difficulty)) {
                newHard = count;
            }
        }
        
        // Update stats if we got valid data
        if (newEasy > 0 || newMedium > 0 || newHard > 0) {
            easyProblems = newEasy;
            mediumProblems = newMedium;
            hardProblems = newHard;
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
    
    private void updateStats() {
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
        
        // Update problem stats table
        easyCountTableText.setText(String.valueOf(easyProblems));
        mediumCountTableText.setText(String.valueOf(mediumProblems));
        hardCountTableText.setText(String.valueOf(hardProblems));
        totalCountText.setText(String.valueOf(easyProblems + mediumProblems + hardProblems));
        
        // Get username from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String username = sharedPref.getString("username", "Champ");
        welcomeText.setText("Hi " + username);
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    // Inner classes for adapters would go here
    private class DailyGoalsAdapter extends RecyclerView.Adapter<DailyGoalsAdapter.ViewHolder> {
        private List<DailyGoal> goals;
        
        public DailyGoalsAdapter(List<DailyGoal> goals) {
            this.goals = goals;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_goal, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DailyGoal goal = goals.get(position);
            holder.goalTitle.setText(goal.getTitle());
            holder.goalCategory.setText(goal.getCategory());
            holder.difficultyBadge.setText(goal.getDifficulty());
            
            // Apply theme-based colors to the card and text
            if (isDarkTheme) {
                // Dark theme colors for list items - using lighter background
                if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(getResources().getColor(R.color.leetcode_dark_card_light, getTheme()));
                }
                holder.goalTitle.setTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme()));
                holder.goalCategory.setTextColor(getResources().getColor(R.color.leetcode_text_secondary, getTheme()));
            } else {
                // Light theme colors for list items
                if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
                    ((com.google.android.material.card.MaterialCardView) holder.itemView)
                        .setCardBackgroundColor(getResources().getColor(R.color.background_secondary, getTheme()));
                }
                holder.goalTitle.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                holder.goalCategory.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            }
            
            // Set difficulty badge color
            int colorRes = R.color.easy_color;
            if ("Medium".equals(goal.getDifficulty())) {
                colorRes = R.color.medium_color;
            } else if ("Hard".equals(goal.getDifficulty())) {
                colorRes = R.color.hard_color;
            }
            holder.difficultyBadge.setTextColor(getResources().getColor(colorRes, getTheme()));
        }
        
        @Override
        public int getItemCount() {
            return goals.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView goalTitle, goalCategory, difficultyBadge;
            View statusIndicator;
            
            ViewHolder(View itemView) {
                super(itemView);
                goalTitle = itemView.findViewById(R.id.goalTitle);
                goalCategory = itemView.findViewById(R.id.goalCategory);
                difficultyBadge = itemView.findViewById(R.id.difficultyBadge);
                statusIndicator = itemView.findViewById(R.id.statusIndicator);
            }
        }
    }
    
    private class DailyGoal {
        private String title;
        private String category;
        private String difficulty;
        private boolean completed;
        
        public DailyGoal(String title, String category, String difficulty, boolean completed) {
            this.title = title;
            this.category = category;
            this.difficulty = difficulty;
            this.completed = completed;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getDifficulty() { return difficulty; }
        public boolean isCompleted() { return completed; }
    }
}
