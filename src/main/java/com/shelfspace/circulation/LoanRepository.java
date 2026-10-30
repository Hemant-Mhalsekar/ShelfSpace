package com.shelfspace.circulation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    boolean existsByMemberIdAndCopyBookIdAndStatusIn(Long memberId, Long bookId, Collection<LoanStatus> statuses);

    Page<Loan> findByMemberIdOrderByRequestedAtDesc(Long memberId, Pageable pageable);

    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

    List<Loan> findByStatusAndDueDate(LoanStatus status, LocalDate date);

    long countByStatus(LoanStatus status);
}
