package com.shelfspace.auth;

import com.shelfspace.auth.token.RefreshClaims;
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
 * Both carry the user's token version so they can be revoked; refresh tokens also carry an id (jti)
 * and a family id so they can be tracked in the database.
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_VERSION = "ver";
    private static final String CLAIM_FAMILY = "fam";

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
        return base(user, TokenType.ACCESS, accessTtl).compact();
    }

    public String generateRefreshToken(User user, String tokenId, String familyId) {
        return base(user, TokenType.REFRESH, refreshTtl)
                .id(tokenId)
                .claim(CLAIM_FAMILY, familyId)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    /**
     * Verifies signature, expiry and token type.
     *
     * @throws InvalidTokenException if anything is wrong with the token
     */
    public AuthUser parse(String token, TokenType expectedType) {
        Claims claims = verify(token, expectedType);
        try {
            return new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    version(claims));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    public RefreshClaims parseRefresh(String token) {
        Claims claims = verify(token, TokenType.REFRESH);
        String jti = claims.getId();
        String family = claims.get(CLAIM_FAMILY, String.class);
        if (jti == null || family == null) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        try {
            return new RefreshClaims(Long.valueOf(claims.getSubject()), jti, family, version(claims));
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    private Claims verify(String token, TokenType expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        if (!expectedType.name().equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new InvalidTokenException("Wrong token type");
        }
        return claims;
    }

    private static int version(Claims claims) {
        Integer v = claims.get(CLAIM_VERSION, Integer.class);
        return v == null ? 0 : v;
    }

    private io.jsonwebtoken.JwtBuilder base(User user, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key);
    }
}
