// Pie Chart Component extracted from MainActivity
public class PieChartComponent {
    
    private PieChart pieChart;
    private PopupWindow pieChartPopup;
    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    
    public void setupPieChart() {
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
}
