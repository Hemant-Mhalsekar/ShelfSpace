package com.shelfspace.fine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByLoanMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("""
            SELECT COALESCE(SUM(f.amount), 0) FROM Fine f
            WHERE f.loan.member.id = :memberId AND f.status = com.shelfspace.fine.FineStatus.UNPAID
            """)
    BigDecimal totalUnpaidForMember(@Param("memberId") Long memberId);
}
