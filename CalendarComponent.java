// Calendar Component extracted from MainActivity
public class CalendarComponent {
    
    private Calendar currentCalendar;
    private TextView monthYearText;
    private ImageButton prevButton, nextButton;
    private RecyclerView contributionGrid;
    private UltraSimpleAdapter contributionAdapter;
    private org.json.JSONObject submissionCalendarData;
    
    public void setupCalendar() {
        currentCalendar = Calendar.getInstance();
        updateCalendarView();
        
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
}
