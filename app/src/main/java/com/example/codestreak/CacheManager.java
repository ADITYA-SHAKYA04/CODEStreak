package com.example.codestreak;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Centralized cache management for the CodeStreak app
 * Handles caching of API responses, user data, and app state
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_PREFS = "cache_preferences";
    private static final String USER_DATA_PREFS = "user_data_cache";
    private static final String API_CACHE_PREFS = "api_cache";
    
    // Cache expiration times (in milliseconds)
    private static final long LEETCODE_DATA_CACHE_DURATION = TimeUnit.HOURS.toMillis(6); // 6 hours
    private static final long DAILY_GOALS_CACHE_DURATION = TimeUnit.HOURS.toMillis(24); // 24 hours
    private static final long USER_STATS_CACHE_DURATION = TimeUnit.HOURS.toMillis(2); // 2 hours
    private static final long SUBMISSION_CACHE_DURATION = TimeUnit.HOURS.toMillis(1); // 1 hour
    
    private final Context context;
    private final Gson gson;
    private final SharedPreferences cachePrefs;
    private final SharedPreferences userDataPrefs;
    private final SharedPreferences apiCachePrefs;
    
    public CacheManager(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.cachePrefs = context.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        this.userDataPrefs = context.getSharedPreferences(USER_DATA_PREFS, Context.MODE_PRIVATE);
        this.apiCachePrefs = context.getSharedPreferences(API_CACHE_PREFS, Context.MODE_PRIVATE);
    }
    
    // Generic cache methods
    
    /**
     * Cache any object with expiration time
     */
    public <T> void cacheObject(String key, T object, long durationMs) {
        try {
            String json = gson.toJson(object);
            long expirationTime = System.currentTimeMillis() + durationMs;
            
            cachePrefs.edit()
                    .putString(key + "_data", json)
                    .putLong(key + "_expiration", expirationTime)
                    .apply();
            
            Log.d(TAG, "Cached object with key: " + key + ", expires in: " + (durationMs / 1000) + " seconds");
        } catch (Exception e) {
            Log.e(TAG, "Error caching object with key: " + key, e);
        }
    }
    
    /**
     * Retrieve cached object if not expired
     */
    public <T> T getCachedObject(String key, Class<T> clazz) {
        try {
            long expirationTime = cachePrefs.getLong(key + "_expiration", 0);
            
            if (System.currentTimeMillis() > expirationTime) {
                Log.d(TAG, "Cache expired for key: " + key);
                return null;
            }
            
            String json = cachePrefs.getString(key + "_data", null);
            if (json != null) {
                Log.d(TAG, "Cache hit for key: " + key);
                return gson.fromJson(json, clazz);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached object with key: " + key, e);
        }
        
        Log.d(TAG, "Cache miss for key: " + key);
        return null;
    }
    
    /**
     * Retrieve cached list if not expired
     */
    public <T> List<T> getCachedList(String key, Type typeToken) {
        try {
            long expirationTime = cachePrefs.getLong(key + "_expiration", 0);
            
            if (System.currentTimeMillis() > expirationTime) {
                Log.d(TAG, "Cache expired for key: " + key);
                return null;
            }
            
            String json = cachePrefs.getString(key + "_data", null);
            if (json != null) {
                Log.d(TAG, "Cache hit for key: " + key);
                return gson.fromJson(json, typeToken);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached list with key: " + key, e);
        }
        
        Log.d(TAG, "Cache miss for key: " + key);
        return null;
    }
    
    // Specific cache methods for different data types
    
    /**
     * Cache LeetCode user statistics
     */
    public void cacheLeetCodeStats(LeetCodeUserStats stats) {
        cacheObject("leetcode_stats", stats, LEETCODE_DATA_CACHE_DURATION);
    }
    
    /**
     * Get cached LeetCode statistics
     */
    public LeetCodeUserStats getCachedLeetCodeStats() {
        return getCachedObject("leetcode_stats", LeetCodeUserStats.class);
    }
    
    /**
     * Cache daily goals
     */
    public void cacheDailyGoals(List<?> goals) {
        Type listType = new TypeToken<List<?>>(){}.getType();
        cacheObject("daily_goals", goals, DAILY_GOALS_CACHE_DURATION);
    }
    
    /**
     * Get cached daily goals
     */
    public List<?> getCachedDailyGoals() {
        Type listType = new TypeToken<List<?>>(){}.getType();
        return getCachedList("daily_goals", listType);
    }
    
    /**
     * Cache user submission calendar
     */
    public void cacheSubmissionCalendar(List<SubmissionDay> submissions) {
        Type listType = new TypeToken<List<SubmissionDay>>(){}.getType();
        cacheObject("submission_calendar", submissions, SUBMISSION_CACHE_DURATION);
    }
    
    /**
     * Get cached submission calendar
     */
    public List<SubmissionDay> getCachedSubmissionCalendar() {
        Type listType = new TypeToken<List<SubmissionDay>>(){}.getType();
        return getCachedList("submission_calendar", listType);
    }
    
    /**
     * Cache user problems data
     */
    public void cacheUserProblems(List<?> problems) {
        Type listType = new TypeToken<List<?>>(){}.getType();
        cacheObject("user_problems", problems, USER_STATS_CACHE_DURATION);
    }
    
    /**
     * Get cached user problems
     */
    public List<?> getCachedUserProblems() {
        Type listType = new TypeToken<List<?>>(){}.getType();
        return getCachedList("user_problems", listType);
    }
    
    // User preferences caching
    
    /**
     * Cache user streak data
     */
    public void cacheStreakData(int currentStreak, int longestStreak) {
        userDataPrefs.edit()
                .putInt("current_streak", currentStreak)
                .putInt("longest_streak", longestStreak)
                .putLong("streak_last_updated", System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Get cached streak data
     */
    public int[] getCachedStreakData() {
        if (userDataPrefs.contains("current_streak")) {
            return new int[]{
                    userDataPrefs.getInt("current_streak", 0),
                    userDataPrefs.getInt("longest_streak", 0)
            };
        }
        return null;
    }
    
    /**
     * Cache problem counts by difficulty
     */
    public void cacheProblemCounts(int easy, int medium, int hard) {
        userDataPrefs.edit()
                .putInt("easy_count", easy)
                .putInt("medium_count", medium)
                .putInt("hard_count", hard)
                .putLong("problem_counts_last_updated", System.currentTimeMillis())
                .apply();
    }
    
    /**
     * Get cached problem counts
     */
    public int[] getCachedProblemCounts() {
        if (userDataPrefs.contains("easy_count")) {
            return new int[]{
                    userDataPrefs.getInt("easy_count", 0),
                    userDataPrefs.getInt("medium_count", 0),
                    userDataPrefs.getInt("hard_count", 0)
            };
        }
        return null;
    }
    
    // Cache invalidation methods
    
    /**
     * Clear specific cache entry
     */
    public void clearCache(String key) {
        cachePrefs.edit()
                .remove(key + "_data")
                .remove(key + "_expiration")
                .apply();
        Log.d(TAG, "Cleared cache for key: " + key);
    }
    
    /**
     * Clear all API caches
     */
    public void clearAllApiCaches() {
        apiCachePrefs.edit().clear().apply();
        cachePrefs.edit().clear().apply();
        Log.d(TAG, "Cleared all API caches");
    }
    
    /**
     * Clear user data cache
     */
    public void clearUserDataCache() {
        userDataPrefs.edit().clear().apply();
        Log.d(TAG, "Cleared user data cache");
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        clearAllApiCaches();
        clearUserDataCache();
        Log.d(TAG, "Cleared all caches");
    }
    
    /**
     * Check if a specific cache entry is valid
     */
    public boolean isCacheValid(String key) {
        long expirationTime = cachePrefs.getLong(key + "_expiration", 0);
        return System.currentTimeMillis() < expirationTime;
    }
    
    /**
     * Get cache size information
     */
    public String getCacheInfo() {
        int cacheEntries = cachePrefs.getAll().size() / 2; // Each entry has data + expiration
        int userDataEntries = userDataPrefs.getAll().size();
        int apiCacheEntries = apiCachePrefs.getAll().size();
        
        return String.format("Cache entries: %d, User data: %d, API cache: %d", 
                cacheEntries, userDataEntries, apiCacheEntries);
    }
    
    // Simple data classes for caching
    public static class LeetCodeUserStats {
        public int totalSolved;
        public int easySolved;
        public int mediumSolved;
        public int hardSolved;
        public int currentStreak;
        public int longestStreak;
        public String username;
        public long lastUpdated;
        
        public LeetCodeUserStats(int totalSolved, int easySolved, int mediumSolved, 
                                int hardSolved, int currentStreak, int longestStreak, String username) {
            this.totalSolved = totalSolved;
            this.easySolved = easySolved;
            this.mediumSolved = mediumSolved;
            this.hardSolved = hardSolved;
            this.currentStreak = currentStreak;
            this.longestStreak = longestStreak;
            this.username = username;
            this.lastUpdated = System.currentTimeMillis();
        }
    }
    
    public static class SubmissionDay {
        public String date;
        public int count;
        public String difficulty;
        
        public SubmissionDay(String date, int count, String difficulty) {
            this.date = date;
            this.count = count;
            this.difficulty = difficulty;
        }
    }
}
