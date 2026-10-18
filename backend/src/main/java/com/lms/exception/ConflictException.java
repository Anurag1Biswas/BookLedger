package com.lms.exception;

/** The request conflicts with current state, e.g. deleting a book that has history (HTTP 409). */
public class ConflictException extends BusinessRuleException {
    public ConflictException(String message) {
        super(message);
    }
}
