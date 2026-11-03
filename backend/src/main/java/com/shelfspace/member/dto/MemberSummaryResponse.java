package com.shelfspace.member.dto;

import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row in the admin member list. */
public record MemberSummaryResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        MembershipTier membershipTier,
        MemberStatus status,
        LocalDateTime createdAt,
        long activeLoans,
        long overdueLoans,
        BigDecimal unpaidFines
) {
    public static MemberSummaryResponse from(User u, long active, long overdue, BigDecimal unpaid) {
        return new MemberSummaryResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
                u.getMembershipTier(), u.getStatus(), u.getCreatedAt(), active, overdue, unpaid);
    }
}
