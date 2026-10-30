package com.shelfspace.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    Optional<BookCopy> findByBarcode(String barcode);

    List<BookCopy> findByBookIdAndStatus(Long bookId, CopyStatus status);

    Optional<BookCopy> findFirstByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookIdAndStatus(Long bookId, CopyStatus status);

    long countByBookId(Long bookId);
}
