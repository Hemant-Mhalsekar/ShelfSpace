package com.shelfspace.circulation;

import com.shelfspace.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        assertThat(LoanStatus.REQUESTED.canTransitionTo(LoanStatus.APPROVED)).isTrue();
        assertThat(LoanStatus.APPROVED.canTransitionTo(LoanStatus.ISSUED)).isTrue();
        assertThat(LoanStatus.ISSUED.canTransitionTo(LoanStatus.OVERDUE)).isTrue();
        assertThat(LoanStatus.OVERDUE.canTransitionTo(LoanStatus.RETURNED)).isTrue();
    }

    @Test
    void finishedLoansCannotChange() {
        for (LoanStatus next : LoanStatus.values()) {
            assertThat(LoanStatus.RETURNED.canTransitionTo(next)).isFalse();
            assertThat(LoanStatus.REJECTED.canTransitionTo(next)).isFalse();
        }
    }

    @Test
    void loanRejectsInvalidJump() {
        Loan loan = new Loan();
        assertThatThrownBy(() -> loan.transitionTo(LoanStatus.RETURNED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REQUESTED to RETURNED");
    }
}
