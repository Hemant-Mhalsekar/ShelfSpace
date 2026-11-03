package com.shelfspace.circulation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FineCalculatorTest {

    private static final LocalDate DUE = LocalDate.of(2026, 6, 15);
    private static final BigDecimal PER_DAY = new BigDecimal("10.00");
    private static final BigDecimal CAP = new BigDecimal("500.00");

    @Test
    void notOverdueOnOrBeforeDueDate() {
        assertThat(FineCalculator.overdueDays(DUE, DUE)).isZero();
        assertThat(FineCalculator.overdueDays(DUE, DUE.minusDays(5))).isZero();
    }

    @Test
    void countsWholeDaysLate() {
        assertThat(FineCalculator.overdueDays(DUE, LocalDate.of(2026, 6, 18))).isEqualTo(3);
    }

    @Test
    void noDueDateMeansNoFine() {
        assertThat(FineCalculator.overdueDays(null, DUE)).isZero();
    }

    @Test
    void threeDaysAtTenRupeesIsThirty() {
        assertThat(FineCalculator.overdueFine(3, PER_DAY, CAP)).isEqualByComparingTo("30.00");
    }

    @Test
    void fineIsCapped() {
        assertThat(FineCalculator.overdueFine(100, PER_DAY, CAP)).isEqualByComparingTo("500.00");
    }

    @Test
    void zeroDaysIsZeroFine() {
        assertThat(FineCalculator.overdueFine(0, PER_DAY, CAP)).isEqualByComparingTo("0.00");
    }
}
