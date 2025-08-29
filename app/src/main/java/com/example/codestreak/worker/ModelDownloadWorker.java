package com.example.codestreak.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.CoroutineWorker;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import com.example.codestreak.MainActivity;
import com.example.codestreak.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * DownloadWorker - Exact implementation following Google AI Edge Gallery patterns
 * Handles model downloads with resume capability, progress tracking, and authentication
 */
public class ModelDownloadWorker extends androidx.work.Worker {
    private static final String TAG = "ModelDownloadWorker";
    private static final String FOREGROUND_NOTIFICATION_CHANNEL_ID = "model_download_channel_foreground";
    private static final String TMP_FILE_EXT = "tmp";
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    // Input data keys - exactly matching Google's implementation
    public static final String KEY_MODEL_URL = "model_url";
    public static final String KEY_MODEL_NAME = "model_name";
    public static final String KEY_MODEL_COMMIT_HASH = "model_commit_hash";
    public static final String KEY_MODEL_DOWNLOAD_FILE_NAME = "model_download_file_name";
    public static final String KEY_MODEL_DOWNLOAD_MODEL_DIR = "model_download_model_dir";
    public static final String KEY_MODEL_IS_ZIP = "model_is_zip";
    public static final String KEY_MODEL_UNZIPPED_DIR = "model_unzipped_dir";
    public static final String KEY_MODEL_EXTRA_DATA_URLS = "model_extra_data_urls";
    public static final String KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES = "model_extra_data_download_file_names";
    public static final String KEY_MODEL_TOTAL_BYTES = "model_total_bytes";
    public static final String KEY_MODEL_DOWNLOAD_ACCESS_TOKEN = "model_download_access_token";
    
    // Progress data keys
    public static final String KEY_MODEL_DOWNLOAD_RECEIVED_BYTES = "model_download_received_bytes";
    public static final String KEY_MODEL_DOWNLOAD_RATE = "model_download_rate";
    public static final String KEY_MODEL_DOWNLOAD_REMAINING_MS = "model_download_remaining_ms";
    public static final String KEY_MODEL_START_UNZIPPING = "model_start_unzipping";
    public static final String KEY_MODEL_DOWNLOAD_ERROR_MESSAGE = "model_download_error_message";
    
    private static boolean channelCreated = false;
    private final NotificationManager notificationManager;
    private final int notificationId;
    private final File externalFilesDir;
    
    public ModelDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.externalFilesDir = context.getExternalFilesDir(null);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationId = params.getId().hashCode();
        
        // Create notification channel - exactly like Google's implementation
        if (!channelCreated) {
            NotificationChannel channel = new NotificationChannel(
                FOREGROUND_NOTIFICATION_CHANNEL_ID,
                "Model Downloading",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifications for model downloading");
            notificationManager.createNotificationChannel(channel);
            channelCreated = true;
        }
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String fileUrl = getInputData().getString(KEY_MODEL_URL);
        String modelName = getInputData().getString(KEY_MODEL_NAME);
        if (modelName == null) modelName = "Model";
        
        String version = getInputData().getString(KEY_MODEL_COMMIT_HASH);
        String fileName = getInputData().getString(KEY_MODEL_DOWNLOAD_FILE_NAME);
        String modelDir = getInputData().getString(KEY_MODEL_DOWNLOAD_MODEL_DIR);
        boolean isZip = getInputData().getBoolean(KEY_MODEL_IS_ZIP, false);
        String unzippedDir = getInputData().getString(KEY_MODEL_UNZIPPED_DIR);
        String accessToken = getInputData().getString(KEY_MODEL_DOWNLOAD_ACCESS_TOKEN);
        long totalBytes = getInputData().getLong(KEY_MODEL_TOTAL_BYTES, 0L);
        
        // Parse extra data files
        List<UrlAndFileName> allFiles = new ArrayList<>();
        if (fileUrl != null && fileName != null) {
            allFiles.add(new UrlAndFileName(fileUrl, fileName));
        }
        
        String extraUrls = getInputData().getString(KEY_MODEL_EXTRA_DATA_URLS);
        String extraFileNames = getInputData().getString(KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES);
        if (extraUrls != null && extraFileNames != null) {
            String[] urls = extraUrls.split(",");
            String[] names = extraFileNames.split(",");
            for (int i = 0; i < Math.min(urls.length, names.length); i++) {
                allFiles.add(new UrlAndFileName(urls[i], names[i]));
            }
        }
        
        if (fileUrl == null || fileName == null || version == null || modelDir == null) {
            return Result.failure();
        }
        
        try {
            // Set foreground service immediately - exactly like Google's implementation
            setForegroundAsync(createForegroundInfo(0, modelName));
            
            Log.d(TAG, "About to download: " + allFiles.size() + " files");
            
            // Download files in sequence - exactly like Google
            long downloadedBytes = 0L;
            List<Long> bytesReadSizeBuffer = new ArrayList<>();
            List<Long> bytesReadLatencyBuffer = new ArrayList<>();
            
            for (UrlAndFileName file : allFiles) {
                downloadedBytes += downloadFile(file, version, modelDir, accessToken, 
                    downloadedBytes, totalBytes, bytesReadSizeBuffer, bytesReadLatencyBuffer, modelName);
            }
            
            // Unzip if needed - exactly like Google's implementation
            if (isZip && unzippedDir != null) {
                setProgressAsync(new Data.Builder()
                    .putBoolean(KEY_MODEL_START_UNZIPPING, true)
                    .build());
                
                unzipFile(fileName, version, modelDir, unzippedDir);
            }
            
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Download failed: " + e.getMessage());
            return Result.failure(new Data.Builder()
                .putString(KEY_MODEL_DOWNLOAD_ERROR_MESSAGE, e.getMessage())
                .build());
        }
    }
    
    private long downloadFile(UrlAndFileName file, String version, String modelDir, 
                             String accessToken, long previousDownloadedBytes, long totalBytes,
                             List<Long> bytesReadSizeBuffer, List<Long> bytesReadLatencyBuffer,
                             String modelName) throws Exception {
        
        try {
            URL url = new URL(file.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Add authentication - exactly like Google's implementation
            if (accessToken != null) {
                Log.d(TAG, "Using access token: " + accessToken.substring(0, Math.min(10, accessToken.length())) + "...");
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            }
        
        // Prepare output directory
        File outputDir = new File(externalFilesDir, modelDir + File.separator + version);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Check for partial download and resume - exactly like Google's implementation
        File outputTmpFile = new File(outputDir, file.fileName + "." + TMP_FILE_EXT);
        long outputFileBytes = outputTmpFile.length();
        long startByte = 0;
        
        if (outputFileBytes > 0) {
            Log.d(TAG, "File '" + outputTmpFile.getName() + "' partial size: " + outputFileBytes + ". Trying to resume download");
            connection.setRequestProperty("Range", "bytes=" + outputFileBytes + "-");
            startByte = outputFileBytes;
        }
        
        connection.connect();
        Log.d(TAG, "Response code: " + connection.getResponseCode());
        
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK || 
            connection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
            
            // Handle Content-Range header - exactly like Google's implementation
            String contentRange = connection.getHeaderField("Content-Range");
            if (contentRange != null) {
                // Parse Content-Range header: bytes start-end/total
                String[] rangeParts = contentRange.substring(contentRange.indexOf("bytes ") + 6).split("/");
                String[] byteRange = rangeParts[0].split("-");
                long actualStartByte = Long.parseLong(byteRange[0]);
                long endByte = Long.parseLong(byteRange[1]);
                Log.d(TAG, "Content-Range: " + contentRange + ". Start bytes: " + actualStartByte + ", end bytes: " + endByte);
            } else {
                Log.d(TAG, "Download starts from beginning.");
            }
        } else {
            throw new IOException("HTTP error code: " + connection.getResponseCode());
        }
        
        InputStream inputStream = connection.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(outputTmpFile, true); // append mode
        
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytesRead;
        long lastSetProgressTs = 0;
        long deltaBytes = 0L;
        long fileDownloadedBytes = startByte;
        
        // Download with progress tracking - exactly like Google's implementation
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            fileDownloadedBytes += bytesRead;
            deltaBytes += bytesRead;
            
            // Report progress every 200ms - exactly like Google
            long curTs = System.currentTimeMillis();
            if (curTs - lastSetProgressTs > 200) {
                // Calculate download rate - exactly like Google's implementation
                float bytesPerMs = 0f;
                if (lastSetProgressTs != 0L) {
                    if (bytesReadSizeBuffer.size() == 5) {
                        bytesReadSizeBuffer.remove(0);
                    }
                    bytesReadSizeBuffer.add(deltaBytes);
                    
                    if (bytesReadLatencyBuffer.size() == 5) {
                        bytesReadLatencyBuffer.remove(0);
                    }
                    bytesReadLatencyBuffer.add(curTs - lastSetProgressTs);
                    
                    long totalDeltaBytes = 0;
                    long totalLatency = 0;
                    for (Long bytes : bytesReadSizeBuffer) totalDeltaBytes += bytes;
                    for (Long latency : bytesReadLatencyBuffer) totalLatency += latency;
                    
                    if (totalLatency > 0) {
                        bytesPerMs = (float) totalDeltaBytes / totalLatency;
                    }
                }
                
                // Calculate remaining time
                float remainingMs = 0f;
                long currentTotalDownloaded = previousDownloadedBytes + fileDownloadedBytes;
                if (bytesPerMs > 0f && totalBytes > 0L) {
                    remainingMs = (totalBytes - currentTotalDownloaded) / bytesPerMs;
                }
                
                // Update progress - exactly like Google's implementation
                setProgressAsync(new Data.Builder()
                    .putLong(KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, currentTotalDownloaded)
                    .putLong(KEY_MODEL_DOWNLOAD_RATE, (long) (bytesPerMs * 1000))
                    .putLong(KEY_MODEL_DOWNLOAD_REMAINING_MS, (long) remainingMs)
                    .build());
                
                // Update foreground notification
                int progress = totalBytes > 0 ? (int) ((currentTotalDownloaded * 100) / totalBytes) : 0;
                setForegroundAsync(createForegroundInfo(progress, modelName));
                
                Log.d(TAG, "Downloaded bytes: " + currentTotalDownloaded);
                lastSetProgressTs = curTs;
                deltaBytes = 0L;
            }
        }
        
        outputStream.close();
        inputStream.close();
        connection.disconnect();
        
        // Rename tmp file to final name - exactly like Google's implementation
        String originalFilePath = outputTmpFile.getAbsolutePath().replace("." + TMP_FILE_EXT, "");
        File originalFile = new File(originalFilePath);
        if (originalFile.exists()) {
            originalFile.delete();
        }
        outputTmpFile.renameTo(originalFile);
        
        Log.d(TAG, "Download complete for: " + file.fileName);
        return fileDownloadedBytes - startByte; // Return only newly downloaded bytes
        
        } catch (Exception e) {
            Log.e(TAG, "Error downloading file: " + file.fileName + " - " + e.getMessage());
            throw e;
        }
    }
    
    private void unzipFile(String fileName, String version, String modelDir, String unzippedDir) throws Exception {
        File destDir = new File(externalFilesDir, modelDir + File.separator + version + File.separator + unzippedDir);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        String zipFilePath = externalFilesDir + File.separator + modelDir + File.separator + version + File.separator + fileName;
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilePath)));
        
        byte[] unzipBuffer = new byte[4096];
        ZipEntry zipEntry = zipIn.getNextEntry();
        
        while (zipEntry != null) {
            String filePath = destDir.getAbsolutePath() + File.separator + zipEntry.getName();
            
            if (!zipEntry.isDirectory()) {
                // Extract file
                FileOutputStream bos = new FileOutputStream(filePath);
                int len;
                while ((len = zipIn.read(unzipBuffer)) > 0) {
                    bos.write(unzipBuffer, 0, len);
                }
                bos.close();
            } else {
                // Create directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            
            zipIn.closeEntry();
            zipEntry = zipIn.getNextEntry();
        }
        
        zipIn.close();
        
        // Delete original zip file
        File zipFile = new File(zipFilePath);
        zipFile.delete();
    }
    
    private ForegroundInfo createForegroundInfo(int progress, String modelName) {
        String title = "Downloading model";
        if (modelName != null) {
            title = "Downloading \"" + modelName + "\"";
        }
        String content = "Downloading in progress: " + progress + "%";
        
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            getApplicationContext(),
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
            getApplicationContext(), FOREGROUND_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent);
        
        return new ForegroundInfo(
            notificationId,
            notification.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        );
    }
    
    // Helper class for URL and filename pairs
    private static class UrlAndFileName {
        final String url;
        final String fileName;
        
        UrlAndFileName(String url, String fileName) {
            this.url = url;
            this.fileName = fileName;
        }
        
        @Override
        public String toString() {
            return "UrlAndFileName{url='" + url + "', fileName='" + fileName + "'}";
        }
    }
}
