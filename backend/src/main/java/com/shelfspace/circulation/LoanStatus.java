package com.shelfspace.circulation;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Loan lifecycle.
 *
 * <pre>
 * REQUESTED -> APPROVED -> ISSUED -> RETURNED
 *     |            |          |
 *     v            v          v
 *  REJECTED    CANCELLED   OVERDUE -> RETURNED | LOST
 *                             ISSUED -> LOST
 * </pre>
 */
public enum LoanStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    ISSUED,
    OVERDUE,
    RETURNED,
    LOST;

    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED = Map.of(
            REQUESTED, EnumSet.of(APPROVED, REJECTED, CANCELLED),
            APPROVED, EnumSet.of(ISSUED, CANCELLED),
            ISSUED, EnumSet.of(OVERDUE, RETURNED, LOST),
            OVERDUE, EnumSet.of(RETURNED, LOST),
            REJECTED, EnumSet.noneOf(LoanStatus.class),
            CANCELLED, EnumSet.noneOf(LoanStatus.class),
            RETURNED, EnumSet.noneOf(LoanStatus.class),
            LOST, EnumSet.noneOf(LoanStatus.class)
    );

    public boolean canTransitionTo(LoanStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    /** True while the member still holds (or is about to hold) the copy. */
    public boolean isActive() {
        return this == APPROVED || this == ISSUED || this == OVERDUE;
    }
}
