package com.example.codestreak;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class NotificationScheduler {
    
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_MORNING_TIME = "morning_time";
    private static final String KEY_EVENING_TIME = "evening_time";
    
    // Default notification times
    private static final int DEFAULT_MORNING_HOUR = 9; // 9:00 AM
    private static final int DEFAULT_MORNING_MINUTE = 0;
    private static final int DEFAULT_EVENING_HOUR = 20; // 8:00 PM
    private static final int DEFAULT_EVENING_MINUTE = 0;
    
    public static void scheduleNotifications(Context context) {
        if (!areNotificationsEnabled(context)) {
            Log.d("NotificationScheduler", "Notifications are disabled");
            return;
        }
        
        scheduleMorningNotification(context);
        scheduleEveningNotification(context);
    }
    
    public static void scheduleNextNotifications(Context context) {
        if (!areNotificationsEnabled(context)) {
            return;
        }
        
        // Schedule for the next day
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, 1);
        
        scheduleMorningNotification(context);
        scheduleEveningNotification(context);
    }
    
    private static void scheduleMorningNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getMorningHour(context));
        calendar.set(Calendar.MINUTE, getMorningMinute(context));
        calendar.set(Calendar.SECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, DailyNotificationReceiver.class);
        intent.setAction(DailyNotificationReceiver.ACTION_MORNING_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }
        
        Log.d("NotificationScheduler", "Morning notification scheduled for: " + calendar.getTime());
    }
    
    private static void scheduleEveningNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, getEveningHour(context));
        calendar.set(Calendar.MINUTE, getEveningMinute(context));
        calendar.set(Calendar.SECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(context, DailyNotificationReceiver.class);
        intent.setAction(DailyNotificationReceiver.ACTION_EVENING_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }
        
        Log.d("NotificationScheduler", "Evening notification scheduled for: " + calendar.getTime());
    }
    
    public static void cancelNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Cancel morning notification
        Intent morningIntent = new Intent(context, DailyNotificationReceiver.class);
        morningIntent.setAction(DailyNotificationReceiver.ACTION_MORNING_NOTIFICATION);
        PendingIntent morningPendingIntent = PendingIntent.getBroadcast(
            context, 1001, morningIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(morningPendingIntent);
        
        // Cancel evening notification
        Intent eveningIntent = new Intent(context, DailyNotificationReceiver.class);
        eveningIntent.setAction(DailyNotificationReceiver.ACTION_EVENING_NOTIFICATION);
        PendingIntent eveningPendingIntent = PendingIntent.getBroadcast(
            context, 1002, eveningIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(eveningPendingIntent);
        
        Log.d("NotificationScheduler", "Notifications cancelled");
    }
    
    // Preference management methods
    public static void setNotificationsEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
        
        if (enabled) {
            scheduleNotifications(context);
        } else {
            cancelNotifications(context);
        }
    }
    
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true); // Default to enabled
    }
    
    public static void setMorningTime(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putInt(KEY_MORNING_TIME + "_hour", hour)
            .putInt(KEY_MORNING_TIME + "_minute", minute)
            .apply();
        
        if (areNotificationsEnabled(context)) {
            scheduleMorningNotification(context);
        }
    }
    
    public static void setEveningTime(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putInt(KEY_EVENING_TIME + "_hour", hour)
            .putInt(KEY_EVENING_TIME + "_minute", minute)
            .apply();
        
        if (areNotificationsEnabled(context)) {
            scheduleEveningNotification(context);
        }
    }
    
    public static int getMorningHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MORNING_TIME + "_hour", DEFAULT_MORNING_HOUR);
    }
    
    public static int getMorningMinute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MORNING_TIME + "_minute", DEFAULT_MORNING_MINUTE);
    }
    
    public static int getEveningHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_EVENING_TIME + "_hour", DEFAULT_EVENING_HOUR);
    }
    
    public static int getEveningMinute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_EVENING_TIME + "_minute", DEFAULT_EVENING_MINUTE);
    }
}
