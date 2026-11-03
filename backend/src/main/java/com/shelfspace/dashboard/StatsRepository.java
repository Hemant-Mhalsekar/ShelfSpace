package com.shelfspace.dashboard;

import com.shelfspace.circulation.Loan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only aggregate queries for the admin dashboard. */
public interface StatsRepository extends Repository<Loan, Long> {

    @Query("""
            SELECT b.id AS bookId, b.title AS title, b.author AS author, COUNT(l) AS timesBorrowed
            FROM Loan l JOIN l.copy c JOIN c.book b
            WHERE l.borrowedAt IS NOT NULL
            GROUP BY b.id, b.title, b.author
            ORDER BY COUNT(l) DESC, b.title ASC
            """)
    List<BookBorrowCount> mostBorrowed(Pageable pageable);

    @Query("""
            SELECT YEAR(l.borrowedAt) AS yr, MONTH(l.borrowedAt) AS mon, COUNT(l) AS total
            FROM Loan l
            WHERE l.borrowedAt >= :since
            GROUP BY YEAR(l.borrowedAt), MONTH(l.borrowedAt)
            """)
    List<MonthlyCount> borrowsPerMonth(@Param("since") LocalDateTime since);
}
