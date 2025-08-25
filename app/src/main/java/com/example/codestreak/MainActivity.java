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
        
        // Start with current month/year by default
        // We'll navigate to a month with data once the API data is loaded
        
        updateCalendarView();
    }
    
    private void updateCalendarView() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        int month = currentCalendar.get(Calendar.MONTH);
        int year = currentCalendar.get(Calendar.YEAR);
        
        // Special check for December 2019
        if (month == Calendar.DECEMBER && year == 2019) {
            System.out.println("REAL_DATA_DEBUG: Special check for December 2019!");
            // We'll do an extra specific check for December 2019 data
            checkDecember2019Data();
            
            // Show an alert dialog explaining API limitations
            showApiLimitationDialog(months[month], year);
        }
        
        // Dynamically check if current month has real data
        boolean hasRealData = hasSubmissionsForMonth(month, year);
        
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
                             ", using fallback.");
            
            // Show a toast with months that have data
            List<String> monthsWithData = findMonthsWithData();
            if (!monthsWithData.isEmpty() && submissionCalendarData != null) {
                // Check if user is viewing historical data that's not in the API
                if ((month == Calendar.DECEMBER && year == 2019) || 
                    (month == Calendar.JANUARY && year == 2020)) {
                    String message = "LeetCode API only returns recent data (2025). Historical data from " + 
                                    months[month] + " " + year + " isn't available via API.";
                    // Removed toast
                } else {
                    String message = "Try navigating to months with data: " + String.join(", ", monthsWithData);
                    // Removed toast
                }
            }
        }
    }
    
    /**
     * Show an explanatory dialog about API limitations
     */
    private void showApiLimitationDialog(String month, int year) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("LeetCode API Limitation");
        builder.setMessage("While your LeetCode profile shows activity in " + month + " " + year + 
                          ", the LeetCode API only returns recent data (2025).\n\n" +
                          "This is a common API limitation to reduce bandwidth and server load.\n\n" +
                          "To see your real submission data, please navigate to months in 2025.");
        
        builder.setPositiveButton("Go to Recent Data", (dialog, which) -> {
            // Navigate to a month with data
            List<String> monthsWithData = findMonthsWithData();
            if (!monthsWithData.isEmpty()) {
                // Parse the first month with data
                String firstMonthWithData = monthsWithData.get(0);
                String[] parts = firstMonthWithData.split(" ");
                if (parts.length == 2) {
                    String monthName = parts[0];
                    int dataYear = Integer.parseInt(parts[1]);
                    
                    // Find the month index
                    String[] monthNames = {"January", "February", "March", "April", "May", "June",
                                         "July", "August", "September", "October", "November", "December"};
                    for (int i = 0; i < monthNames.length; i++) {
                        if (monthNames[i].equals(monthName)) {
                            // Set the calendar to this month/year
                            currentCalendar.set(Calendar.YEAR, dataYear);
                            currentCalendar.set(Calendar.MONTH, i);
                            updateCalendarView();
                            updateContributionGrid();
                            break;
                        }
                    }
                }
            }
        });
        
        builder.setNegativeButton("Stay Here", null);
        builder.show();
    }
    
    /**
     * Special method to check December 2019 data
     * Note: LeetCode API appears to only return recent data (2025), not historical data
     */
    private void checkDecember2019Data() {
        if (submissionCalendarData == null) {
            return;
        }
        
        System.out.println("REAL_DATA_DEBUG: ===== CHECKING DECEMBER 2019 DATA =====");
        try {
            Calendar dec2019Start = Calendar.getInstance();
            dec2019Start.set(2019, Calendar.DECEMBER, 1, 0, 0, 0);
            dec2019Start.set(Calendar.MILLISECOND, 0);
            long startTimestamp = dec2019Start.getTimeInMillis() / 1000;
            
            Calendar dec2019End = Calendar.getInstance();
            dec2019End.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
            dec2019End.set(Calendar.MILLISECOND, 0);
            long endTimestamp = dec2019End.getTimeInMillis() / 1000;
            
            System.out.println("REAL_DATA_DEBUG: December 2019 timestamp range: " + startTimestamp + " to " + endTimestamp);
            
            // Print earliest and latest timestamps in data for comparison
            long earliestTimestamp = Long.MAX_VALUE;
            long latestTimestamp = 0;
            
            java.util.Iterator<String> allKeys = submissionCalendarData.keys();
            while (allKeys.hasNext()) {
                String key = allKeys.next();
                long timestamp = Long.parseLong(key);
                if (timestamp < earliestTimestamp) {
                    earliestTimestamp = timestamp;
                }
                if (timestamp > latestTimestamp) {
                    latestTimestamp = timestamp;
                }
            }
            
            Date earliestDate = new Date(earliestTimestamp * 1000);
            Date latestDate = new Date(latestTimestamp * 1000);
            
            System.out.println("REAL_DATA_DEBUG: API data ranges from " + earliestDate + " to " + latestDate);
            System.out.println("REAL_DATA_DEBUG: Note: LeetCode API appears to only return recent data, not historical data from 2019");
            
            // Check all submission timestamps
            java.util.Iterator<String> keys = submissionCalendarData.keys();
            boolean foundAny2019Data = false;
            while (keys.hasNext()) {
                String key = keys.next();
                long timestamp = Long.parseLong(key);
                int value = submissionCalendarData.getInt(key);
                
                if (value > 0) {
                    Date date = new Date(timestamp * 1000);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    int keyYear = cal.get(Calendar.YEAR);
                    
                    if (keyYear < 2025) {  // Look for any data before 2025
                        System.out.println("REAL_DATA_DEBUG: Pre-2025 submission found! " +
                                         "Key=" + key + ", Value=" + value + ", Date=" + date);
                        foundAny2019Data = true;
                    }
                }
            }
            
            if (!foundAny2019Data) {
                System.out.println("REAL_DATA_DEBUG: No historical data found in API response. " + 
                                  "The LeetCode API only returns recent activity (2025), not historical data from 2019/2020.");
            }
            
            System.out.println("REAL_DATA_DEBUG: ===== END DECEMBER 2019 CHECK =====");
        } catch (Exception e) {
            System.out.println("REAL_DATA_DEBUG: Error in checkDecember2019Data: " + e.getMessage());
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
                
                // For December 2019 specific debug
                if (dayCalendar.get(Calendar.MONTH) == Calendar.DECEMBER && 
                    dayCalendar.get(Calendar.YEAR) == 2019 && day == 1) {
                    System.out.println("REAL_DATA_DEBUG: Looking for Dec 2019 data with timestamp base: " + localTimestamp);
                }
                
                // Also try with more timezone variations (LeetCode data might be in different timezone)
                String[] possibleKeys = {
                    localKey,
                    String.valueOf(localTimestamp + 19800),   // +5:30 hours (IST timezone)
                    String.valueOf(localTimestamp - 19800),   // -5:30 hours
                    String.valueOf(localTimestamp + 86400),   // +1 day
                    String.valueOf(localTimestamp - 86400),   // -1 day
                    String.valueOf(localTimestamp + 43200),   // +12 hours
                    String.valueOf(localTimestamp - 43200),   // -12 hours
                    String.valueOf(localTimestamp + 64800),   // +18 hours
                    String.valueOf(localTimestamp - 64800)    // -18 hours
                };
                
                for (String key : possibleKeys) {
                    if (submissionCalendarData.has(key)) {
                        int submissions = submissionCalendarData.getInt(key);
                        if (submissions > 0) {
                            Date keyDate = new Date(Long.parseLong(key) * 1000);
                            System.out.println("REAL_DATA_MATCH: Month " + (dayCalendar.get(Calendar.MONTH) + 1) + 
                                             " Day " + day + " Year " + dayCalendar.get(Calendar.YEAR) + 
                                             " has " + submissions + " REAL submissions (key: " + key + 
                                             ", date: " + keyDate + ")");
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
                    
                    // Dynamically find which months have data
                    List<String> monthsWithData = findMonthsWithData();
                    if (!monthsWithData.isEmpty()) {
                        System.out.println("REAL_DATA_DEBUG: Try navigating to " + String.join(" or ", monthsWithData) + " to see REAL submission data!");
                    } else {
                        System.out.println("REAL_DATA_DEBUG: No months with submission data found");
                    }
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
        
        // We've already checked for real data above and returned if found
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
    
    /**
     * Dynamically check if the given month has any submission data
     * 
     * @param month Month to check (0-11, where 0 is January)
     * @param year Year to check
     * @return true if there's at least one submission in this month
     */
    private boolean hasSubmissionsForMonth(int month, int year) {
        if (submissionCalendarData == null) {
            return false;
        }
        
        try {
            Calendar monthStart = Calendar.getInstance();
            monthStart.set(year, month, 1, 0, 0, 0);
            monthStart.set(Calendar.MILLISECOND, 0);
            
            Calendar monthEnd = (Calendar) monthStart.clone();
            monthEnd.add(Calendar.MONTH, 1);
            
            long startTimestamp = monthStart.getTimeInMillis() / 1000;
            long endTimestamp = monthEnd.getTimeInMillis() / 1000;
            
            System.out.println("REAL_DATA_DEBUG: Checking for submissions in " + 
                             (month + 1) + "/" + year + " timestamp range: " + 
                             startTimestamp + " to " + endTimestamp);
            
            java.util.Iterator<String> keys = submissionCalendarData.keys();
            while (keys.hasNext()) {
                String timestampStr = keys.next();
                long timestamp = Long.parseLong(timestampStr);
                int submissions = submissionCalendarData.getInt(timestampStr);
                
                // Check if timestamp is in the current month and has submissions
                if (timestamp >= startTimestamp && timestamp < endTimestamp && submissions > 0) {
                    System.out.println("REAL_DATA_DEBUG: Found submissions for " + (month + 1) + "/" + year + 
                                     " at timestamp " + timestamp + " with " + submissions + " submissions");
                    return true;
                }
            }
            
            // If we didn't find submissions with the exact timestamp range, try with timezone adjustments
            keys = submissionCalendarData.keys();
            while (keys.hasNext()) {
                String timestampStr = keys.next();
                long timestamp = Long.parseLong(timestampStr);
                int submissions = submissionCalendarData.getInt(timestampStr);
                
                // Try with a more flexible range (±24 hours on either end)
                if (timestamp >= startTimestamp - 86400 && timestamp < endTimestamp + 86400 && submissions > 0) {
                    Date date = new Date(timestamp * 1000);
                    System.out.println("REAL_DATA_DEBUG: Found nearby submissions for " + (month + 1) + "/" + year + 
                                     " at " + date + " with " + submissions + " submissions");
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("REAL_DATA_DEBUG: Error checking submissions for month: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Find all months that have submission data
     * 
     * @return List of month names that have submissions
     */
    private List<String> findMonthsWithData() {
        List<String> monthsWithData = new ArrayList<>();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};
                             
        if (submissionCalendarData == null) {
            return monthsWithData;
        }
        
        // Find earliest and latest timestamp in submission calendar
        long earliestTimestamp = Long.MAX_VALUE;
        long latestTimestamp = 0;
        
        try {
            // Print all timestamp keys for debugging
            System.out.println("REAL_DATA_DEBUG: ===== ALL TIMESTAMP KEYS =====");
            java.util.Iterator<String> debugKeys = submissionCalendarData.keys();
            int keyCount = 0;
            while (debugKeys.hasNext() && keyCount < 30) {  // Limit to first 30 to avoid log spam
                String key = debugKeys.next();
                int count = submissionCalendarData.getInt(key);
                Date date = new Date(Long.parseLong(key) * 1000);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                int day = cal.get(Calendar.DAY_OF_MONTH);
                System.out.println(String.format("REAL_DATA_DEBUG: Key=%s, Count=%d, Date=%d-%02d-%02d", 
                                key, count, year, month, day));
                keyCount++;
            }
            System.out.println("REAL_DATA_DEBUG: ===== END TIMESTAMP KEYS =====");
            
            // Proceed with finding months with data
            java.util.Iterator<String> keys = submissionCalendarData.keys();
            while (keys.hasNext()) {
                String timestampStr = keys.next();
                long timestamp = Long.parseLong(timestampStr);
                int count = submissionCalendarData.getInt(timestampStr);
                
                if (count > 0) {
                    earliestTimestamp = Math.min(earliestTimestamp, timestamp);
                    latestTimestamp = Math.max(latestTimestamp, timestamp);
                }
            }
            
            // If no data, return empty list
            if (earliestTimestamp == Long.MAX_VALUE || latestTimestamp == 0) {
                return monthsWithData;
            }
            
            // Create Date objects for earliest and latest
            Date earliest = new Date(earliestTimestamp * 1000);
            Date latest = new Date(latestTimestamp * 1000);
            
            // Create calendars
            Calendar earliestCal = Calendar.getInstance();
            earliestCal.setTime(earliest);
            int startYear = earliestCal.get(Calendar.YEAR);
            
            Calendar latestCal = Calendar.getInstance();
            latestCal.setTime(latest);
            int endYear = latestCal.get(Calendar.YEAR);
            
            System.out.println("REAL_DATA_DEBUG: Data spans from " + startYear + " to " + endYear);
            
            // Check each month in each year from earliest to latest
            for (int year = startYear; year <= endYear; year++) {
                for (int month = 0; month < 12; month++) {
                    if (hasSubmissionsForMonth(month, year)) {
                        monthsWithData.add(monthNames[month] + " " + year);
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("REAL_DATA_DEBUG: Error finding months with data: " + e.getMessage());
        }
        
        return monthsWithData;
    }
    
    private void fetchLeetCodeData() {
        // Use the user's actual LeetCode username
        String username = "adityashak04"; // Your actual LeetCode username
        
        System.out.println("REAL_DATA_DEBUG: Starting LeetCode API call for user: " + username);
        System.out.println("REAL_DATA_DEBUG: INITIAL MONTH/YEAR: " + currentCalendar.get(Calendar.MONTH) + "/" + 
                           currentCalendar.get(Calendar.YEAR));
        
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
        
        // Check for December 2019 data specifically
        System.out.println("REAL_DATA_DEBUG: Checking for December 2019 data...");
        Calendar dec2019 = Calendar.getInstance();
        dec2019.set(2019, Calendar.DECEMBER, 1, 0, 0, 0);
        dec2019.set(Calendar.MILLISECOND, 0);
        long dec2019Start = dec2019.getTimeInMillis() / 1000;
        
        Calendar jan2020 = Calendar.getInstance();
        jan2020.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        jan2020.set(Calendar.MILLISECOND, 0);
        long dec2019End = jan2020.getTimeInMillis() / 1000;
        
        System.out.println("REAL_DATA_DEBUG: December 2019 timestamp range: " + dec2019Start + " to " + dec2019End);
        
        // Debug: Show all timestamps from real data
        java.util.Iterator<String> keys = submissionCalendarData.keys();
        int debugCount = 0;
        int totalSubmissions = 0;
        int december2019Count = 0;
        long earliestTimestamp = Long.MAX_VALUE;
        long latestTimestamp = 0;
        
        while (keys.hasNext()) {
            String key = keys.next();
            int value = submissionCalendarData.getInt(key);
            totalSubmissions += value;
            
            long timestamp = Long.parseLong(key);
            if (timestamp < earliestTimestamp) {
                earliestTimestamp = timestamp;
            }
            if (timestamp > latestTimestamp) {
                latestTimestamp = timestamp;
            }
            
            java.util.Date date = new java.util.Date(timestamp * 1000);
            
            // Check if timestamp is in December 2019
            if (timestamp >= dec2019Start && timestamp < dec2019End) {
                december2019Count += value;
                System.out.println("REAL_DATA_DEBUG: DECEMBER 2019 DATA FOUND! - " + key + " -> " + 
                                  value + " submissions on " + date);
            }
            
            // Show first 10 entries
            if (debugCount < 10) {
                System.out.println("REAL_DATA_DEBUG: Entry " + debugCount + " - " + key + " -> " + 
                                  value + " submissions on " + date);
                debugCount++;
            }
        }
        
        // Show data range summary
        if (earliestTimestamp != Long.MAX_VALUE) {
            Date earliestDate = new Date(earliestTimestamp * 1000);
            Date latestDate = new Date(latestTimestamp * 1000);
            Calendar earliestCal = Calendar.getInstance();
            earliestCal.setTime(earliestDate);
            Calendar latestCal = Calendar.getInstance();
            latestCal.setTime(latestDate);
            
            System.out.println("REAL_DATA_DEBUG: ✅ API data spans from " + 
                              earliestCal.get(Calendar.MONTH) + "/" + earliestCal.get(Calendar.YEAR) + 
                              " to " + latestCal.get(Calendar.MONTH) + "/" + latestCal.get(Calendar.YEAR));
            
            if (earliestCal.get(Calendar.YEAR) > 2020) {
                System.out.println("REAL_DATA_DEBUG: NOTE: LeetCode API is only providing recent data (" + 
                                  earliestCal.get(Calendar.YEAR) + " onwards), not historical data from 2019-2020.");
                System.out.println("REAL_DATA_DEBUG: This is normal behavior for many APIs to limit bandwidth.");
            }
        }
        
        System.out.println("REAL_DATA_DEBUG: ✅ Total submissions found in data: " + totalSubmissions);
        System.out.println("REAL_DATA_DEBUG: ✅ December 2019 submissions: " + december2019Count);
        
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
        
        // Navigate to first month with data if possible
        runOnUiThread(() -> {
            // Find months with data
            List<String> monthsWithData = findMonthsWithData();
            if (!monthsWithData.isEmpty()) {
                String firstMonthWithData = monthsWithData.get(0);
                System.out.println("REAL_DATA_DEBUG: Navigating to first month with data: " + firstMonthWithData);
                
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
                        // Set the calendar to this month/year
                        currentCalendar.set(Calendar.YEAR, year);
                        currentCalendar.set(Calendar.MONTH, monthIndex);
                        
                        // Update UI
                        updateCalendarView();
                        updateContributionGrid();
                    }
                }
            }
        });
    }
}
