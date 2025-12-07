package com.xbot.exception;

public class FileSizeLimitExceededException extends IllegalArgumentException {

    public FileSizeLimitExceededException(long currentSize, long maxSize) {
        super(String.format("File size %d exceeds maximum %d", currentSize, maxSize));
    }
}
