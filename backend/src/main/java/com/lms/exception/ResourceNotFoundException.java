package com.lms.exception;

/** Requested entity does not exist (HTTP 404). Extends RuntimeException so existing catch blocks still work. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
