package com.example.codestreak;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class LocalDataManager {
    private static final String PREF_NAME = "CodeStreakData";
    private static final String KEY_PROBLEMS_DATA = "problems_data";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_LONGEST_STREAK = "longest_streak";
    private static final String KEY_TOTAL_EASY = "total_easy";
    private static final String KEY_TOTAL_MEDIUM = "total_medium";
    private static final String KEY_TOTAL_HARD = "total_hard";
    
    private SharedPreferences preferences;
    private Gson gson;
    
    public LocalDataManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public void saveProblemSolvedData(List<ProblemSolvedData> dataList) {
        String json = gson.toJson(dataList);
        preferences.edit().putString(KEY_PROBLEMS_DATA, json).apply();
    }
    
    public List<ProblemSolvedData> getProblemSolvedData() {
        String json = preferences.getString(KEY_PROBLEMS_DATA, null);
        if (json != null) {
            Type type = new TypeToken<List<ProblemSolvedData>>(){}.getType();
            return gson.fromJson(json, type);
        }
        return generateSampleData();
    }
    
    public void saveStreakData(int currentStreak, int longestStreak) {
        preferences.edit()
                .putInt(KEY_CURRENT_STREAK, currentStreak)
                .putInt(KEY_LONGEST_STREAK, longestStreak)
                .apply();
    }
    
    public int getCurrentStreak() {
        return preferences.getInt(KEY_CURRENT_STREAK, 7);
    }
    
    public int getLongestStreak() {
        return preferences.getInt(KEY_LONGEST_STREAK, 24);
    }
    
    public void saveTotalProblems(int easy, int medium, int hard) {
        preferences.edit()
                .putInt(KEY_TOTAL_EASY, easy)
                .putInt(KEY_TOTAL_MEDIUM, medium)
                .putInt(KEY_TOTAL_HARD, hard)
                .apply();
    }
    
    public int getTotalEasy() {
        return preferences.getInt(KEY_TOTAL_EASY, 45);
    }
    
    public int getTotalMedium() {
        return preferences.getInt(KEY_TOTAL_MEDIUM, 32);
    }
    
    public int getTotalHard() {
        return preferences.getInt(KEY_TOTAL_HARD, 18);
    }
    
    private List<ProblemSolvedData> generateSampleData() {
        List<ProblemSolvedData> sampleData = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        
        // Generate data for the past 30 days
        for (int i = 29; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            
            // Random problem counts for demonstration
            int easy = (int) (Math.random() * 4);
            int medium = (int) (Math.random() * 3);
            int hard = (int) (Math.random() * 2);
            
            sampleData.add(new ProblemSolvedData(calendar.getTime(), easy, medium, hard));
        }
        
        return sampleData;
    }
}
