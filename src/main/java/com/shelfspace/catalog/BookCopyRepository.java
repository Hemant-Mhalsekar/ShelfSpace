package com.shelfspace.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookIdOrderByIdAsc(Long bookId);

    List<BookCopy> findByBookIdAndStatus(Long bookId, CopyStatus status);

    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookId(Long bookId);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<CopyStatus> statuses);

    /** Total and available copies for many books in ONE query (avoids N+1 on search pages). */
    @Query("""
            SELECT c.book.id AS bookId,
                   COUNT(c) AS total,
                   SUM(CASE WHEN c.status = com.shelfspace.catalog.CopyStatus.AVAILABLE THEN 1 ELSE 0 END) AS available
            FROM BookCopy c
            WHERE c.book.id IN :bookIds
            GROUP BY c.book.id
            """)
    List<CopyCounts> countByBookIds(@Param("bookIds") Collection<Long> bookIds);
}
