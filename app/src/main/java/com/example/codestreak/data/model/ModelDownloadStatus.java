package com.example.codestreak.data.model;

/**
 * Download status tracking - exactly matching Google AI Edge Gallery
 */
public class ModelDownloadStatus {
    public final ModelDownloadStatusType status;
    public final long totalBytes;
    public final long receivedBytes;
    public final long bytesPerSecond;
    public final long remainingMs;
    public final String errorMessage;
    
    public ModelDownloadStatus(ModelDownloadStatusType status) {
        this(status, 0L, 0L, 0L, 0L, null);
    }
    
    public ModelDownloadStatus(ModelDownloadStatusType status, long totalBytes, long receivedBytes, 
                              long bytesPerSecond, long remainingMs) {
        this(status, totalBytes, receivedBytes, bytesPerSecond, remainingMs, null);
    }
    
    public ModelDownloadStatus(ModelDownloadStatusType status, String errorMessage) {
        this(status, 0L, 0L, 0L, 0L, errorMessage);
    }
    
    public ModelDownloadStatus(ModelDownloadStatusType status, long totalBytes, long receivedBytes, 
                              long bytesPerSecond, long remainingMs, String errorMessage) {
        this.status = status;
        this.totalBytes = totalBytes;
        this.receivedBytes = receivedBytes;
        this.bytesPerSecond = bytesPerSecond;
        this.remainingMs = remainingMs;
        this.errorMessage = errorMessage;
    }
}
