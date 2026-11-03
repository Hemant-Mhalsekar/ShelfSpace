package com.shelfspace.circulation.dto;

import java.math.BigDecimal;

/** Result of a return or lost report: the closed loan and what was charged. */
public record ReturnResponse(LoanResponse loan, long overdueDays, BigDecimal fineCharged) {
}
