package com.shelfspace.auth.token;

import com.shelfspace.auth.InvalidTokenException;
import com.shelfspace.auth.JwtService;
import com.shelfspace.auth.dto.AuthResponse;
import com.shelfspace.common.exception.BusinessRuleException;
import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.User;
import com.shelfspace.member.UserRepository;
import com.shelfspace.member.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Issues, rotates and revokes tokens.
 *
 * - Every sign-in starts a "family". Each refresh uses up the old refresh token and issues a new one in the same family.
 * - If a used refresh token comes back, it was copied: the whole family is revoked and that device must sign in again.
 * - Bumping the user's token version kills every access and refresh token they have, on every device.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String SIGNED_OUT = "Your session has ended. Please sign in again.";

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** A new sign-in: new family. */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuthResponse issue(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    /**
     * Swaps a refresh token for a new pair. Not rolled back on InvalidTokenException,
     * so revoking a family after reuse is kept even though the request fails.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse rotate(String refreshToken) {
        RefreshClaims claims = jwtService.parseRefresh(refreshToken);
        LocalDateTime now = LocalDateTime.now(clock);

        if (refreshTokenRepository.markUsed(claims.tokenId(), now) == 0) {
            refreshTokenRepository.findById(claims.tokenId())
                    .filter(t -> t.getUsedAt() != null && t.getRevokedAt() == null)
                    .ifPresent(t -> {
                        log.warn("Refresh token reuse for user {}; revoking family {}", claims.userId(), claims.familyId());
                        refreshTokenRepository.revokeFamily(claims.familyId(), now);
                    });
            throw new InvalidTokenException(SIGNED_OUT);
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new InvalidTokenException(SIGNED_OUT));
        if (user.getTokenVersion() != claims.tokenVersion()) {
            throw new InvalidTokenException(SIGNED_OUT);
        }
        if (user.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("This account is suspended. Please contact the library.");
        }
        return issue(user, claims.familyId());
    }

    /** Sign-out on one device. Quietly does nothing for a token that is already invalid. */
    @Transactional
    public void revoke(String refreshToken) {
        try {
            RefreshClaims claims = jwtService.parseRefresh(refreshToken);
            refreshTokenRepository.revokeFamily(claims.familyId(), LocalDateTime.now(clock));
        } catch (InvalidTokenException ignored) {
            // Nothing to revoke
        }
    }

    /** Signs the user out everywhere. The caller's transaction saves the new version. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAll(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        refreshTokenRepository.revokeAllForUser(user.getId(), LocalDateTime.now(clock));
    }

    /** Used by the auth filter on every request. */
    @Transactional(readOnly = true)
    public boolean isCurrent(Long userId, int tokenVersion) {
        return userRepository.findTokenVersion(userId).map(v -> v == tokenVersion).orElse(false);
    }

    /** Expired rows are no use to anyone; keep them a day for troubleshooting, then delete. */
    @Scheduled(cron = "${app.jobs.token-cleanup-cron}")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now(clock).minusDays(1));
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    private AuthResponse issue(User user, String familyId) {
        String tokenId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokenRepository.save(new RefreshToken(tokenId, user.getId(), familyId, now, now.plus(jwtService.refreshTtl())));
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user, tokenId, familyId),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user));
    }
}
