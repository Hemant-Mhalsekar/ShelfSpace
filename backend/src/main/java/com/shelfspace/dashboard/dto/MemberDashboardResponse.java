package com.shelfspace.dashboard.dto;

import com.shelfspace.circulation.dto.LoanResponse;
import com.shelfspace.reservation.dto.ReservationResponse;

import java.math.BigDecimal;
import java.util.List;

public record MemberDashboardResponse(
        Summary summary,
        List<LoanResponse> currentLoans,
        List<ReservationResponse> reservations,
        List<LoanResponse> recentlyReturned
) {
    public record Summary(
            int booksOut,
            int booksAllowed,
            int dueWithin3Days,
            int overdue,
            BigDecimal unpaidFines,
            long unreadNotifications
    ) {
    }
}
