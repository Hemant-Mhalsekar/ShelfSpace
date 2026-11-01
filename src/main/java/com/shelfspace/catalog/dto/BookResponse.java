package com.shelfspace.catalog.dto;

import com.shelfspace.catalog.Book;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        Integer publicationYear,
        BigDecimal replacementCost,
        long totalCopies,
        long availableCopies
) {
    public static BookResponse from(Book b, long total, long available) {
        return new BookResponse(b.getId(), b.getTitle(), b.getAuthor(), b.getIsbn(), b.getCategory(),
                b.getPublicationYear(), b.getReplacementCost(), total, available);
    }
}
