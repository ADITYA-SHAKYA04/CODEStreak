// Card Holder Component for streak display extracted from MainActivity
public class StreakCardHolderComponent {
    
    private TextView currentStreakText, longestStreakText;
    private int currentStreak = 2;
    private int longestStreak = 7;
    
    public void initializeCardViews() {
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        
        updateStreakCards();
    }
    
    public void updateStreakCards() {
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
        
        // Add animations for streak updates
        if (currentStreakText != null) {
            currentStreakText.setScaleX(1.2f);
            currentStreakText.setScaleY(1.2f);
            currentStreakText.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .start();
        }
        
        if (longestStreakText != null) {
            longestStreakText.setScaleX(1.2f);
            longestStreakText.setScaleY(1.2f);
            longestStreakText.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .start();
        }
    }
    
    // Getters
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    
    // Setters with animation
    public void setCurrentStreak(int streak) { 
        this.currentStreak = streak;
        updateStreakCards();
    }
    
    public void setLongestStreak(int streak) { 
        this.longestStreak = streak;
        updateStreakCards();
    }
    
    // Method to update both streaks at once
    public void updateStreaks(int currentStreak, int longestStreak) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        updateStreakCards();
    }
}
