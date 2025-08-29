package com.example.codestreak;

import android.content.Context;
import java.io.File;

/**
 * Google AI Edge Gallery compatible model representation for chat AI
 * Based on Google's Model.kt structure
 */
public class GoogleChatModel {
    private final String name;
    private final String displayName;
    private final String info;
    private final String url;
    private final long sizeInBytes;
    private final String downloadFileName;
    private final String version;
    private final boolean isZip;
    private final String unzipDir;
    private final String accessToken;
    private final String normalizedName;
    
    // Constructor
    public GoogleChatModel(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName.isEmpty() ? builder.name : builder.displayName;
        this.info = builder.info;
        this.url = builder.url;
        this.sizeInBytes = builder.sizeInBytes;
        this.downloadFileName = builder.downloadFileName;
        this.version = builder.version;
        this.isZip = builder.isZip;
        this.unzipDir = builder.unzipDir;
        this.accessToken = builder.accessToken;
        this.normalizedName = name.replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    // Getters exactly like Google's implementation
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getInfo() { return info; }
    public String getUrl() { return url; }
    public long getSizeInBytes() { return sizeInBytes; }
    public String getDownloadFileName() { return downloadFileName; }
    public String getVersion() { return version; }
    public boolean isZip() { return isZip; }
    public String getUnzipDir() { return unzipDir; }
    public String getAccessToken() { return accessToken; }
    public String getNormalizedName() { return normalizedName; }
    
    /**
     * Get extra data files total size (for now returning 0, can be extended)
     */
    public long getExtraDataFilesTotalSize() {
        return 0L;
    }
    
    /**
     * Get the model file path exactly like Google's getPath() method
     */
    public String getModelPath(Context context) {
        String baseDir = context.getExternalFilesDir(null).getAbsolutePath() + 
            File.separator + normalizedName + File.separator + version;
        
        if (isZip && !unzipDir.isEmpty()) {
            return baseDir + File.separator + unzipDir;
        } else {
            return baseDir + File.separator + downloadFileName;
        }
    }
    
    /**
     * Check if model is already downloaded
     */
    public boolean isDownloaded(Context context) {
        File modelFile = new File(getModelPath(context));
        return modelFile.exists();
    }
    
    /**
     * Builder pattern exactly like Google's approach
     */
    public static class Builder {
        private String name = "";
        private String displayName = "";
        private String info = "";
        private String url = "";
        private long sizeInBytes = 0L;
        private String downloadFileName = "";
        private String version = "1.0";
        private boolean isZip = false;
        private String unzipDir = "";
        private String accessToken = null;
        
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Builder setInfo(String info) {
            this.info = info;
            return this;
        }
        
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }
        
        public Builder setSizeInBytes(long sizeInBytes) {
            this.sizeInBytes = sizeInBytes;
            return this;
        }
        
        public Builder setDownloadFileName(String downloadFileName) {
            this.downloadFileName = downloadFileName;
            return this;
        }
        
        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }
        
        public Builder setIsZip(boolean isZip) {
            this.isZip = isZip;
            return this;
        }
        
        public Builder setUnzipDir(String unzipDir) {
            this.unzipDir = unzipDir;
            return this;
        }
        
        public Builder setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public GoogleChatModel build() {
            return new GoogleChatModel(this);
        }
    }
    
    // Predefined chat models exactly like Google's approach
    public static final GoogleChatModel GEMMA_2B = new Builder()
        .setName("Gemma-2B-Chat")
        .setDisplayName("Gemma 2B Chat")
        .setInfo("Google's Gemma 2B model optimized for chat and conversation")
        .setUrl("https://huggingface.co/google/gemma-2b-it/resolve/main/gemma-2b-it.gguf")
        .setSizeInBytes(1_600_000_000L) // ~1.6GB
        .setDownloadFileName("gemma-2b-it.gguf")
        .setVersion("1.0")
        .build();
    
    public static final GoogleChatModel GEMMA_7B = new Builder()
        .setName("Gemma-7B-Chat")
        .setDisplayName("Gemma 7B Chat")
        .setInfo("Google's Gemma 7B model with enhanced conversation capabilities")
        .setUrl("https://huggingface.co/google/gemma-7b-it/resolve/main/gemma-7b-it.gguf")
        .setSizeInBytes(4_800_000_000L) // ~4.8GB
        .setDownloadFileName("gemma-7b-it.gguf")
        .setVersion("1.0")
        .build();
    
    public static final GoogleChatModel LLAMA_3_8B = new Builder()
        .setName("Llama-3-8B-Chat")
        .setDisplayName("Llama 3 8B Chat")
        .setInfo("Meta's Llama 3 8B model fine-tuned for chat applications")
        .setUrl("https://huggingface.co/microsoft/Llama-3-8B-Instruct-GGUF/resolve/main/Llama-3-8B-Instruct-Q4_K_M.gguf")
        .setSizeInBytes(4_370_000_000L) // ~4.37GB
        .setDownloadFileName("llama-3-8b-instruct.gguf")
        .setVersion("1.0")
        .build();
    
    public static final GoogleChatModel PHI_3_MINI = new Builder()
        .setName("Phi-3-Mini-Chat")
        .setDisplayName("Phi-3 Mini Chat")
        .setInfo("Microsoft's Phi-3 Mini model optimized for mobile and edge devices")
        .setUrl("https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf")
        .setSizeInBytes(2_400_000_000L) // ~2.4GB
        .setDownloadFileName("phi-3-mini-instruct.gguf")
        .setVersion("1.0")
        .build();
    
    public static final GoogleChatModel STABLE_CODE_3B = new Builder()
        .setName("StableCode-3B-Chat")
        .setDisplayName("StableCode 3B Chat")
        .setInfo("Stability AI's code-focused model for programming assistance and chat")
        .setUrl("https://huggingface.co/stabilityai/stablecode-completion-alpha-3b-4k/resolve/main/model.gguf")
        .setSizeInBytes(1_800_000_000L) // ~1.8GB
        .setDownloadFileName("stablecode-3b.gguf")
        .setVersion("1.0")
        .build();
}
