// Popup Management Component extracted from MainActivity
public class PopupComponent {
    
    private PopupWindow pieChartPopup;
    private PopupWindow calendarDayPopup;
    private Calendar currentCalendar;
    
    public void showCalendarDayPopup(int dayNumber, int problemCount, View clickedView) {
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
    
    public void hideCalendarDayPopup() {
        if (calendarDayPopup != null && calendarDayPopup.isShowing()) {
            calendarDayPopup.dismiss();
            calendarDayPopup = null;
        }
    }
    
    public void hidePieChartPopup() {
        if (pieChartPopup != null && pieChartPopup.isShowing()) {
            pieChartPopup.dismiss();
            pieChartPopup = null;
        }
    }
    
    public void hideAllPopups() {
        hidePieChartPopup();
        hideCalendarDayPopup();
    }
    
    public void cleanup() {
        hideAllPopups();
    }
}
