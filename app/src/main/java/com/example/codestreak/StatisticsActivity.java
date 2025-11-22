package com.example.codestreak;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;

public class StatisticsActivity extends BaseActivity {

    private ImageButton backButton;
    private TextView totalSolvedText, easyCountText, mediumCountText, hardCountText;
    private TextView acceptanceRateText, submissionCountText, acceptedCountText;
    private TextView easyPercentText, mediumPercentText, hardPercentText;
    private BarChart difficultyBarChart;
    private LineChart streakLineChart;
    private MaterialCardView overviewCard, submissionCard, difficultyCard, activityCard;
    
    private ViewStub skeletonStub;
    private View skeletonView;
    private View contentScrollView;
    
    private SharedPreferences sharedPreferences;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        
        // Overview stats
        totalSolvedText = findViewById(R.id.totalSolvedText);
        easyCountText = findViewById(R.id.easyCountText);
        mediumCountText = findViewById(R.id.mediumCountText);
        hardCountText = findViewById(R.id.hardCountText);
        
        // Submission stats
        acceptanceRateText = findViewById(R.id.acceptanceRateText);
        submissionCountText = findViewById(R.id.submissionCountText);
        acceptedCountText = findViewById(R.id.acceptedCountText);
        
        // Percentage texts
        easyPercentText = findViewById(R.id.easyPercentText);
        mediumPercentText = findViewById(R.id.mediumPercentText);
        hardPercentText = findViewById(R.id.hardPercentText);
        
        // Charts
        difficultyBarChart = findViewById(R.id.difficultyBarChart);
        streakLineChart = findViewById(R.id.streakLineChart);
        
        // Cards
        overviewCard = findViewById(R.id.overviewCard);
        submissionCard = findViewById(R.id.submissionCard);
        difficultyCard = findViewById(R.id.difficultyCard);
        activityCard = findViewById(R.id.activityCard);
        
        // Skeleton
        skeletonStub = findViewById(R.id.skeletonStub);
        contentScrollView = findViewById(R.id.contentScrollView);
        
        sharedPreferences = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
    }

    private void loadUserData() {
        // Check if guest mode
        boolean isGuestMode = sharedPreferences.getBoolean("is_guest_mode", false);
        
        if (isGuestMode) {
            // Show guest mode message
            showGuestModePrompt();
            return;
        }
        
        username = sharedPreferences.getString("leetcode_username", "");
        if (username.isEmpty()) {
            username = sharedPreferences.getString("username", "");
        }
        
        if (username.isEmpty()) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchStatistics();
    }
    
    private void showGuestModePrompt() {
        showSkeleton(false);
        contentScrollView.setVisibility(View.VISIBLE);
        
        // Show placeholder data
        totalSolvedText.setText("—");
        easyCountText.setText("0");
        mediumCountText.setText("0");
        hardCountText.setText("0");
        acceptanceRateText.setText("—");
        submissionCountText.setText("0");
        acceptedCountText.setText("0");
        easyPercentText.setText("0%");
        mediumPercentText.setText("0%");
        hardPercentText.setText("0%");
        
        Toast.makeText(this, "Login to view your statistics", Toast.LENGTH_LONG).show();
    }

    private void fetchStatistics() {
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
                    updateStatistics(jsonResponse);
                    setupStreakChart(jsonResponse);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showSkeleton(false);
                    Toast.makeText(StatisticsActivity.this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
                    loadLocalData();
                });
            }
        }).start();
    }

    private void updateStatistics(JSONObject data) {
        try {
            int totalSolved = data.optInt("totalSolved", 0);
            int easySolved = data.optInt("easySolved", 0);
            int mediumSolved = data.optInt("mediumSolved", 0);
            int hardSolved = data.optInt("hardSolved", 0);
            
            int easyTotal = data.optInt("totalEasy", 822);
            int mediumTotal = data.optInt("totalMedium", 1717);
            int hardTotal = data.optInt("totalHard", 741);
            
            double acceptanceRate = data.optDouble("acceptanceRate", 0.0);
            int totalSubmissions = data.optInt("totalSubmissions", 0);
            int totalAccepted = data.optInt("totalAccepted", 0);

            // Update overview stats
            totalSolvedText.setText(String.valueOf(totalSolved));
            easyCountText.setText(easySolved + "/" + easyTotal);
            mediumCountText.setText(mediumSolved + "/" + mediumTotal);
            hardCountText.setText(hardSolved + "/" + hardTotal);
            
            // Update submission stats
            acceptanceRateText.setText(String.format("%.1f%%", acceptanceRate));
            submissionCountText.setText(String.valueOf(totalSubmissions));
            acceptedCountText.setText(String.valueOf(totalAccepted));
            
            // Calculate percentages
            double easyPercent = easyTotal > 0 ? (easySolved * 100.0 / easyTotal) : 0;
            double mediumPercent = mediumTotal > 0 ? (mediumSolved * 100.0 / mediumTotal) : 0;
            double hardPercent = hardTotal > 0 ? (hardSolved * 100.0 / hardTotal) : 0;
            
            easyPercentText.setText(String.format("%.1f%%", easyPercent));
            mediumPercentText.setText(String.format("%.1f%%", mediumPercent));
            hardPercentText.setText(String.format("%.1f%%", hardPercent));
            
            // Setup charts
            setupBarChart(easySolved, mediumSolved, hardSolved);
            // Note: setupStreakChart is called separately with full JSON data

        } catch (Exception e) {
            e.printStackTrace();
            loadLocalData();
        }
    }

    private void loadLocalData() {
        int totalSolved = sharedPreferences.getInt("totalSolved", 0);
        int easySolved = sharedPreferences.getInt("easySolved", 0);
        int mediumSolved = sharedPreferences.getInt("mediumSolved", 0);
        int hardSolved = sharedPreferences.getInt("hardSolved", 0);

        totalSolvedText.setText(String.valueOf(totalSolved));
        easyCountText.setText(String.valueOf(easySolved));
        mediumCountText.setText(String.valueOf(mediumSolved));
        hardCountText.setText(String.valueOf(hardSolved));
        
        acceptanceRateText.setText("--");
        submissionCountText.setText("--");
        acceptedCountText.setText("--");
        
        easyPercentText.setText("--");
        mediumPercentText.setText("--");
        hardPercentText.setText("--");
        
        setupBarChart(easySolved, mediumSolved, hardSolved);
        setupStreakChart(null);
    }

    private void setupBarChart(int easy, int medium, int hard) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, easy));
        entries.add(new BarEntry(1, medium));
        entries.add(new BarEntry(2, hard));

        BarDataSet dataSet = new BarDataSet(entries, "Problems Solved");
        
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#00B8A3"));
        colors.add(Color.parseColor("#FFA116"));
        colors.add(Color.parseColor("#FF375F"));
        
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        
        difficultyBarChart.setData(data);
        difficultyBarChart.getDescription().setEnabled(false);
        difficultyBarChart.setFitBars(true);
        difficultyBarChart.animateY(1000);
        
        // X-axis
        XAxis xAxis = difficultyBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.text_primary));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                String[] labels = {"Easy", "Medium", "Hard"};
                int index = (int) value;
                return index >= 0 && index < labels.length ? labels[index] : "";
            }
        });
        
        // Y-axis
        YAxis leftAxis = difficultyBarChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.text_primary));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getResources().getColor(R.color.divider));
        
        difficultyBarChart.getAxisRight().setEnabled(false);
        difficultyBarChart.getLegend().setEnabled(false);
        difficultyBarChart.invalidate();
    }

    private void setupStreakChart(JSONObject data) {
        ArrayList<Entry> entries = new ArrayList<>();
        
        try {
            if (data != null && data.has("submissionCalendar")) {
                // Parse submission calendar from API
                JSONObject calendar = data.getJSONObject("submissionCalendar");
                
                // Get timestamps from calendar and sort them
                List<Long> timestamps = new ArrayList<>();
                java.util.Iterator<String> keys = calendar.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    timestamps.add(Long.parseLong(key));
                }
                java.util.Collections.sort(timestamps);
                
                // Get current time and calculate last 7 days
                long currentTime = System.currentTimeMillis() / 1000;
                long oneDaySeconds = 86400;
                
                // Initialize counts for last 7 days
                int[] dailyCounts = new int[7];
                
                // Count submissions for each of the last 7 days
                for (Long timestamp : timestamps) {
                    long daysAgo = (currentTime - timestamp) / oneDaySeconds;
                    if (daysAgo >= 0 && daysAgo < 7) {
                        int index = 6 - (int)daysAgo; // Reverse order (0 = oldest, 6 = today)
                        dailyCounts[index] += calendar.getInt(String.valueOf(timestamp));
                    }
                }
                
                // Create entries from daily counts
                for (int i = 0; i < 7; i++) {
                    entries.add(new Entry(i, dailyCounts[i]));
                }
                
            } else {
                // Fallback: Use streak-based estimation
                int currentStreak = sharedPreferences.getInt("currentStreak", 0);
                
                for (int i = 0; i < 7; i++) {
                    float activity;
                    if (currentStreak > 0 && i >= 7 - currentStreak) {
                        activity = 1 + (float)(Math.random() * 2);
                    } else {
                        activity = (float)(Math.random() * 1.5);
                    }
                    entries.add(new Entry(i, activity));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("StatisticsActivity", "Error parsing submission calendar: " + e.getMessage());
            // Fallback to estimated data
            int currentStreak = sharedPreferences.getInt("currentStreak", 0);
            for (int i = 0; i < 7; i++) {
                float activity = (currentStreak > 0 && i >= 7 - currentStreak) ? 2f : 0.5f;
                entries.add(new Entry(i, activity));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Problems Solved");
        dataSet.setColor(Color.parseColor("#FFA116"));
        dataSet.setCircleColor(Color.parseColor("#FFA116"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.parseColor("#0A0A0A"));
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FFA116"));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        LineData lineData = new LineData(dataSet);
        
        streakLineChart.setData(lineData);
        streakLineChart.getDescription().setEnabled(false);
        streakLineChart.animateX(1000);
        streakLineChart.setBackgroundColor(getResources().getColor(R.color.surface_primary));
        streakLineChart.setDrawGridBackground(false);
        
        // X-axis - Get actual day names for last 7 days
        XAxis xAxis = streakLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.text_primary));
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Get actual day names for last 7 days
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -6 + (int)value);
                String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                return days[calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1];
            }
        });
        
        // Y-axis
        YAxis leftAxis = streakLineChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.text_primary));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getResources().getColor(R.color.divider));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        
        streakLineChart.getAxisRight().setEnabled(false);
        
        Legend legend = streakLineChart.getLegend();
        legend.setTextColor(getResources().getColor(R.color.text_primary));
        legend.setTextSize(12f);
        legend.setFormSize(10f);
        
        streakLineChart.invalidate();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void showSkeleton(boolean show) {
        if (show) {
            if (skeletonView == null && skeletonStub != null) {
                try {
                    skeletonView = skeletonStub.inflate();
                    startSkeletonAnimation(skeletonView);
                } catch (Exception e) {
                    android.util.Log.e("StatisticsActivity", "Failed to inflate skeleton", e);
                    return;
                }
            }
            
            if (contentScrollView != null) {
                contentScrollView.setVisibility(View.INVISIBLE);
            }
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
            if (skeletonView != null) {
                skeletonView.setVisibility(View.GONE);
                stopSkeletonAnimation(skeletonView);
            }
            if (contentScrollView != null) {
                contentScrollView.setVisibility(View.VISIBLE);
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
