package com.shelfspace.dashboard;

import com.shelfspace.circulation.Loan;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends IntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void memberDashboardSummarisesTheirLoans() throws Exception {
        TestMember member = newMember();
        requestLoan(member, newBook(1));                       // pending request
        long dueSoon = issueLoan(member, newBook(1));
        long late = issueLoan(member, newBook(1));
        setDueDate(dueSoon, LocalDate.now().plusDays(2));
        setDueDate(late, LocalDate.now().minusDays(1));

        mockMvc.perform(get("/api/v1/dashboard/me").header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.booksOut").value(2))
                .andExpect(jsonPath("$.summary.booksAllowed").value(5))
                .andExpect(jsonPath("$.summary.dueWithin3Days").value(1))
                .andExpect(jsonPath("$.summary.overdue").value(1))
                .andExpect(jsonPath("$.summary.unreadNotifications").value(2))
                .andExpect(jsonPath("$.currentLoans.length()").value(3))
                .andExpect(jsonPath("$.reservations.length()").value(0));
    }

    @Test
    void adminDashboardShowsMostBorrowedAndMonthlyStats() throws Exception {
        long popular = newBook(1);
        for (int i = 0; i < 3; i++) {
            returnLoan(issueLoan(newMember(), popular));
        }

        mockMvc.perform(get("/api/v1/dashboard/admin").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mostBorrowed[0].bookId").value(popular))
                .andExpect(jsonPath("$.mostBorrowed[0].timesBorrowed").value(3))
                .andExpect(jsonPath("$.borrowsPerMonth.length()").value(6))
                .andExpect(jsonPath("$.borrowsPerMonth[5].month").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.inventory.titles").isNumber())
                .andExpect(jsonPath("$.circulation.overdueNow").isNumber())
                .andExpect(jsonPath("$.fines.unpaidTotal").isNumber());
    }

    @Test
    void membersCannotOpenTheAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin").header("Authorization", newMember().token()))
                .andExpect(status().isForbidden());
    }

    private void setDueDate(long loanId, LocalDate dueDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDueDate(dueDate);
        loanRepository.save(loan);
    }
}
