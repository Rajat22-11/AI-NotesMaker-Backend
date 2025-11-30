package com.contentprocessor.exception;

/**
 * Simple runtime exception to represent 400 Bad Request errors in service logic.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

