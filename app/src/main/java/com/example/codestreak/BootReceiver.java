package com.example.codestreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("BootReceiver", "Received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            // Reschedule notifications after boot or app update
            NotificationHelper.createNotificationChannel(context);
            NotificationScheduler.scheduleNotifications(context);
            
            Log.d("BootReceiver", "Notifications rescheduled after boot/update");
        }
    }
}
