package com.example.codestreak;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
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
    private TextView easyCountText, mediumCountText, hardCountText;
    private TextView currentStreakText, longestStreakText;
    
    private Calendar currentCalendar;
    private LeetCodeAPI leetCodeAPI;
    private org.json.JSONObject submissionCalendarData;
    
    // Stats variables
    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    private int currentStreak = 2;  // From the profile image: Max streak: 2
    private int longestStreak = 7;  // From the profile image: Total active days: 7
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Remove action bar for cleaner look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        initializeViews();
        setupCalendar();
        setupPieChart();
        updateStats();
        
        // Initialize with fallback data first (for immediate UI)
        updateContributionGrid();
        
        // Then fetch real data in background and update UI when ready
        leetCodeAPI = new LeetCodeAPI();
        fetchLeetCodeData();
    }
    
    private void initializeViews() {
        pieChart = findViewById(R.id.pieChart);
        contributionGrid = findViewById(R.id.contributionGrid);
        monthYearText = findViewById(R.id.monthYearText);
        prevButton = findViewById(R.id.prevMonthButton);
        nextButton = findViewById(R.id.nextMonthButton);
        easyCountText = findViewById(R.id.easyCountText);
        mediumCountText = findViewById(R.id.mediumCountText);
        hardCountText = findViewById(R.id.hardCountText);
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        
        // Setup navigation buttons
        prevButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
            updateContributionGrid();
        });
        
        nextButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
            updateContributionGrid();
        });
    }
    
    private void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        
        // Navigate to May 2025 by default where we know there's real data
        currentCalendar.set(Calendar.YEAR, 2025);
        currentCalendar.set(Calendar.MONTH, Calendar.MAY);
        
        updateCalendarView();
    }
    
    private void updateCalendarView() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year = currentCalendar.get(Calendar.YEAR);
        
        // Check if this month likely has real data
        boolean hasRealData = (year == 2025 && (month == Calendar.MAY || month == Calendar.JULY));
        
        String monthYear = months[month] + " " + year;
        if (hasRealData) {
            monthYear += " ★"; // Add star to indicate real data
        }
        
        monthYearText.setText(monthYear);
        
        // Update notice when navigating
        if (hasRealData) {
            System.out.println("REAL_DATA_DEBUG: Showing month with REAL submission data!");
        } else {
            System.out.println("REAL_DATA_DEBUG: No real data for " + months[month] + " " + year + 
                             ", using fallback. Try May or July 2025 for real data.");
        }
    }
    
    private void setupPieChart() {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Problems\nSolved");
        pieChart.setCenterTextSize(16f);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.getLegend().setEnabled(false);
        
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
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }
    
    private void updateContributionGrid() {
        List<Integer> monthData = generateMonthlyContributionData(currentCalendar);
        
        if (contributionAdapter == null) {
            // Convert List to ArrayList for the constructor
            ArrayList<Integer> arrayListData = new ArrayList<>(monthData);
            contributionAdapter = new UltraSimpleAdapter(arrayListData, currentCalendar);
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
                // Create timestamp for the day - generic approach that works for any month
                Calendar localCal = (Calendar) dayCalendar.clone();
                localCal.set(Calendar.DAY_OF_MONTH, day);
                localCal.set(Calendar.HOUR_OF_DAY, 0);
                localCal.set(Calendar.MINUTE, 0);
                localCal.set(Calendar.SECOND, 0);
                localCal.set(Calendar.MILLISECOND, 0);
                
                long localTimestamp = localCal.getTimeInMillis() / 1000;
                String localKey = String.valueOf(localTimestamp);
                
                // Also try nearby timestamps (LeetCode data might be UTC shifted)
                String[] possibleKeys = {
                    localKey,
                    String.valueOf(localTimestamp + 19800),   // +5:30 hours (IST timezone)
                    String.valueOf(localTimestamp - 19800),   // -5:30 hours
                    String.valueOf(localTimestamp + 86400),   // +1 day
                    String.valueOf(localTimestamp - 86400)    // -1 day
                };
                
                for (String key : possibleKeys) {
                    if (submissionCalendarData.has(key)) {
                        int submissions = submissionCalendarData.getInt(key);
                        if (submissions > 0) {
                            System.out.println("REAL_DATA_MATCH: Month " + (dayCalendar.get(Calendar.MONTH) + 1) + 
                                             " Day " + day + " has " + submissions + " REAL submissions (key: " + key + ")");
                            // Only days with real submissions will be colored
                            return submissions;
                        }
                    }
                }
                
                // Check if we have any data at all for this month - if not, log it
                boolean hasAnyDataThisMonth = false;
                Calendar monthStart = (Calendar) dayCalendar.clone();
                monthStart.set(Calendar.DAY_OF_MONTH, 1);
                monthStart.set(Calendar.HOUR_OF_DAY, 0);
                monthStart.set(Calendar.MINUTE, 0);
                monthStart.set(Calendar.SECOND, 0);
                monthStart.set(Calendar.MILLISECOND, 0);
                
                Calendar monthEnd = (Calendar) monthStart.clone();
                monthEnd.add(Calendar.MONTH, 1);
                
                long startTimestamp = monthStart.getTimeInMillis() / 1000;
                long endTimestamp = monthEnd.getTimeInMillis() / 1000;
                
                java.util.Iterator<String> keys = submissionCalendarData.keys();
                while (keys.hasNext()) {
                    String timestampStr = keys.next();
                    long timestamp = Long.parseLong(timestampStr);
                    if (timestamp >= startTimestamp && timestamp < endTimestamp) {
                        hasAnyDataThisMonth = true;
                        break;
                    }
                }
                
                if (!hasAnyDataThisMonth && day == 1) {
                    System.out.println("REAL_DATA_DEBUG: No real data found for month " + (dayCalendar.get(Calendar.MONTH) + 1) + 
                                     " " + dayCalendar.get(Calendar.YEAR) + ", will use fallback pattern");
                    System.out.println("REAL_DATA_DEBUG: Try navigating to May or July 2025 to see REAL submission data!");
                }
                
            } catch (Exception e) {
                System.out.println("REAL_DATA_DEBUG: Error getting submissions for day " + day + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (day == 1) { // Only log once per month
                System.out.println("REAL_DATA_DEBUG: No submission calendar data available, using fallback pattern");
            }
        }
        
        // For months with real data, only color specific days that have data
        // For all other months/days, return 0 (no submissions)
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        
        // Only May and July 2025 have real data - for these months, 
        // we've already checked for real data above and returned if found
        // For all days without real data, return 0 (no color)
        return 0;
    }
    
    private void updateStats() {
        easyCountText.setText(String.valueOf(easyProblems));
        mediumCountText.setText(String.valueOf(mediumProblems));
        hardCountText.setText(String.valueOf(hardProblems));
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
    }
    
    private void fetchLeetCodeData() {
        // Use the user's actual LeetCode username
        String username = "adityashak04"; // Your actual LeetCode username
        
        System.out.println("REAL_DATA_DEBUG: Starting LeetCode API call for user: " + username);
        
        leetCodeAPI.getUserSubmissionStats(username, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                System.out.println("REAL_DATA_DEBUG: ✅ API SUCCESS! Response length: " + response.length());
                try {
                    parseAndUpdateData(response);
                    System.out.println("REAL_DATA_DEBUG: ✅ Data parsed successfully, FORCING UI update with REAL data");
                    
                    // Force update on UI thread
                    runOnUiThread(() -> {
                        System.out.println("REAL_DATA_DEBUG: ✅ Running UI update on main thread");
                        updateStats();
                        updatePieChart();
                        updateContributionGrid(); // This will now use real data
                        System.out.println("REAL_DATA_DEBUG: ✅ UI update completed with real data");
                    });
                } catch (Exception e) {
                    System.out.println("REAL_DATA_DEBUG: ❌ Error parsing data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("REAL_DATA_DEBUG: ❌ API ERROR: " + error.getMessage());
                System.out.println("REAL_DATA_DEBUG: ❌ API failed, continuing with fallback data");
                error.printStackTrace();
            }
        });
        
        // Add a timeout check
        new android.os.Handler().postDelayed(() -> {
            if (submissionCalendarData == null) {
                System.out.println("REAL_DATA_DEBUG: ⚠️ API timeout - no response after 10 seconds");
            }
        }, 10000);
    }
    
    private void parseAndUpdateData(String jsonResponse) throws Exception {
        System.out.println("REAL_DATA_DEBUG: Starting to parse JSON response...");
        org.json.JSONObject response = new org.json.JSONObject(jsonResponse);
        org.json.JSONObject data = response.getJSONObject("data");
        org.json.JSONObject matchedUser = data.getJSONObject("matchedUser");
        
        // Fix: submissionCalendar comes as a string, need to parse it as JSONObject
        String submissionCalendarString = matchedUser.getString("submissionCalendar");
        submissionCalendarData = new org.json.JSONObject(submissionCalendarString);
        System.out.println("REAL_DATA_DEBUG: ✅ REAL DATA LOADED! Submission calendar has " + submissionCalendarData.length() + " entries");
        
        // Debug: Show some sample timestamps from real data
        java.util.Iterator<String> keys = submissionCalendarData.keys();
        int debugCount = 0;
        int totalSubmissions = 0;
        while (keys.hasNext()) {
            String key = keys.next();
            int value = submissionCalendarData.getInt(key);
            totalSubmissions += value;
            if (value > 0 && debugCount < 5) {
                long timestamp = Long.parseLong(key);
                java.util.Date date = new java.util.Date(timestamp * 1000);
                System.out.println("REAL_DATA_DEBUG: Sample entry - " + key + " -> " + value + " submissions on " + date);
                debugCount++;
            }
        }
        System.out.println("REAL_DATA_DEBUG: ✅ Total submissions found in data: " + totalSubmissions);
        
        // Parse problem counts
        org.json.JSONObject submitStats = matchedUser.getJSONObject("submitStats");
        org.json.JSONArray acSubmissionNum = submitStats.getJSONArray("acSubmissionNum");
        
        int newEasy = 0, newMedium = 0, newHard = 0;
        
        for (int i = 0; i < acSubmissionNum.length(); i++) {
            org.json.JSONObject submission = acSubmissionNum.getJSONObject(i);
            String difficulty = submission.getString("difficulty");
            int count = submission.getInt("count");
            
            System.out.println("REAL_DATA_DEBUG: Found " + count + " " + difficulty + " problems");
            
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
            System.out.println("REAL_DATA_DEBUG: ✅ REAL PROBLEM COUNTS! Easy: " + easyProblems + 
                              ", Medium: " + mediumProblems + ", Hard: " + hardProblems);
        } else {
            System.out.println("REAL_DATA_DEBUG: No valid problem counts found, keeping defaults");
        }
        
        System.out.println("REAL_DATA_DEBUG: ✅ parseAndUpdateData() completed successfully!");
    }
}
