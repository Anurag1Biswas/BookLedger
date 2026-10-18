package com.lms.exception;

/** A Kafka message could not be understood. Not retried; goes straight to the dead-letter topic. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
