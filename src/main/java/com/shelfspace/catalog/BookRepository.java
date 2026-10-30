package com.shelfspace.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Soft-deleted books are hidden automatically by @SQLRestriction on {@link Book}. */
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("""
            SELECT b FROM Book b
            WHERE (:q IS NULL
                   OR LOWER(b.title)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR b.isbn = :q)
              AND (:category IS NULL OR b.category = :category)
              AND (:availableOnly = false OR EXISTS (
                     SELECT 1 FROM BookCopy c
                     WHERE c.book = b AND c.status = com.shelfspace.catalog.CopyStatus.AVAILABLE))
            """)
    Page<Book> search(@Param("q") String q,
                      @Param("category") String category,
                      @Param("availableOnly") boolean availableOnly,
                      Pageable pageable);

    @Query("SELECT DISTINCT b.category FROM Book b ORDER BY b.category")
    List<String> findAllCategories();
}
