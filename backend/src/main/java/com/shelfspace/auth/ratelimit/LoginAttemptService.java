package com.shelfspace.auth.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks an email + IP pair after too many wrong passwords.
 *
 * Keyed on both so that someone guessing from one machine is stopped, while they can't lock the real
 * owner out from somewhere else. Kept in memory: fine for a single instance; several instances would
 * need a shared store such as Redis.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED = 10_000;

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lock;
    private final Clock clock;

    public LoginAttemptService(RateLimitProperties props, Clock clock) {
        this.maxFailures = props.loginMaxFailures();
        this.lock = Duration.ofMinutes(props.loginLockMinutes());
        this.clock = clock;
    }

    /** @throws TooManyRequestsException while the pair is locked, even if the password is right */
    public void checkAllowed(String email, String ip) {
        Attempts a = attempts.get(key(email, ip));
        Instant now = clock.instant();
        if (a != null && a.lockedUntil() != null && a.lockedUntil().isAfter(now)) {
            long seconds = Math.max(1, Duration.between(now, a.lockedUntil()).toSeconds());
            long minutes = (seconds + 59) / 60;
            throw new TooManyRequestsException(
                    "Too many failed sign-in attempts. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."),
                    seconds);
        }
    }

    public void recordFailure(String email, String ip) {
        Instant now = clock.instant();
        if (attempts.size() > MAX_TRACKED) {
            prune(now);
        }
        Attempts updated = attempts.compute(key(email, ip), (k, a) -> {
            if (a == null || a.windowStart().plus(lock).isBefore(now)) {
                return new Attempts(1, now, null);
            }
            int failures = a.failures() + 1;
            return new Attempts(failures, a.windowStart(), failures >= maxFailures ? now.plus(lock) : null);
        });
        if (updated.lockedUntil() != null && updated.failures() == maxFailures) {
            log.warn("Sign-in locked for {} from {} after {} failures", email, ip, maxFailures);
        }
    }

    public void recordSuccess(String email, String ip) {
        attempts.remove(key(email, ip));
    }

    private void prune(Instant now) {
        attempts.entrySet().removeIf(e -> {
            Attempts a = e.getValue();
            Instant end = a.lockedUntil() != null ? a.lockedUntil() : a.windowStart().plus(lock);
            return end.isBefore(now);
        });
    }

    private static String key(String email, String ip) {
        return email + '|' + ip;
    }
}
