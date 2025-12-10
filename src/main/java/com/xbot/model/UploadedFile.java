package com.xbot.model;

import java.time.LocalDateTime;

/**
 * Represents an uploaded file from a user
 */
public class UploadedFile {
    private final String fileId;
    private final String fileName;
    private final String mimeType;
    private final long fileSize;
    private final LocalDateTime uploadedAt;
    private String localPath; // Where file is saved temporarily
    private boolean processed;

    public UploadedFile(String fileId, String fileName, String mimeType, long fileSize) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadedAt = LocalDateTime.now();
        this.processed = false;
    }

    // Getters and setters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public String getMimeType() { return mimeType; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }

    public boolean isHtmlFile() {
        return fileName != null &&
                (fileName.toLowerCase().endsWith(".html") ||
                        fileName.toLowerCase().endsWith(".htm"));
    }

    public boolean isJsonFile() {
        return fileName != null && fileName.toLowerCase().endsWith(".json");
    }

    public boolean isSupportedFormat() {
        return isHtmlFile() || isJsonFile();
    }
}
