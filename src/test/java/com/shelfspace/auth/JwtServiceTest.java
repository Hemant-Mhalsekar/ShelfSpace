package com.shelfspace.auth;

import com.shelfspace.member.Role;
import com.shelfspace.member.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-longer-than-32-bytes";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 15, 7));
        user = new User();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setEmail("hemant@example.com");
        user.setRole(Role.ADMIN);
    }

    @Test
    void accessTokenRoundTrip() {
        String token = jwtService.generateAccessToken(user);

        AuthUser parsed = jwtService.parse(token, TokenType.ACCESS);

        assertThat(parsed.id()).isEqualTo(42L);
        assertThat(parsed.email()).isEqualTo("hemant@example.com");
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refresh = jwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> jwtService.parse(refresh, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.parse(tampered, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService other = new JwtService(new JwtProperties("another-secret-that-is-also-longer-than-32-bytes", 15, 7));
        String foreign = other.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parse(foreign, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiring = new JwtService(new JwtProperties(SECRET, -1, 7));
        String expired = expiring.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parse(expired, TokenType.ACCESS))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 15, 7)))
                .isInstanceOf(IllegalStateException.class);
    }
}
