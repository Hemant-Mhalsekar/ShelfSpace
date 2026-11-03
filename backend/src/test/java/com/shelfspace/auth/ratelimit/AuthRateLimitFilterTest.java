package com.shelfspace.auth.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private LoginAttemptServiceTest.TestClock clock;
    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new LoginAttemptServiceTest.TestClock();
        filter = new AuthRateLimitFilter(new RateLimitProperties(5, 15, 3), clock,
                new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    private int call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void blocksAfterTheLimitUntilTheNextMinute() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(call("POST", "/api/v1/auth/login", "9.9.9.9")).isEqualTo(200);
        }
        assertThat(call("POST", "/api/v1/auth/register", "9.9.9.9")).isEqualTo(429);

        clock.advance(Duration.ofMinutes(1));
        assertThat(call("POST", "/api/v1/auth/login", "9.9.9.9")).isEqualTo(200);
    }

    @Test
    void eachIpHasItsOwnLimit() throws Exception {
        for (int i = 0; i < 4; i++) call("POST", "/api/v1/auth/login", "9.9.9.9");
        assertThat(call("POST", "/api/v1/auth/login", "8.8.8.8")).isEqualTo(200);
    }

    @Test
    void otherEndpointsAreNotLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("GET", "/api/v1/books", "9.9.9.9")).isEqualTo(200);
        }
    }
}
