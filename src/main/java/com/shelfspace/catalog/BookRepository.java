package com.shelfspace.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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
            """)
    Page<Book> search(@Param("q") String q, @Param("category") String category, Pageable pageable);
}
