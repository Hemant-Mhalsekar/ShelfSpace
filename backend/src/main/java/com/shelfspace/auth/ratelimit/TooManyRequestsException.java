package com.shelfspace.auth.ratelimit;

import lombok.Getter;

/** Mapped to 429 with a Retry-After header. */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
