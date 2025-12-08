package com.xbot.service;

import com.xbot.exception.BusySessionException;
import com.xbot.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user sessions and uploaded files
 */
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);


    public enum UserSessionState {
        IDLE,
        UPLOADING,
        PROCESSING,
        ERROR
    };

    private static class UserSession{
        Long userId;
        List<UploadedFile> files = new ArrayList<>();
        UserSessionState state = UserSessionState.IDLE;
        Timer timer;
        LocalDateTime lastActivity;
        Long chatId;

        UserSession(Long userId, Long chatId) {
            this.userId = userId;
            this.chatId = chatId;
            this.lastActivity = LocalDateTime.now();
        }
    }

    public interface ProcessingCallback {
        void onProcessingTimeout(Long userId, List<UploadedFile> files);
        void onProcessingComplete(Long userId);
        void onProcessingError(Long userId, String error);
    }

    // Map: userId -> list of uploaded files
    private final Map<Long, UserSession> userSession = new ConcurrentHashMap<>();
    private ProcessingCallback processingCallback;

    private final int maxFilesPerUser;
    private final long sessionTimeoutMinutes;
    private final int processingTimeoutMs;

    public SessionService(int maxFilesPerUser, long sessionTimeoutMinutes, int processingTimeoutMs) {
        this.maxFilesPerUser = maxFilesPerUser;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.processingTimeoutMs = processingTimeoutMs;
    }

    public void setProcessingCallback(ProcessingCallback callback) {
        this.processingCallback = callback;
    }

    public synchronized void addFile(Long userId, Long chatId, UploadedFile file) {
        UserSession session = getOrCreateSession(userId, chatId);

        if (session.state == UserSessionState.PROCESSING) {
            throw new BusySessionException("Wait for previous request done!");
        }

        if (session.files.size() >= maxFilesPerUser) {
            throw new IllegalStateException("Maximum " + maxFilesPerUser + " files allowed");
        }

        session.files.add(file);
        session.lastActivity = LocalDateTime.now();

        log.info("Added file {} for user {}, total files: {}, state: {}",
                file.getFileName(), userId, session.files.size(), session.state);

        if (session.state == UserSessionState.IDLE) {
            session.state = UserSessionState.UPLOADING;
            startProcessTimer(session);
        } else if (session.state == UserSessionState.UPLOADING) {
            resetProcessTimer(session);
        }
    }

    public List<UploadedFile> getFiles(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.files;
    }

    public void clearFiles(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        session.files.clear();
        log.info("Cleared files for user {}", userId);
    }

    public int getFileCount(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.files.size();
    }

    public boolean isBusy(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.state == UserSessionState.PROCESSING;
    }

    /*public boolean hasFiles(Long userId) {
        return getFileCount(userId) > 0;
    }

    public void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        userFiles.entrySet().removeIf(entry ->
                lastActivity.getOrDefault(entry.getKey(), LocalDateTime.MIN).isBefore(cutoff));
    }*/

    private UserSession getOrCreateSession(Long userId, Long chatId) {
        return userSession.computeIfAbsent(userId, k -> new UserSession(userId, chatId));
    }

    private void startProcessTimer(UserSession session) {
        session.timer = new Timer(true);
        session.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("Timer triggered for user {}", session.userId);
                triggerProcess(session);
            }
        }, processingTimeoutMs);

        log.debug("Started timer for user {} ({} ms)", session.userId, processingTimeoutMs);
    }

    private void resetProcessTimer(UserSession session) {
        cancelTimer(session);
        startProcessTimer(session);
        log.debug("Reset timer for user {}", session.userId);
    }

    private void cancelTimer(UserSession session) {
        if (session.timer != null) {
            session.timer.cancel();
            session.timer = null;
            log.debug("Cancelled timer for user {}", session.userId);
        }
    }

    private void triggerProcess(UserSession session) {
        if (session.state != UserSessionState.UPLOADING) {
            log.warn("Wooow! Cannot trigger processing for user {}, wrong state {}", session.userId, session.state);
            return;
        }

        if (session.files.isEmpty()) {
            log.warn("Oops! No files to process for user {}", session.userId);
            session.state = UserSessionState.IDLE;
            return;
        }

        log.info("Triggering processing for user {} with {} files", session.userId, session.files.size());
        if (processingCallback != null) {
            processingCallback.onProcessingTimeout(session.userId, new ArrayList<>(session.files));
        }
    }
}