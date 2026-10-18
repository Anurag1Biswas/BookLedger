package com.lms.exception;

/** A library business rule was violated, e.g. book not available (HTTP 400). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
