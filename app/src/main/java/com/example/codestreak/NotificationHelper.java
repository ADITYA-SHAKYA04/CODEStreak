package com.example.codestreak;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    
    private static final String CHANNEL_ID = "daily_problem_channel";
    private static final String CHANNEL_NAME = "Daily Problem Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for daily coding problems";
    
    public static final int MORNING_NOTIFICATION_ID = 1001;
    public static final int EVENING_NOTIFICATION_ID = 1002;
    
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public static void showDailyProblemNotification(Context context, boolean isMorning) {
        // Create intent to open ProblemsActivity
        Intent intent = new Intent(context, ProblemsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = isMorning ? "🌅 Morning Code Challenge!" : "🌙 Evening Problem Time!";
        String content = isMorning ? 
            "Start your day with a coding problem! Keep your streak alive 🔥" :
            "End your day productively! Solve today's coding challenge 💻";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(content + "\n\nTap to browse problems and maintain your coding streak!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL);
        
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notificationId = isMorning ? MORNING_NOTIFICATION_ID : EVENING_NOTIFICATION_ID;
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    public static void showStreakReminder(Context context, int currentStreak) {
        Intent intent = new Intent(context, ModernMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String title = "🔥 Streak Alert!";
        String content = currentStreak > 0 ? 
            "Your " + currentStreak + " day streak is at risk! Solve a problem today." :
            "Start a new coding streak today! Every journey begins with a single step.";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL);
        
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(9999, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
