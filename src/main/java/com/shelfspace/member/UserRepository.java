package com.shelfspace.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Cheap check run on every authenticated request. */
    @Query("SELECT u.tokenVersion FROM User u WHERE u.id = :id")
    Optional<Integer> findTokenVersion(@Param("id") Long id);

    long countByRoleAndStatus(Role role, MemberStatus status);

    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL
                   OR LOWER(u.name)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR u.phone LIKE CONCAT('%', :q, '%'))
              AND (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:tier IS NULL OR u.membershipTier = :tier)
            """)
    Page<User> search(@Param("q") String q,
                      @Param("role") Role role,
                      @Param("status") MemberStatus status,
                      @Param("tier") MembershipTier tier,
                      Pageable pageable);
}
