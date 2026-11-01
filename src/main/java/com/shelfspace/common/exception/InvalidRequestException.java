package com.shelfspace.common.exception;

/** A request that is well-formed JSON but asks for something invalid (e.g. an unsupported sort field). Maps to 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
