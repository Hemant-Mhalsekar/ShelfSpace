package com.shelfspace.auth.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shelfspace.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caps register, login and refresh calls per IP per minute (fixed one-minute windows).
 * Other endpoints need a valid token, so they aren't limited here.
 *
 * The client IP comes from X-Forwarded-For (server.forward-headers-strategy=framework), which is right
 * behind Vercel and Render. Like LoginAttemptService, the counters are per instance and in memory.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED = Set.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh");
    private static final int MAX_TRACKED = 10_000;

    private record Window(long minute, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int perMinute;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(RateLimitProperties props, Clock clock, ObjectMapper objectMapper) {
        this.perMinute = props.authRequestsPerMinute();
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return perMinute <= 0 || !"POST".equals(request.getMethod()) || !LIMITED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant now = clock.instant();
        long minute = now.getEpochSecond() / 60;
        if (windows.size() > MAX_TRACKED) {
            windows.values().removeIf(w -> w.minute() < minute);
        }
        Window w = windows.merge(request.getRemoteAddr(), new Window(minute, 1),
                (old, fresh) -> old.minute() == minute ? new Window(minute, old.count() + 1) : fresh);

        if (w.count() > perMinute) {
            long retryAfter = 60 - now.getEpochSecond() % 60;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), new ApiError(now, 429, "Too Many Requests",
                    "Too many requests. Please wait a minute and try again.", request.getRequestURI(), null));
            return;
        }
        chain.doFilter(request, response);
    }
}
