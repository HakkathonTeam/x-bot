package com.xbot.service;

import com.xbot.exception.BusySessionException;
import com.xbot.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        UPLOADING_BGN,
        UPLOADING,
        PROCESSING
    }

    private static class UserSession{
        Long userId;
        List<UploadedFile> files = new ArrayList<>();
        UserSessionState state = UserSessionState.IDLE;
        Timer timer;
        LocalDateTime lastActivity;
        Long chatId;
        boolean maxFilesErrorMsgFlag;
        int fileCount;

        UserSession(Long userId, Long chatId) {
            this.userId = userId;
            this.chatId = chatId;
            this.lastActivity = LocalDateTime.now();
            this.maxFilesErrorMsgFlag = false;
            this.fileCount = 0;
        }

        void deleteFiles() {
            if (!files.isEmpty()) {
                for (var currentFile : files) {
                    if (currentFile.getLocalPath() != null) {
                        try {
                            Files.deleteIfExists(Paths.get(currentFile.getLocalPath()));
                            log.debug("Deleted file: {}", currentFile.getLocalPath());
                        } catch (IOException e) {
                            log.warn("Failed to delete file: {}", currentFile.getLocalPath(), e);
                        }
                    }
                }
                files.clear();
            }
        }
    }

    public interface ProcessingCallback {
        boolean onProcessingBegin(Long userId, Long chatId, List<UploadedFile> files) throws Exception;
        void onProcessingComplete(Long chatId);
        void onProcessingError(Long chatId);
    }

    // Map: userId -> list of uploaded files
    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private ProcessingCallback processingCallback;

    private final int maxFilesPerUser;
    private final long sessionTimeoutMinutes;
    private final int processingTimeoutMs;
    private final Timer cleanupTimer = new Timer("session-cleanup-timer", true);

    public SessionService(int maxFilesPerUser, long sessionTimeoutMinutes, int processingTimeoutMs) {
        this.maxFilesPerUser = maxFilesPerUser;
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.processingTimeoutMs = processingTimeoutMs;

        cleanupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                cleanupOldSessions();
            }
        }, 300000, 300000);
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

        if (session.state == UserSessionState.IDLE || session.state == UserSessionState.UPLOADING_BGN) {
            session.state = UserSessionState.UPLOADING;
            startProcessTimer(session);
        } else if (session.state == UserSessionState.UPLOADING) {
            resetProcessTimer(session);
        }
    }

    public synchronized void cleanAllFiles() {
        sessions.values().forEach(UserSession::deleteFiles);
    }

    public int getFileCount(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.fileCount;
    }

    public boolean isBusy(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.state == UserSessionState.PROCESSING;
    }

    public boolean isIdle(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.state == UserSessionState.IDLE;
    }

    public void setUploadBegin(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        session.state = UserSessionState.UPLOADING_BGN;
    }

    public synchronized void cleanupOldSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        Iterator<Map.Entry<Long, UserSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, UserSession> entry = iterator.next();
            UserSession session = entry.getValue();

            if (session.lastActivity.isBefore(cutoff)) {
                cancelTimer(session);
                session.deleteFiles();
                iterator.remove();
                log.debug("Removed old session for user: {}", session.userId);
            }
        }
    }

    public synchronized void stopAllTimers() {
        cleanupTimer.cancel();
        sessions.values().forEach(this::cancelTimer);
    }

    public boolean maxFilesErrorMsgWasSend(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        return session.maxFilesErrorMsgFlag;
    }

    public void setMaxFilesErrorMsgWasSend(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        session.maxFilesErrorMsgFlag = true;
    }

    public void increaseFileCount(Long userId, Long chatId) {
        UserSession session = getOrCreateSession(userId, chatId);
        session.fileCount++;
    }

    private UserSession getOrCreateSession(Long userId, Long chatId) {
        return sessions.computeIfAbsent(userId, k -> new UserSession(userId, chatId));
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

    private synchronized void triggerProcess(UserSession session) {
        if (session.state != UserSessionState.UPLOADING) {
            log.warn("Wooow! Cannot trigger processing for user {}, wrong state {}", session.userId, session.state);
            return;
        }

            if (session.files.isEmpty()) {
                log.warn("Oops! No files to process for user {}", session.userId);
                session.state = UserSessionState.IDLE;
                session.maxFilesErrorMsgFlag = false;
                session.fileCount = 0;
                return;
            }

            log.info("Triggering processing for user {} with {} files", session.userId, session.files.size());
            session.state = UserSessionState.PROCESSING;
            session.lastActivity = LocalDateTime.now();
            try {
                if (processingCallback != null) {
                    log.debug("Process begin for user: {}", session.userId);
                    if (processingCallback.onProcessingBegin(session.userId, session.chatId, session.files)) {
                        processingCallback.onProcessingComplete(session.chatId);
                    } else {
                        processingCallback.onProcessingError(session.chatId);
                    }
                    session.deleteFiles();
                }
            } catch (Exception e) {
                log.error("Error during process: {}", e.getMessage());
                Thread.currentThread().interrupt();
                if (processingCallback != null) {
                    processingCallback.onProcessingError(session.chatId);
                }
            }
            log.debug("Process end for user: {}", session.userId);
            session.lastActivity = LocalDateTime.now();
            session.state = UserSessionState.IDLE;
            session.maxFilesErrorMsgFlag = false;
            session.fileCount = 0;
    }
}