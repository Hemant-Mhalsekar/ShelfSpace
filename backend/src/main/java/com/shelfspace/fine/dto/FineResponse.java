package com.shelfspace.fine.dto;

import com.shelfspace.fine.Fine;
import com.shelfspace.fine.FineReason;
import com.shelfspace.fine.FineStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FineResponse(
        Long id,
        Long loanId,
        Long memberId,
        String bookTitle,
        BigDecimal amount,
        FineReason reason,
        FineStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        String note
) {
    public static FineResponse from(Fine f) {
        var loan = f.getLoan();
        var book = loan.getCopy().getBook();
        return new FineResponse(f.getId(), loan.getId(), loan.getMember().getId(),
                book == null ? "(removed)" : book.getTitle(),
                f.getAmount(), f.getReason(), f.getStatus(), f.getCreatedAt(), f.getPaidAt(), f.getNote());
    }
}
