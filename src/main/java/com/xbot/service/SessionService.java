package com.xbot.service;

import com.xbot.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions and uploaded files
 */
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    // Map: userId -> list of uploaded files
    private final Map<Long, List<UploadedFile>> userFiles = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lastActivity = new ConcurrentHashMap<>();

    private static final int MAX_FILES_PER_USER = 10;
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    public void addFile(Long userId, UploadedFile file) {
        List<UploadedFile> files = userFiles.computeIfAbsent(userId, k -> new ArrayList<>());

        if (files.size() >= MAX_FILES_PER_USER) {
            throw new IllegalStateException("Maximum " + MAX_FILES_PER_USER + " files allowed");
        }

        files.add(file);
        lastActivity.put(userId, LocalDateTime.now());
        log.info("Added file {} for user {}, total files: {}",
                file.getFileName(), userId, files.size());
    }

    public List<UploadedFile> getFiles(Long userId) {
        lastActivity.put(userId, LocalDateTime.now());
        return userFiles.getOrDefault(userId, new ArrayList<>());
    }

    public void clearFiles(Long userId) {
        List<UploadedFile> files = userFiles.remove(userId);
        if (files != null) {
            log.info("Cleared {} files for user {}", files.size(), userId);
        }
    }

    public int getFileCount(Long userId) {
        return userFiles.getOrDefault(userId, new ArrayList<>()).size();
    }

    public boolean hasFiles(Long userId) {
        return getFileCount(userId) > 0;
    }

    public void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES);
        userFiles.entrySet().removeIf(entry ->
                lastActivity.getOrDefault(entry.getKey(), LocalDateTime.MIN).isBefore(cutoff));
    }
}