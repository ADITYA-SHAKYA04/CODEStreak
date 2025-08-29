package com.example.codestreak.data.model;

import java.util.List;

/**
 * Model data class - exactly matching Google AI Edge Gallery structure
 */
public class Model {
    public final String name;
    public final String normalizedName;
    public final String url;
    public final String version;
    public final String downloadFileName;
    public final boolean isZip;
    public final String unzipDir;
    public final long totalBytes;
    public final String accessToken;
    public final List<ExtraDataFile> extraDataFiles;
    
    public Model(String name, String normalizedName, String url, String version, 
                String downloadFileName, boolean isZip, String unzipDir, 
                long totalBytes, String accessToken, List<ExtraDataFile> extraDataFiles) {
        this.name = name;
        this.normalizedName = normalizedName;
        this.url = url;
        this.version = version;
        this.downloadFileName = downloadFileName;
        this.isZip = isZip;
        this.unzipDir = unzipDir;
        this.totalBytes = totalBytes;
        this.accessToken = accessToken;
        this.extraDataFiles = extraDataFiles != null ? extraDataFiles : new java.util.ArrayList<>();
    }
    
    /**
     * Extra data file for models that need additional files
     */
    public static class ExtraDataFile {
        public final String url;
        public final String downloadFileName;
        public final long sizeInBytes;
        
        public ExtraDataFile(String url, String downloadFileName, long sizeInBytes) {
            this.url = url;
            this.downloadFileName = downloadFileName;
            this.sizeInBytes = sizeInBytes;
        }
    }
}
