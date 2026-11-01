package com.shelfspace.policy.dto;

import com.shelfspace.member.MembershipTier;
import com.shelfspace.policy.BorrowingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PolicyResponse(
        MembershipTier membershipTier,
        int maxBooks,
        int loanDays,
        int maxRenewals,
        BigDecimal finePerDay,
        BigDecimal fineCap,
        BigDecimal maxUnpaidFines,
        int maxReservations,
        int holdHours,
        LocalDateTime updatedAt
) {
    public static PolicyResponse from(BorrowingPolicy p) {
        return new PolicyResponse(p.getMembershipTier(), p.getMaxBooks(), p.getLoanDays(), p.getMaxRenewals(),
                p.getFinePerDay(), p.getFineCap(), p.getMaxUnpaidFines(), p.getMaxReservations(), p.getHoldHours(),
                p.getUpdatedAt());
    }
}
