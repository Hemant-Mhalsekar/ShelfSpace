package com.shelfspace.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Marks a token as used, but only if it is still fresh. Returns 0 if someone already used or revoked it.
     * Doing this in one UPDATE means two requests racing with the same token can't both succeed.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken t SET t.usedAt = :now
            WHERE t.id = :id AND t.usedAt IS NULL AND t.revokedAt IS NULL AND t.expiresAt > :now
            """)
    int markUsed(@Param("id") String id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.userId = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
