package com.shelfspace.auth.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    /** A clock the test can move forward. */
    static class TestClock extends Clock {
        Instant now = Instant.parse("2026-11-02T10:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private TestClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        service = new LoginAttemptService(new RateLimitProperties(3, 15, 0), clock);
    }

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterTheLockTime() {
        for (int i = 0; i < 3; i++) service.recordFailure("a@x.com", "1.1.1.1");

        assertThatThrownBy(() -> service.checkAllowed("a@x.com", "1.1.1.1"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("15 minutes");

        clock.advance(Duration.ofMinutes(14));
        assertThatThrownBy(() -> service.checkAllowed("a@x.com", "1.1.1.1"))
                .hasMessageContaining("1 minute.");

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));
        assertThatCode(() -> service.checkAllowed("a@x.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void failuresSpreadOverMoreThanTheWindowDontAddUp() {
        service.recordFailure("a@x.com", "1.1.1.1");
        service.recordFailure("a@x.com", "1.1.1.1");
        clock.advance(Duration.ofMinutes(16));
        service.recordFailure("a@x.com", "1.1.1.1");

        assertThatCode(() -> service.checkAllowed("a@x.com", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void otherIpsAndEmailsAreUnaffected() {
        for (int i = 0; i < 3; i++) service.recordFailure("a@x.com", "1.1.1.1");

        assertThatCode(() -> service.checkAllowed("a@x.com", "2.2.2.2")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkAllowed("b@x.com", "1.1.1.1")).doesNotThrowAnyException();
    }
}
