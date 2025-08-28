package com.example.codestreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DailyNotificationReceiver extends BroadcastReceiver {
    
    public static final String ACTION_MORNING_NOTIFICATION = "com.example.codestreak.MORNING_NOTIFICATION";
    public static final String ACTION_EVENING_NOTIFICATION = "com.example.codestreak.EVENING_NOTIFICATION";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("DailyNotificationReceiver", "Received action: " + action);
        
        if (ACTION_MORNING_NOTIFICATION.equals(action)) {
            NotificationHelper.showDailyProblemNotification(context, true);
            Log.d("DailyNotificationReceiver", "Morning notification sent");
        } else if (ACTION_EVENING_NOTIFICATION.equals(action)) {
            NotificationHelper.showDailyProblemNotification(context, false);
            Log.d("DailyNotificationReceiver", "Evening notification sent");
        }
        
        // Reschedule the next notifications
        NotificationScheduler.scheduleNextNotifications(context);
    }
}
