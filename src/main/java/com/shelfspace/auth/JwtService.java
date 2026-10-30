package com.shelfspace.auth;

import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies signed JWTs.
 * Access tokens are short-lived and sent on every request; refresh tokens only get a new pair.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtService(JwtProperties props) {
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = Duration.ofMinutes(props.accessTokenMinutes());
        this.refreshTtl = Duration.ofDays(props.refreshTokenDays());
    }

    public String generateAccessToken(User user) {
        return generate(user, TokenType.ACCESS, accessTtl);
    }

    public String generateRefreshToken(User user) {
        return generate(user, TokenType.REFRESH, refreshTtl);
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * Verifies signature, expiry and token type.
     *
     * @throws InvalidTokenException if anything is wrong with the token
     */
    public AuthUser parse(String token, TokenType expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.name().equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new InvalidTokenException("Wrong token type");
            }
            return new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    private String generate(User user, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }
}
