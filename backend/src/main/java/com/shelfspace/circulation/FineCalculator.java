package com.shelfspace.circulation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Pure fine maths, kept separate so it can be unit-tested without Spring or a database. */
public final class FineCalculator {

    private FineCalculator() {
    }

    /** Whole days past the due date on the given day. 0 if not overdue or no due date. */
    public static long overdueDays(LocalDate dueDate, LocalDate onDate) {
        if (dueDate == null || onDate == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, onDate));
    }

    /** overdueDays x finePerDay, never more than the cap. */
    public static BigDecimal overdueFine(long overdueDays, BigDecimal finePerDay, BigDecimal cap) {
        if (overdueDays <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        BigDecimal fine = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
        return fine.min(cap).setScale(2, RoundingMode.HALF_UP);
    }
}
