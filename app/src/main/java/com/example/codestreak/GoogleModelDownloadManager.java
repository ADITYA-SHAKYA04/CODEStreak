package com.example.codestreak;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Google AI Edge Gallery compatible download manager for chat AI models
 * Reuses Google's exact download architecture and patterns
 */
public class GoogleModelDownloadManager {
    private static final String TAG = "GoogleModelDownload";
    private static final String MODEL_NAME_TAG = "modelName";
    private static final String TASK_ID_TAG = "taskId";
    
    private final Context context;
    private final WorkManager workManager;
    private final SharedPreferences downloadStartTimePrefs;
    private final AISolutionHelper_backup aiHelper;
    
    // Google's exact notification configuration
    private static final String DOWNLOAD_NOTIFICATION_CHANNEL = "model_download_channel";
    private static boolean channelCreated = false;
    
    public GoogleModelDownloadManager(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
        this.downloadStartTimePrefs = context.getSharedPreferences("download_start_time_ms", Context.MODE_PRIVATE);
        this.aiHelper = new AISolutionHelper_backup(context);
        
        setupNotificationChannel();
    }
    
    private void setupNotificationChannel() {
        if (!channelCreated) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            NotificationChannel channel = new NotificationChannel(
                DOWNLOAD_NOTIFICATION_CHANNEL,
                "AI Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications for AI model downloading progress");
            notificationManager.createNotificationChannel(channel);
            channelCreated = true;
        }
    }
    
    /**
     * Download a chat AI model using Google's WorkManager architecture
     */
    public void downloadChatModel(
        GoogleChatModel model, 
        AISolutionHelper_backup.GoogleDownloadCallback callback
    ) {
        Log.d(TAG, "Starting download for model: " + model.getName());
        
        if (!(context instanceof android.app.Activity)) {
            Log.e(TAG, "Context is not an Activity, cannot proceed with download");
            callback.onError("Context must be an Activity to download models");
            return;
        }
        
        // Use the real download method from AISolutionHelper_backup
        android.app.Activity activity = (android.app.Activity) context;
        
        // First check if model already exists
        if (model.isDownloaded(context)) {
            Log.d(TAG, "Model already downloaded: " + model.getName());
            callback.onComplete(model.getModelPath(context));
            return;
        }
        
        // Use the actual WorkManager download from AISolutionHelper_backup
        callback.onProgress(0, "Initializing download for " + model.getDisplayName() + "...");
        
        // Use detectExistingGoogleModels first to check, then download if needed
        aiHelper.detectExistingGoogleModels(activity, new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                callback.onProgress(progress, status);
            }
            
            @Override
            public void onComplete(String modelPath) {
                Log.d(TAG, "Model found/downloaded successfully: " + modelPath);
                callback.onComplete(modelPath);
                sendSuccessNotification(model);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Download failed: " + error);
                // Try direct download with WorkManager as fallback
                Log.d(TAG, "Attempting WorkManager download as fallback...");
                aiHelper.downloadModelWithWorkManager(activity, new AISolutionHelper_backup.GoogleDownloadCallback() {
                    @Override
                    public void onProgress(int progress, String status) {
                        callback.onProgress(progress, status);
                    }
                    
                    @Override
                    public void onComplete(String modelPath) {
                        Log.d(TAG, "WorkManager download completed: " + modelPath);
                        callback.onComplete(modelPath);
                        sendSuccessNotification(model);
                    }
                    
                    @Override
                    public void onError(String finalError) {
                        Log.e(TAG, "WorkManager download also failed: " + finalError);
                        callback.onError(finalError);
                        sendFailureNotification(model);
                    }
                });
            }
        });
    }
    
    /**
     * Cancel model download (simplified)
     */
    public void cancelDownload(GoogleChatModel model) {
        Log.d(TAG, "Cancel download requested for: " + model.getName());
        // In full implementation: workManager.cancelAllWorkByTag(MODEL_NAME_TAG + ":" + model.getName());
    }
    
    /**
     * Cancel all downloads (simplified)
     */
    public void cancelAllDownloads(Runnable onComplete) {
        Log.d(TAG, "Cancel all downloads requested");
        // In full implementation: workManager.cancelAllWork().getResult().addListener(onComplete, Executors.newSingleThreadExecutor());
        if (onComplete != null) {
            onComplete.run();
        }
    }
    
    private void sendSuccessNotification(GoogleChatModel model) {
        String title = "Download Complete";
        String text = "\"" + model.getDisplayName() + "\" is ready for chat";
        sendNotification(title, text, createAppIntent());
    }
    
    private void sendFailureNotification(GoogleChatModel model) {
        String title = "Download Failed";
        String text = "Failed to download \"" + model.getDisplayName() + "\"";
        sendNotification(title, text, createAppIntent());
    }
    
    private void sendNotification(String title, String text, PendingIntent pendingIntent) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private PendingIntent createAppIntent() {
        Intent intent = new Intent(context, ModernMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        return PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
