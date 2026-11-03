package com.shelfspace.common.exception;

import java.time.Instant;
import java.util.Map;

/** Single error shape returned by every endpoint. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
