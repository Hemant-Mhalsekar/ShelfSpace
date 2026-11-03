package com.shelfspace.recommendation;

import com.shelfspace.circulation.Loan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Read-only queries behind "readers also borrowed". All counting happens in MySQL.
 * Soft-deleted books are excluded by Book's @SQLRestriction on every join.
 * Callers always pass at least one id in the NOT IN lists (MySQL rejects an empty IN ()).
 */
public interface RecommendationRepository extends Repository<Loan, Long> {

    /**
     * Books borrowed by members who also borrowed any of {@code bookIds},
     * scored by how many distinct members borrowed both.
     */
    @Query("""
            SELECT b2.id AS bookId, COUNT(DISTINCT l2.member.id) AS score
            FROM Loan l1
            JOIN l1.copy c1
            JOIN c1.book b1
            JOIN Loan l2 ON l2.member.id = l1.member.id
            JOIN l2.copy c2
            JOIN c2.book b2
            WHERE b1.id IN :bookIds
              AND l1.borrowedAt IS NOT NULL
              AND l2.borrowedAt IS NOT NULL
              AND b2.id NOT IN :excludeBookIds
              AND l1.member.id <> :excludeMemberId
            GROUP BY b2.id
            ORDER BY COUNT(DISTINCT l2.member.id) DESC, b2.id DESC
            """)
    List<BookScore> coBorrowed(@Param("bookIds") Collection<Long> bookIds,
                               @Param("excludeBookIds") Collection<Long> excludeBookIds,
                               @Param("excludeMemberId") Long excludeMemberId,
                               Pageable pageable);

    /** Most-borrowed books in the given categories. */
    @Query("""
            SELECT b.id AS bookId, COUNT(l) AS score
            FROM Loan l JOIN l.copy c JOIN c.book b
            WHERE l.borrowedAt IS NOT NULL
              AND b.category IN :categories
              AND b.id NOT IN :excludeBookIds
            GROUP BY b.id
            ORDER BY COUNT(l) DESC, b.id DESC
            """)
    List<BookScore> popularInCategories(@Param("categories") Collection<String> categories,
                                        @Param("excludeBookIds") Collection<Long> excludeBookIds,
                                        Pageable pageable);

    /** Most-borrowed books overall. */
    @Query("""
            SELECT b.id AS bookId, COUNT(l) AS score
            FROM Loan l JOIN l.copy c JOIN c.book b
            WHERE l.borrowedAt IS NOT NULL
              AND b.id NOT IN :excludeBookIds
            GROUP BY b.id
            ORDER BY COUNT(l) DESC, b.id DESC
            """)
    List<BookScore> popularOverall(@Param("excludeBookIds") Collection<Long> excludeBookIds, Pageable pageable);

    /** Every book this member has ever requested or borrowed. */
    @Query("SELECT DISTINCT b.id FROM Loan l JOIN l.copy c JOIN c.book b WHERE l.member.id = :memberId")
    List<Long> bookIdsForMember(@Param("memberId") Long memberId);

    /** Books this member actually borrowed (the basis for "people like you"). */
    @Query("""
            SELECT DISTINCT b.id FROM Loan l JOIN l.copy c JOIN c.book b
            WHERE l.member.id = :memberId AND l.borrowedAt IS NOT NULL
            """)
    List<Long> borrowedBookIdsForMember(@Param("memberId") Long memberId);

    /** The member's favourite categories, most-borrowed first. */
    @Query("""
            SELECT b.category FROM Loan l JOIN l.copy c JOIN c.book b
            WHERE l.member.id = :memberId
            GROUP BY b.category
            ORDER BY COUNT(l) DESC
            """)
    List<String> topCategoriesForMember(@Param("memberId") Long memberId, Pageable pageable);
}
