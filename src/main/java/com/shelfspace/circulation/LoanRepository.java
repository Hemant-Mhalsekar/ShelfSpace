package com.shelfspace.circulation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByMemberIdAndStatusIn(Long memberId, Collection<LoanStatus> statuses);

    boolean existsByMemberIdAndCopyBookIdAndStatusIn(Long memberId, Long bookId, Collection<LoanStatus> statuses);

    /** Any book still out past its due date (whether or not the overdue job has flagged it yet). */
    boolean existsByMemberIdAndStatusInAndDueDateBefore(Long memberId, Collection<LoanStatus> statuses, LocalDate date);

    /** Loads member, copy and book in the same query, so mapping to a response doesn't trigger extra selects. */
    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    Optional<Loan> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    @Query(value = """
            SELECT l FROM Loan l
            WHERE (:memberId IS NULL OR l.member.id = :memberId)
              AND (:status IS NULL OR l.status = :status)
            """,
            countQuery = """
            SELECT COUNT(l) FROM Loan l
            WHERE (:memberId IS NULL OR l.member.id = :memberId)
              AND (:status IS NULL OR l.status = :status)
            """)
    Page<Loan> search(@Param("memberId") Long memberId, @Param("status") LoanStatus status, Pageable pageable);

    /** Due dates of copies currently out, soonest first. Used to estimate when a reserved book frees up. */
    @Query("""
            SELECT l.dueDate FROM Loan l
            WHERE l.copy.book.id = :bookId AND l.status IN :statuses AND l.dueDate IS NOT NULL
            ORDER BY l.dueDate ASC
            """)
    List<LocalDate> findDueDatesForBook(@Param("bookId") Long bookId, @Param("statuses") Collection<LoanStatus> statuses);

    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    List<Loan> findByMemberIdAndStatusInOrderByDueDateAsc(Long memberId, Collection<LoanStatus> statuses);

    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    List<Loan> findTop5ByMemberIdAndStatusInOrderByReturnedAtDesc(Long memberId, Collection<LoanStatus> statuses);

    long countByStatusIn(Collection<LoanStatus> statuses);

    long countByStatusInAndDueDateBefore(Collection<LoanStatus> statuses, LocalDate date);

    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

    @EntityGraph(attributePaths = {"member", "copy", "copy.book"})
    List<Loan> findByStatusAndDueDate(LoanStatus status, LocalDate date);

    long countByStatus(LoanStatus status);
}
