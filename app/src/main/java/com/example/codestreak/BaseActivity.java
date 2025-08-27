package com.example.codestreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected boolean isDarkTheme = false;
    
    // Static method to get current theme preference
    public static boolean isDarkThemeEnabled(SharedPreferences prefs) {
        return prefs.getBoolean("dark_theme", false);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before calling super.onCreate()
        loadThemePreference();
        
        // Apply theme before setting content view
        applyTheme();
        
        super.onCreate(savedInstanceState);
    }
    
    private void applyTheme() {
        if (isDarkTheme) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar);
        } else {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Always check theme preference on resume in case it was changed in another activity
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean currentTheme = isDarkThemeEnabled(prefs);
        
        if (currentTheme != isDarkTheme) {
            // Theme changed, recreate activity to apply new theme
            isDarkTheme = currentTheme;
            recreate();
            return;
        }
    }
    
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupBaseNavigation();
    }
    
    private void setupBaseNavigation() {
        // Find common navigation elements
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        
        // Setup navigation if available
        if (navigationView != null) {
            setupNavigationDrawer();
        }
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // Setup menu button
        ImageButton menuButton = findViewById(R.id.menuButton);
        if (menuButton != null && drawerLayout != null) {
            menuButton.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }
    
    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                if (!(this instanceof ModernMainActivity)) {
                    Intent intent = new Intent(this, ModernMainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            } else if (id == R.id.nav_problems) {
                if (!(this instanceof ProblemsActivity)) {
                    Intent intent = new Intent(this, ProblemsActivity.class);
                    startActivity(intent);
                }
            } else if (id == R.id.nav_theme_settings) {
                // Navigate to home for theme settings
                Intent intent = new Intent(this, ModernMainActivity.class);
                intent.putExtra("show_theme_settings", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else if (id == R.id.nav_profile) {
                // Handle profile
            } else if (id == R.id.nav_stats) {
                // Handle statistics
            } else if (id == R.id.nav_achievements) {
                // Handle achievements
            } else if (id == R.id.nav_settings) {
                // Handle app settings
            }
            
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    
    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        isDarkTheme = isDarkThemeEnabled(prefs);
    }
    
    protected void saveThemePreference(boolean darkTheme) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_theme", darkTheme).apply();
        this.isDarkTheme = darkTheme;
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
