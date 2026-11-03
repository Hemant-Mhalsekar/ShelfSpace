package com.shelfspace.circulation.dto;

import com.shelfspace.circulation.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * overdueDays and accruedFine are live estimates for loans still out (ISSUED/OVERDUE); null otherwise.
 * Charged fines are in the /fines endpoints.
 */
public record LoanResponse(
        Long id,
        LoanStatus status,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        String bookIsbn,
        String copyBarcode,
        Long memberId,
        String memberName,
        LocalDateTime requestedAt,
        LocalDateTime borrowedAt,
        LocalDate dueDate,
        LocalDateTime returnedAt,
        int renewCount,
        Long overdueDays,
        BigDecimal accruedFine
) {
}
