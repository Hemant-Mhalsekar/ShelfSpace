package com.shelfspace.fine;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    @EntityGraph(attributePaths = {"loan", "loan.member", "loan.copy", "loan.copy.book"})
    List<Fine> findByLoanMemberIdOrderByCreatedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"loan", "loan.member", "loan.copy", "loan.copy.book"})
    Optional<Fine> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"loan", "loan.member", "loan.copy", "loan.copy.book"})
    @Query(value = """
            SELECT f FROM Fine f
            WHERE (:memberId IS NULL OR f.loan.member.id = :memberId)
              AND (:status IS NULL OR f.status = :status)
            """,
            countQuery = """
            SELECT COUNT(f) FROM Fine f
            WHERE (:memberId IS NULL OR f.loan.member.id = :memberId)
              AND (:status IS NULL OR f.status = :status)
            """)
    Page<Fine> search(@Param("memberId") Long memberId, @Param("status") FineStatus status, Pageable pageable);

    @Query("""
            SELECT SUM(f.amount) FROM Fine f
            WHERE f.loan.member.id = :memberId AND f.status = com.shelfspace.fine.FineStatus.UNPAID
            """)
    BigDecimal sumUnpaidForMember(@Param("memberId") Long memberId);

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.status = com.shelfspace.fine.FineStatus.UNPAID")
    BigDecimal sumAllUnpaid();

    @Query("""
            SELECT SUM(f.amount) FROM Fine f
            WHERE f.status = com.shelfspace.fine.FineStatus.PAID AND f.paidAt >= :since
            """)
    BigDecimal sumPaidSince(@Param("since") java.time.LocalDateTime since);

    @Query("""
            SELECT f.loan.member.id AS memberId, SUM(f.amount) AS total FROM Fine f
            WHERE f.status = com.shelfspace.fine.FineStatus.UNPAID AND f.loan.member.id IN :memberIds
            GROUP BY f.loan.member.id
            """)
    List<com.shelfspace.member.MemberFineTotal> unpaidTotalsForMembers(@Param("memberIds") java.util.Collection<Long> memberIds);

    /** Total unpaid fines; zero (not null) when there are none. */
    default BigDecimal totalUnpaidForMember(Long memberId) {
        BigDecimal sum = sumUnpaidForMember(memberId);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
