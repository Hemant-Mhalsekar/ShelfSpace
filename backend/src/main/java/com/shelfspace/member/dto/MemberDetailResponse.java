package com.shelfspace.member.dto;

import com.shelfspace.circulation.dto.LoanResponse;
import com.shelfspace.reservation.dto.ReservationResponse;

import java.util.List;

public record MemberDetailResponse(
        MemberSummaryResponse member,
        List<LoanResponse> currentLoans,
        List<ReservationResponse> reservations,
        List<LoanResponse> recentHistory
) {
}
