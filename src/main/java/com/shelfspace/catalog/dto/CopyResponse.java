package com.shelfspace.catalog.dto;

import com.shelfspace.catalog.BookCopy;
import com.shelfspace.catalog.CopyStatus;

import java.time.LocalDateTime;

public record CopyResponse(Long id, Long bookId, String barcode, CopyStatus status, LocalDateTime updatedAt) {

    public static CopyResponse from(BookCopy c) {
        return new CopyResponse(c.getId(), c.getBook().getId(), c.getBarcode(), c.getStatus(), c.getUpdatedAt());
    }
}
