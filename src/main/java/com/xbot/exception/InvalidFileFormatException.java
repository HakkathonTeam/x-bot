package com.xbot.exception;

public class InvalidFileFormatException extends IllegalArgumentException {
    public InvalidFileFormatException(String message) {
        super(message);
    }
}
