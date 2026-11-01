package com.shelfspace.circulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CirculationJobsIntegrationTest extends IntegrationTest {

    @Autowired
    private CirculationJobs jobs;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void overdueJobFlagsLateLoansAndNotifies() throws Exception {
        TestMember member = newMember();
        long loanId = issueLoan(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(2));

        assertThat(jobs.markOverdueLoans()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/v1/loans/{id}", loanId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.status").value("OVERDUE"))
                .andExpect(jsonPath("$.overdueDays").value(2));
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", member.token()))
                .andExpect(jsonPath("$.content[*].type", hasItem("OVERDUE")));

        // An overdue loan can still be returned, and is fined
        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fineCharged").value(20.0));
    }

    @Test
    void dueTomorrowReminderIsSentOnlyOncePerDay() throws Exception {
        TestMember member = newMember();
        long loanId = issueLoan(member, newBook(1));
        setDueDate(loanId, LocalDate.now().plusDays(1));

        jobs.sendDueTomorrowReminders();
        jobs.sendDueTomorrowReminders();   // e.g. the app restarted and the job ran again

        String body = mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", member.token()))
                .andReturn().getResponse().getContentAsString();
        long reminders = 0;
        for (JsonNode n : json.readTree(body).get("content")) {
            if ("DUE_SOON".equals(n.get("type").asText())) {
                reminders++;
            }
        }
        assertThat(reminders).isEqualTo(1);
    }

    @Test
    void notificationsCanBeMarkedRead() throws Exception {
        TestMember member = newMember();
        issueLoan(member, newBook(1));   // approval sends a notification

        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", member.token()))
                .andExpect(jsonPath("$.unread").value(1));
        mockMvc.perform(post("/api/v1/notifications/me/read-all").header("Authorization", member.token()))
                .andExpect(jsonPath("$.updated").value(1));
        mockMvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", member.token()))
                .andExpect(jsonPath("$.unread").value(0));
    }

    private void setDueDate(long loanId, LocalDate dueDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDueDate(dueDate);
        loanRepository.save(loan);
    }
}
