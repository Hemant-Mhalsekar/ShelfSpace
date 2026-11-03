package com.shelfspace.common.exception;

/** Thrown when a request breaks a library rule (limit reached, unpaid fines, invalid state change...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
