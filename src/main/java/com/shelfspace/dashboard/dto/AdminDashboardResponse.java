package com.shelfspace.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        Inventory inventory,
        Circulation circulation,
        Fines fines,
        long activeMembers,
        List<TopBook> mostBorrowed,
        List<MonthStat> borrowsPerMonth
) {
    public record Inventory(long titles, long copies, long availableCopies, long damagedCopies, long lostCopies) {
    }

    public record Circulation(long borrowedNow, long overdueNow, long pendingRequests, long membersWaiting) {
    }

    public record Fines(BigDecimal unpaidTotal, BigDecimal collectedThisMonth) {
    }

    public record TopBook(Long bookId, String title, String author, long timesBorrowed) {
    }

    /** month is "2026-11". Months with no borrowing are included with 0. */
    public record MonthStat(String month, long borrowed) {
    }
}
