package com.shelfspace.auth.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.rate-limit.*.
 *
 * @param loginMaxFailures      wrong passwords allowed for one email from one IP before it is locked
 * @param loginLockMinutes      how long the lock lasts (also the window failures are counted in)
 * @param authRequestsPerMinute register/login/refresh calls allowed per IP per minute; 0 turns it off
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(int loginMaxFailures, int loginLockMinutes, int authRequestsPerMinute) {
}
