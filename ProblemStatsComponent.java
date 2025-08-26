// Problem Distribution Stats Component extracted from MainActivity
public class ProblemStatsComponent {
    
    private TextView easyCountTableText, mediumCountTableText, hardCountTableText, totalCountText;
    private TextView currentStreakText, longestStreakText;
    
    // Stats variables
    private int easyProblems = 45;
    private int mediumProblems = 71;
    private int hardProblems = 11;
    private int currentStreak = 2;
    private int longestStreak = 7;
    
    public void updateStats() {
        // Update the table text views as well
        easyCountTableText.setText(String.valueOf(easyProblems));
        mediumCountTableText.setText(String.valueOf(mediumProblems));
        hardCountTableText.setText(String.valueOf(hardProblems));
        totalCountText.setText(String.valueOf(easyProblems + mediumProblems + hardProblems));
        
        currentStreakText.setText(String.valueOf(currentStreak));
        longestStreakText.setText(String.valueOf(longestStreak));
    }
    
    public void parseAndUpdateData(String jsonResponse) throws Exception {
        org.json.JSONObject response = new org.json.JSONObject(jsonResponse);
        org.json.JSONObject data = response.getJSONObject("data");
        org.json.JSONObject matchedUser = data.getJSONObject("matchedUser");
        
        // Parse submissionCalendar
        String submissionCalendarString = matchedUser.getString("submissionCalendar");
        org.json.JSONObject submissionCalendarData = new org.json.JSONObject(submissionCalendarString);
        
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
    }
    
    // Getters for the stats
    public int getEasyProblems() { return easyProblems; }
    public int getMediumProblems() { return mediumProblems; }
    public int getHardProblems() { return hardProblems; }
    public int getCurrentStreak() { return currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    
    // Setters for the stats
    public void setEasyProblems(int count) { this.easyProblems = count; }
    public void setMediumProblems(int count) { this.mediumProblems = count; }
    public void setHardProblems(int count) { this.hardProblems = count; }
    public void setCurrentStreak(int streak) { this.currentStreak = streak; }
    public void setLongestStreak(int streak) { this.longestStreak = streak; }
}
