package com.example.codestreak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private PieChart pieChart;
    private RecyclerView contributionGrid;
    private UltraSimpleAdapter contributionAdapter;
    private TextView monthYearText;
    private ImageButton prevButton, nextButton;
    private TextView easyCountTableText, mediumCountTableText, hardCountTableText, totalCountText;
    private TextView currentStreakText, longestStreakText;
    
    // Loading UI components
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout loadingOverlay;
    private ScrollView skeletonLayout;
    private LinearLayout mainContentLayout;
    private ProgressBar loadingProgressBar;
    
    private Calendar currentCalendar;
    private LeetCodeAPI leetCodeAPI;
    
    // Popup for pie chart segments
    private PopupWindow pieChartPopup;
    private PopupWindow calendarDayPopup;
    private org.json.JSONObject submissionCalendarData;
    
    // Add caching for performance
    private java.util.Set<String> monthsWithDataCache = null;
    
    // Stats variables
    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    private int currentStreak = 2;
    private int longestStreak = 7;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        
        // Get username from SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String username = sharedPref.getString("username", null);
        
        if (username == null) {
            // Try the new preferences format
            SharedPreferences newPref = getSharedPreferences("CodeStreakPrefs", Context.MODE_PRIVATE);
            username = newPref.getString("leetcode_username", "adityashak04");
            if (username == null) {
                // If no username is stored, redirect to login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }
        
        // Set username in action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("CODEStreak - " + username);
        }
        
        initializeViews();
        setupLoadingUI();
        setupCalendar();
        setupPieChart();
        updateStats();
        
        // Show loading state initially
        showLoadingState();
        
        // Initialize LeetCode API
        leetCodeAPI = new LeetCodeAPI();
        
        // Fetch data after a brief delay to show loading state
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLeetCodeData();
            }
        }, 100);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_change_username) {
            // Clear the stored username and restart login activity
            SharedPreferences sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove("username");
            editor.apply();
            
            // Also clear new format
            SharedPreferences newPref = getSharedPreferences("CodeStreakPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor newEditor = newPref.edit();
            newEditor.remove("leetcode_username");
            newEditor.apply();
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("change_username", true);
            startActivity(intent);
            finish();
            return true;
        } else if (id == R.id.action_logout) {
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
            startActivity(intent);
            finish();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void initializeViews() {
        // Main UI components
        pieChart = findViewById(R.id.pieChart);
        contributionGrid = findViewById(R.id.contributionGrid);
        monthYearText = findViewById(R.id.monthYearText);
        prevButton = findViewById(R.id.prevMonthButton);
        nextButton = findViewById(R.id.nextMonthButton);
        easyCountTableText = findViewById(R.id.easyCountTableText);
        mediumCountTableText = findViewById(R.id.mediumCountTableText);
        hardCountTableText = findViewById(R.id.hardCountTableText);
        totalCountText = findViewById(R.id.totalCountText);
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        
        // Loading UI components
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        skeletonLayout = findViewById(R.id.skeletonLayout);
        mainContentLayout = findViewById(R.id.mainContentLayout);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        
        // Setup navigation buttons
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, -1);
                updateCalendarView();
                updateContributionGrid();
            }
        });
        
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCalendar.add(Calendar.MONTH, 1);
                updateCalendarView();
                updateContributionGrid();
            }
        });
    }
    
    private void setupLoadingUI() {
        // Configure pull-to-refresh
        swipeRefreshLayout.setColorSchemeColors(
            Color.parseColor("#FFA116"), // LeetCode orange
            Color.parseColor("#00B8A3"), // Easy green
            Color.parseColor("#FFC01E")  // Medium yellow
        );
        
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });
    }
    
    private void showLoadingState() {
        loadingOverlay.setVisibility(View.VISIBLE);
        skeletonLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void showSkeletonState() {
        loadingOverlay.setVisibility(View.GONE);
        skeletonLayout.setVisibility(View.VISIBLE);
        mainContentLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void showContentState() {
        loadingOverlay.setVisibility(View.GONE);
        skeletonLayout.setVisibility(View.GONE);
        mainContentLayout.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setRefreshing(false);
    }
    
    private void refreshData() {
        // Clear existing data and cache
        submissionCalendarData = null;
        monthsWithDataCache = null;
        
        // Show skeleton while refreshing
        showSkeletonState();
        
        // Refresh data after a brief delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLeetCodeData();
            }
        }, 300);
    }
    
    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        updateCalendarView();
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
    }
    
    private void setupPieChart() {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT); // Make center transparent
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        int totalProblems = easyProblems + mediumProblems + hardProblems;
        pieChart.setCenterText("Total\n" + totalProblems);
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(getResources().getColor(R.color.leetcode_text_primary, getTheme())); // Use theme-aware color
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(false);
        
        // Enable animations
        pieChart.setDrawSlicesUnderHole(false);
        pieChart.setTouchEnabled(true);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        
        // Add click listener for showing percentage
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
                // Temporarily disabled to allow segments to show properly
                // The popup dismiss listener will handle cleanup
            }
        });
        
        // Update chart data
        updatePieChart();
    }
    
    private void updatePieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(easyProblems, "Easy"));
        entries.add(new PieEntry(mediumProblems, "Medium"));
        entries.add(new PieEntry(hardProblems, "Hard"));
        
        PieDataSet dataSet = new PieDataSet(entries, "Problems");
        
        // LeetCode colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#00B8A3")); // Easy - green
        colors.add(Color.parseColor("#FFC01E")); // Medium - yellow
        colors.add(Color.parseColor("#FF375F")); // Hard - red
        dataSet.setColors(colors);
        
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        // Enable selection highlighting
        dataSet.setSelectionShift(5f); // Shift selected slice outward
        dataSet.setHighlightEnabled(true);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        
        // Update center text with current total
        int totalProblems = easyProblems + mediumProblems + hardProblems;
        pieChart.setCenterText("Total\n" + totalProblems);
        
        // Add smooth animations
        pieChart.animateXY(1200, 1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuart);
        
        pieChart.invalidate();
    }
    
    private void updateContributionGrid() {
        List<Integer> monthData = generateMonthlyContributionData(currentCalendar);
        
        if (contributionAdapter == null) {
            // Convert List to ArrayList for the constructor
            ArrayList<Integer> arrayListData = new ArrayList<>(monthData);
            contributionAdapter = new UltraSimpleAdapter(arrayListData, currentCalendar);
            
            // Set up click listener for calendar days
            contributionAdapter.setOnDayClickListener(new UltraSimpleAdapter.OnDayClickListener() {
                @Override
                public void onDayClicked(int dayNumber, int problemCount, View clickedView) {
                    showCalendarDayPopup(dayNumber, problemCount, clickedView);
                }
            });
            
            contributionGrid.setLayoutManager(new GridLayoutManager(this, 7));
            contributionGrid.setAdapter(contributionAdapter);
        } else {
            contributionAdapter.updateData(monthData);
        }
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
    
    private void updateStats() {
        
        // Update the table text views as well
        easyCountTableText.setText(String.valueOf(easyProblems));
        mediumCountTableText.setText(String.valueOf(mediumProblems));
        hardCountTableText.setText(String.valueOf(hardProblems));
        totalCountText.setText(String.valueOf(easyProblems + mediumProblems + hardProblems));
        
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
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
                    Date date = new Date(timestamp * 1000);
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
    
    /**
     * Find all months that have submission data
     * Uses cached data for better performance
     */
    private List<String> findMonthsWithData() {
        if (submissionCalendarData == null) {
            return new ArrayList<>();
        }
        
        // Use cached data if available
        if (monthsWithDataCache == null) {
            buildMonthsWithDataCache();
        }
        
        // Convert set to sorted list
        List<String> monthsWithData = new ArrayList<>(monthsWithDataCache);
        
        // Sort the months chronologically
        java.util.Collections.sort(monthsWithData, new java.util.Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                try {
                    String[] partsA = a.split(" ");
                    String[] partsB = b.split(" ");
                    int yearA = Integer.parseInt(partsA[1]);
                    int yearB = Integer.parseInt(partsB[1]);
                    
                    if (yearA != yearB) {
                        return Integer.compare(yearA, yearB);
                    }
                    
                    String[] monthNames = {"January", "February", "March", "April", "May", "June",
                                         "July", "August", "September", "October", "November", "December"};
                    int monthA = -1, monthB = -1;
                    for (int i = 0; i < monthNames.length; i++) {
                        if (monthNames[i].equals(partsA[0])) monthA = i;
                        if (monthNames[i].equals(partsB[0])) monthB = i;
                    }
                    
                    return Integer.compare(monthA, monthB);
                } catch (Exception e) {
                    return 0;
                }
            }
        });
        
        return monthsWithData;
    }
    
    private void fetchLeetCodeData() {
        // Get the user's LeetCode username from login
        String username = LoginActivity.getLeetCodeUsername(this);
        
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
                                    showContentState();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showContentState();
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
                        showContentState();
                        updateContributionGrid();
                    }
                });
                error.printStackTrace();
            }
        });
        
        // Add a timeout check - show content after 15 seconds regardless
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loadingOverlay.getVisibility() == View.VISIBLE || 
                    skeletonLayout.getVisibility() == View.VISIBLE) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showContentState();
                            updateContributionGrid();
                        }
                    });
                }
            }
        }, 15000);
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
        // Do this heavy work on background thread, then update UI
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        boolean currentMonthHasData = hasSubmissionsForMonth(currentMonth, currentYear);
        
        if (currentMonthHasData) {
            // Current month has data, stay here
            final int finalCurrentYear = currentYear;
            final int finalCurrentMonth = currentMonth;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentCalendar.set(Calendar.YEAR, finalCurrentYear);
                    currentCalendar.set(Calendar.MONTH, finalCurrentMonth);
                    updateCalendarView();
                    updateContributionGrid();
                }
            });
        } else {
            // Current month has no data, find first month with data
            List<String> monthsWithData = findMonthsWithData();
            if (!monthsWithData.isEmpty()) {
                String firstMonthWithData = monthsWithData.get(0);
                
                // Parse the month and year
                String[] parts = firstMonthWithData.split(" ");
                if (parts.length == 2) {
                    String monthName = parts[0];
                    int year = Integer.parseInt(parts[1]);
                    
                    // Convert month name to month index (0-11)
                    String[] monthNames = {"January", "February", "March", "April", "May", "June",
                                         "July", "August", "September", "October", "November", "December"};
                    int monthIndex = -1;
                    for (int i = 0; i < monthNames.length; i++) {
                        if (monthNames[i].equals(monthName)) {
                            monthIndex = i;
                            break;
                        }
                    }
                    
                    if (monthIndex != -1) {
                        final int finalYear = year;
                        final int finalMonthIndex = monthIndex;
                        // Update UI on main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Set the calendar to this month/year
                                currentCalendar.set(Calendar.YEAR, finalYear);
                                currentCalendar.set(Calendar.MONTH, finalMonthIndex);
                                
                                // Update UI
                                updateCalendarView();
                                updateContributionGrid();
                            }
                        });
                    }
                }
            }
        }
    }
    
    private float calculateSegmentAngle(PieEntry pieEntry, Highlight highlight) {
        // Get the total of all values to calculate segment angles
        float totalValue = easyProblems + mediumProblems + hardProblems;
        
        // Calculate cumulative angles for each segment
        float easyAngle = (easyProblems / totalValue) * 360f;
        float mediumAngle = (mediumProblems / totalValue) * 360f;
        float hardAngle = (hardProblems / totalValue) * 360f;
        
        String label = pieEntry.getLabel();
        float segmentAngle = 0f;
        
        // Calculate the middle angle of each segment
        if ("Easy".equals(label)) {
            segmentAngle = easyAngle / 2f; // Middle of Easy segment
        } else if ("Medium".equals(label)) {
            segmentAngle = easyAngle + (mediumAngle / 2f); // Middle of Medium segment
        } else if ("Hard".equals(label)) {
            segmentAngle = easyAngle + mediumAngle + (hardAngle / 2f); // Middle of Hard segment
        }
        
        // Adjust for pie chart rotation (typically starts from top)
        segmentAngle -= 90f; // MPAndroidChart starts from 3 o'clock, adjust to 12 o'clock
        
        return segmentAngle;
    }
    
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
        pieChartPopup.setAnimationStyle(R.style.PopupAnimation); // Use custom smooth animation
        
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
                            // Add a subtle animation when segment merges back
                            pieChart.animateY(200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
                        }
                    }, 200); // Slightly longer delay to ensure smooth transition
                }
            }
        });
        
        // Calculate position around the selected segment
        int[] location = new int[2];
        pieChart.getLocationOnScreen(location);
        
        // Get pie chart center
        int centerX = location[0] + pieChart.getWidth() / 2;
        int centerY = location[1] + pieChart.getHeight() / 2;
        
        // Calculate segment position based on highlight
        float angle = calculateSegmentAngle(pieEntry, highlight);
        int radius = Math.min(pieChart.getWidth(), pieChart.getHeight()) / 3; // Distance from center
        
        // Convert angle to radians and calculate position
        double angleRad = Math.toRadians(angle);
        int segmentX = centerX + (int) (radius * Math.cos(angleRad));
        int segmentY = centerY + (int) (radius * Math.sin(angleRad));
        
        // Adjust popup position to be near the segment
        int popupX = segmentX - 75; // Offset to center popup
        int popupY = segmentY - 60; // Offset to show above segment
        
        // Ensure popup stays within screen bounds
        popupX = Math.max(20, Math.min(popupX, getResources().getDisplayMetrics().widthPixels - 200));
        popupY = Math.max(100, Math.min(popupY, getResources().getDisplayMetrics().heightPixels - 200));
        
        pieChartPopup.showAtLocation(pieChart, Gravity.NO_GRAVITY, popupX, popupY);
    }
    
    private void hidePieChartPopup() {
        if (pieChartPopup != null && pieChartPopup.isShowing()) {
            pieChartPopup.dismiss();
            pieChartPopup = null;
        }
        // Note: Highlight clearing is handled by the popup dismiss listener
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
        calendarDayPopup.setAnimationStyle(R.style.PopupAnimation); // Use custom smooth animation
        
        // Show popup above the clicked view
        int[] location = new int[2];
        clickedView.getLocationOnScreen(location);
        
        int popupX = location[0] + clickedView.getWidth() / 2 - 75; // Center horizontally
        int popupY = location[1] - 120; // Show above the clicked view
        
        calendarDayPopup.showAtLocation(clickedView, Gravity.NO_GRAVITY, popupX, popupY);
    }
    
    private void hideCalendarDayPopup() {
        if (calendarDayPopup != null && calendarDayPopup.isShowing()) {
            calendarDayPopup.dismiss();
            calendarDayPopup = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        hidePieChartPopup(); // Clean up pie chart popup
        hideCalendarDayPopup(); // Clean up calendar day popup
        super.onDestroy();
    }
}
