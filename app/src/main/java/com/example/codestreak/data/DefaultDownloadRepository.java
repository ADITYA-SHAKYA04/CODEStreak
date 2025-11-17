package com.example.codestreak.data;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.example.codestreak.MainActivity;
import com.example.codestreak.R;
import com.example.codestreak.data.model.Model;
import com.example.codestreak.data.model.Task;
import com.example.codestreak.data.model.ModelDownloadStatus;
import com.example.codestreak.data.model.ModelDownloadStatusType;
import com.example.codestreak.worker.ModelDownloadWorker;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * DefaultDownloadRepository - Exact implementation following Google AI Edge Gallery patterns
 * Repository for managing model downloads using WorkManager
 */
public class DefaultDownloadRepository implements DownloadRepository {
    private static final String TAG = "DefaultDownloadRepository";
    private static final String MODEL_NAME_TAG = "model_name";
    private static final String TASK_ID_TAG = "task_id";
    
    private final Context context;
    private final WorkManager workManager;
    private final SharedPreferences downloadStartTimeSharedPreferences;
    
    public DefaultDownloadRepository(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
        this.downloadStartTimeSharedPreferences = context.getSharedPreferences(
            "download_start_time_ms", Context.MODE_PRIVATE);
    }
    
    @Override
    public void downloadModel(Task task, Model model, OnStatusUpdatedCallback onStatusUpdated) {
        downloadModel(task, model, null, onStatusUpdated);
    }
    
    @Override
    public void downloadModel(Task task, Model model, String accessToken, OnStatusUpdatedCallback onStatusUpdated) {
        // Create input data - exactly like Google's implementation
        Data.Builder builder = new Data.Builder();
        long totalBytes = model.totalBytes + model.extraDataFiles.stream()
            .mapToLong(file -> file.sizeInBytes)
            .sum();
        
        Data.Builder inputDataBuilder = builder
            .putString(ModelDownloadWorker.KEY_MODEL_NAME, model.name)
            .putString(ModelDownloadWorker.KEY_MODEL_URL, model.url)
            .putString(ModelDownloadWorker.KEY_MODEL_COMMIT_HASH, model.version)
            .putString(ModelDownloadWorker.KEY_MODEL_DOWNLOAD_MODEL_DIR, model.normalizedName)
            .putString(ModelDownloadWorker.KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
            .putBoolean(ModelDownloadWorker.KEY_MODEL_IS_ZIP, model.isZip)
            .putString(ModelDownloadWorker.KEY_MODEL_UNZIPPED_DIR, model.unzipDir)
            .putLong(ModelDownloadWorker.KEY_MODEL_TOTAL_BYTES, totalBytes);
        
        // Add extra data files if present
        if (!model.extraDataFiles.isEmpty()) {
            String extraUrls = model.extraDataFiles.stream()
                .map(file -> file.url)
                .collect(Collectors.joining(","));
            String extraFileNames = model.extraDataFiles.stream()
                .map(file -> file.downloadFileName)
                .collect(Collectors.joining(","));
            
            inputDataBuilder
                .putString(ModelDownloadWorker.KEY_MODEL_EXTRA_DATA_URLS, extraUrls)
                .putString(ModelDownloadWorker.KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES, extraFileNames);
        }
        
        // Add access token if available (from parameter or model)
        String tokenToUse = accessToken != null ? accessToken : model.accessToken;
        if (tokenToUse != null) {
            Log.d(TAG, "Using access token for authentication");
            inputDataBuilder.putString(ModelDownloadWorker.KEY_MODEL_DOWNLOAD_ACCESS_TOKEN, tokenToUse);
        }
        
        Data inputData = inputDataBuilder.build();
        
        // Create worker request - exactly like Google's implementation
        OneTimeWorkRequest downloadWorkRequest = new OneTimeWorkRequest.Builder(ModelDownloadWorker.class)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .addTag(MODEL_NAME_TAG + ":" + model.name)
            .addTag(TASK_ID_TAG + ":" + task.id)
            .build();
        
        UUID workerId = downloadWorkRequest.getId();
        
        // Record download start time
        downloadStartTimeSharedPreferences.edit()
            .putLong(model.name, System.currentTimeMillis())
            .apply();
        
        // Start the download
        workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.REPLACE, downloadWorkRequest);
        
        // Observe progress
        observeWorkerProgress(workerId, task, model, onStatusUpdated);
    }
    
    @Override
    public void cancelDownloadModel(Model model) {
        workManager.cancelAllWorkByTag(MODEL_NAME_TAG + ":" + model.name);
    }
    
    @Override
    public void cancelAll(OnCompleteCallback onComplete) {
        workManager.cancelAllWork()
            .getResult()
            .addListener(onComplete::onComplete, Executors.newSingleThreadExecutor());
    }
    
    @Override
    public void observeWorkerProgress(UUID workerId, Task task, Model model, OnStatusUpdatedCallback onStatusUpdated) {
        workManager.getWorkInfoByIdLiveData(workerId).observeForever(new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    switch (workInfo.getState()) {
                        case ENQUEUED:
                            onStatusUpdated.onStatusUpdated(model, 
                                new ModelDownloadStatus(ModelDownloadStatusType.NOT_DOWNLOADED));
                            break;
                            
                        case RUNNING:
                            // Extract progress data - exactly like Google's implementation
                            long receivedBytes = workInfo.getProgress().getLong(
                                ModelDownloadWorker.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L);
                            long downloadRate = workInfo.getProgress().getLong(
                                ModelDownloadWorker.KEY_MODEL_DOWNLOAD_RATE, 0L);
                            long remainingSeconds = workInfo.getProgress().getLong(
                                ModelDownloadWorker.KEY_MODEL_DOWNLOAD_REMAINING_MS, 0L);
                            boolean startUnzipping = workInfo.getProgress().getBoolean(
                                ModelDownloadWorker.KEY_MODEL_START_UNZIPPING, false);
                            
                            if (!startUnzipping) {
                                if (receivedBytes != 0L) {
                                    onStatusUpdated.onStatusUpdated(model, 
                                        new ModelDownloadStatus(
                                            ModelDownloadStatusType.IN_PROGRESS,
                                            model.totalBytes,
                                            receivedBytes,
                                            downloadRate,
                                            remainingSeconds
                                        ));
                                }
                            } else {
                                onStatusUpdated.onStatusUpdated(model, 
                                    new ModelDownloadStatus(ModelDownloadStatusType.UNZIPPING));
                            }
                            break;
                            
                        case SUCCEEDED:
                            Log.d(TAG, "Worker " + workerId + " success");
                            onStatusUpdated.onStatusUpdated(model, 
                                new ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED));
                            
                            sendNotification(
                                context.getString(R.string.app_name) + " - Download Complete",
                                "Successfully downloaded " + model.name,
                                task.id,
                                model.name
                            );
                            
                            // Calculate download duration
                            long startTime = downloadStartTimeSharedPreferences.getLong(model.name, 0L);
                            long duration = System.currentTimeMillis() - startTime;
                            Log.d(TAG, "Download completed in " + duration + "ms");
                            
                            // Clean up start time
                            downloadStartTimeSharedPreferences.edit().remove(model.name).apply();
                            break;
                            
                        case FAILED:
                        case CANCELLED:
                            ModelDownloadStatusType status = workInfo.getState() == WorkInfo.State.CANCELLED 
                                ? ModelDownloadStatusType.NOT_DOWNLOADED 
                                : ModelDownloadStatusType.FAILED;
                            
                            String errorMessage = workInfo.getOutputData().getString(
                                ModelDownloadWorker.KEY_MODEL_DOWNLOAD_ERROR_MESSAGE);
                            if (errorMessage == null) errorMessage = "";
                            
                            Log.d(TAG, "Worker " + workerId + " FAILED or CANCELLED: " + errorMessage);
                            
                            if (workInfo.getState() != WorkInfo.State.CANCELLED) {
                                sendNotification(
                                    context.getString(R.string.app_name) + " - Download Failed",
                                    "Failed to download " + model.name,
                                    "",
                                    ""
                                );
                            }
                            
                            onStatusUpdated.onStatusUpdated(model, 
                                new ModelDownloadStatus(status, errorMessage));
                            
                            // Calculate failed download duration
                            long startTime2 = downloadStartTimeSharedPreferences.getLong(model.name, 0L);
                            long duration2 = System.currentTimeMillis() - startTime2;
                            Log.d(TAG, "Download failed after " + duration2 + "ms");
                            
                            // Clean up start time
                            downloadStartTimeSharedPreferences.edit().remove(model.name).apply();
                            break;
                            
                        default:
                            // Do nothing for other states
                            break;
                    }
                }
            }
        });
    }
    
    private void sendNotification(String title, String text, String taskId, String modelName) {
        String channelId = "download_notification";
        String channelName = "CodeStreak download notification";
        
        // Create notification channel
        NotificationChannel channel = new NotificationChannel(
            channelId, 
            channelName, 
            NotificationManager.IMPORTANCE_HIGH
        );
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        
        // Create intent for deep link
        Intent intent = new Intent(Intent.ACTION_VIEW, 
            Uri.parse("com.example.codestreak://model/" + taskId + "/" + modelName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
        
        // Check notification permission before showing
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
            == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(1, builder.build());
        }
    }
}
