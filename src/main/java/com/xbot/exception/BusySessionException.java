package com.xbot.exception;

public class BusySessionException extends RuntimeException {
    public BusySessionException(String message) {
        super(message);
    }
}
