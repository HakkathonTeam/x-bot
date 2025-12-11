package com.xbot.service;

import com.xbot.exception.FileSizeLimitExceededException;
import com.xbot.exception.InvalidFileFormatException;
import com.xbot.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Handles file uploads and downloads from Telegram
 */
public class FileUploadService {
    private static final Logger log = LoggerFactory.getLogger(FileUploadService.class);

    private final TelegramClient telegramClient;
    private final SessionService sessionService;
    private final String tempDirectory;
    private final long maxFileSizeBytes;

    public FileUploadService(TelegramClient telegramClient, SessionService sessionService, long maxFileSizeBytes) {
        this.telegramClient = telegramClient;
        this.sessionService = sessionService;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.tempDirectory = createTempDirectory();
    }

    private String createTempDirectory() {
        try {
            Path tempDir = Files.createTempDirectory("xbot_files");
            log.info("Created temp directory: {}", tempDir);
            return tempDir.toString();
        } catch (IOException e) {
            log.error("Failed to create temp directory", e);
            return System.getProperty("java.io.tmpdir");
        }
    }

    /**
     * Download file from Telegram and save it locally
     */
    public UploadedFile downloadFile(Long userId, Long chatId, Document document) throws IOException, TelegramApiException {
        String fileId = document.getFileId();
        String fileName = document.getFileName();
        String mimeType = document.getMimeType();
        long fileSize = document.getFileSize() != null ? document.getFileSize() : 0;

        // Check file size
        if (fileSize >= maxFileSizeBytes || fileSize == 0) {
            throw new FileSizeLimitExceededException(fileSize, maxFileSizeBytes);
        }

        log.info("Downloading file: {} ({} bytes) for user {}", fileName, fileSize, userId);

        // Create UploadedFile object
        UploadedFile uploadedFile = new UploadedFile(fileId, fileName, mimeType, fileSize);

        // Check file format
        if (!uploadedFile.isSupportedFormat()) {
            throw new InvalidFileFormatException("Unsupported file format. Please upload HTML or JSON files.");
        }

        // Download file from Telegram
        org.telegram.telegrambots.meta.api.objects.File telegramFile;

        GetFile getFileRequest = new GetFile(fileId);
        telegramFile = telegramClient.execute(getFileRequest);
        if (telegramFile == null || telegramFile.getFilePath() == null) {
            throw new IOException("Can't get telegramfile for file_id: " + fileId);
        }

        java.io.File file = telegramClient.downloadFile(telegramFile.getFilePath());
        if (file == null) {
            throw new IOException("Failed to download file from Telegram");
        }

        // Save to our temp directory
        String localFileName = userId + "_" + System.currentTimeMillis() + "_" + fileName;
        Path localPath = Paths.get(tempDirectory, localFileName);

        try (InputStream in = Files.newInputStream(file.toPath());
             FileOutputStream out = new FileOutputStream(localPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        uploadedFile.setLocalPath(localPath.toString());
        sessionService.addFile(userId, chatId, uploadedFile);

        log.info("File saved to: {}", localPath);
        return uploadedFile;
    }

    /**
     * Get file from session
     */
    /*public UploadedFile getFile(Long userId, int index) {
        List<UploadedFile> files = sessionService.getFiles(userId);
        if (index >= 0 && index < files.size()) {
            return files.get(index);
        }
        return null;
    }*/

    /**
     * Clean up local files for a user
     */
    public void cleanupUserFiles(Long userId, Long chatId) {
        sessionService.cleanFiles(userId, chatId);
    }

    /**
     * Clean up all temp files
     */
    public void deleteTempDir() {
        try {
            Path dir = Paths.get(tempDirectory);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete file: {}", path, e);
                            }
                        });
                log.info("Cleaned up temp directory: {}", tempDirectory);
                try {
                    Files.delete(dir);
                    log.info("Deleted temp directory: {}", tempDirectory);
                } catch (DirectoryNotEmptyException e) {
                    log.warn("Directory not empty after cleanup, skipping: {}", dir);
                } catch (IOException e) {
                    log.warn("Failed to delete directory: {}", dir, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to cleanup temp directory", e);
        }
    }

    public String getTempDirectory() {
        return tempDirectory;
    }
}