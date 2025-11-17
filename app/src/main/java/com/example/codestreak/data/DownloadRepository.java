package com.example.codestreak.data;

import com.example.codestreak.data.model.Model;
import com.example.codestreak.data.model.Task;
import com.example.codestreak.data.model.ModelDownloadStatus;

import java.util.UUID;

/**
 * Repository interface for managing model downloads - exactly like Google AI Edge Gallery
 */
public interface DownloadRepository {
    
    /**
     * Downloads a model for a specific task
     * @param task The task this model belongs to
     * @param model The model to download
     * @param onStatusUpdated Callback for download status updates
     */
    void downloadModel(
        Task task,
        Model model,
        OnStatusUpdatedCallback onStatusUpdated
    );
    
    /**
     * Downloads a model for a specific task with authentication
     * @param task The task this model belongs to
     * @param model The model to download
     * @param accessToken HuggingFace access token for authentication
     * @param onStatusUpdated Callback for download status updates
     */
    void downloadModel(
        Task task,
        Model model,
        String accessToken,
        OnStatusUpdatedCallback onStatusUpdated
    );
    
    /**
     * Cancels download for a specific model
     * @param model The model to cancel download for
     */
    void cancelDownloadModel(Model model);
    
    /**
     * Cancels all downloads
     * @param onComplete Callback when all downloads are cancelled
     */
    void cancelAll(OnCompleteCallback onComplete);
    
    /**
     * Observes worker progress for a specific download
     * @param workerId The worker ID to observe
     * @param task The task associated with the download
     * @param model The model being downloaded
     * @param onStatusUpdated Callback for status updates
     */
    void observeWorkerProgress(
        UUID workerId,
        Task task,
        Model model,
        OnStatusUpdatedCallback onStatusUpdated
    );
    
    /**
     * Callback interface for download status updates
     */
    interface OnStatusUpdatedCallback {
        void onStatusUpdated(Model model, ModelDownloadStatus status);
    }
    
    /**
     * Callback interface for completion events
     */
    interface OnCompleteCallback {
        void onComplete();
    }
}
