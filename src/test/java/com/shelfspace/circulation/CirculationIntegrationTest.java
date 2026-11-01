package com.shelfspace.circulation;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CirculationIntegrationTest extends IntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void fullLifecycleRequestApproveReturn() throws Exception {
        TestMember member = newMember();
        long bookId = newBook(1);

        long loanId = request(member, bookId);
        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.availableCopies").value(0));

        mockMvc.perform(post("/api/v1/loans/{id}/approve", loanId).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(14).toString()))
                .andExpect(jsonPath("$.overdueDays").value(0));

        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loan.status").value("RETURNED"))
                .andExpect(jsonPath("$.overdueDays").value(0))
                .andExpect(jsonPath("$.fineCharged").value(0.0));

        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void lateReturnChargesTenRupeesPerDay() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(3));

        mockMvc.perform(get("/api/v1/loans/{id}", loanId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.overdueDays").value(3))
                .andExpect(jsonPath("$.accruedFine").value(30.0));

        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.overdueDays").value(3))
                .andExpect(jsonPath("$.fineCharged").value(30.0));

        mockMvc.perform(get("/api/v1/fines/me").header("Authorization", member.token()))
                .andExpect(jsonPath("$.totalUnpaid").value(30.0))
                .andExpect(jsonPath("$.fines[0].reason").value("OVERDUE"));
    }

    @Test
    void fineIsCappedAtTierMaximum() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(100));

        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.fineCharged").value(500.0));
    }

    @Test
    void studentCannotBorrowMoreThanFiveBooks() throws Exception {
        TestMember member = newMember();
        for (int i = 0; i < 5; i++) {
            request(member, newBook(1));
        }
        mockMvc.perform(borrow(member, newBook(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("limit")));
    }

    @Test
    void overdueBookBlocksNewBorrowing() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(1));

        mockMvc.perform(borrow(member, newBook(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("overdue")));
    }

    @Test
    void unpaidFinesBlockBorrowingUntilPaid() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(15));   // ₹150 > ₹100 limit
        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()));

        long nextBook = newBook(1);
        mockMvc.perform(borrow(member, nextBook))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Unpaid fines")));

        long fineId = firstFineId(member);
        mockMvc.perform(post("/api/v1/fines/{id}/pay", fineId).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(borrow(member, nextBook)).andExpect(status().isCreated());
    }

    @Test
    void cannotBorrowSameTitleTwice() throws Exception {
        TestMember member = newMember();
        long bookId = newBook(2);
        request(member, bookId);

        mockMvc.perform(borrow(member, bookId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("already have")));
    }

    @Test
    void noAvailableCopies() throws Exception {
        long bookId = newBook(1);
        request(newMember(), bookId);

        mockMvc.perform(borrow(newMember(), bookId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("No copies")));
    }

    @Test
    void renewalExtendsDueDateUntilLimit() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        LocalDate due = LocalDate.now().plusDays(14);

        mockMvc.perform(post("/api/v1/loans/{id}/renew", loanId).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value(due.plusDays(14).toString()))
                .andExpect(jsonPath("$.renewCount").value(1));
        mockMvc.perform(post("/api/v1/loans/{id}/renew", loanId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.renewCount").value(2));
        mockMvc.perform(post("/api/v1/loans/{id}/renew", loanId).header("Authorization", member.token()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Renewal limit")));
    }

    @Test
    void membersCannotSeeOrApproveOthersLoans() throws Exception {
        TestMember owner = newMember();
        TestMember other = newMember();
        long loanId = request(owner, newBook(1));

        mockMvc.perform(get("/api/v1/loans/{id}", loanId).header("Authorization", other.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/loans/{id}/approve", loanId).header("Authorization", owner.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectingRequestFreesTheCopy() throws Exception {
        long bookId = newBook(1);
        long loanId = request(newMember(), bookId);

        mockMvc.perform(post("/api/v1/loans/{id}/reject", loanId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Reference copy\"}"))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(borrow(newMember(), bookId)).andExpect(status().isCreated());
    }

    @Test
    void memberCanCancelOwnRequest() throws Exception {
        TestMember member = newMember();
        long loanId = request(member, newBook(1));

        mockMvc.perform(post("/api/v1/loans/{id}/cancel", loanId).header("Authorization", member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/loans/{id}/approve", loanId).header("Authorization", adminToken()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void lostBookChargesReplacementCost() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));

        mockMvc.perform(post("/api/v1/loans/{id}/lost", loanId).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.loan.status").value("LOST"))
                .andExpect(jsonPath("$.fineCharged").value(450.0));
    }

    @Test
    void waivingAFineRequiresAReason() throws Exception {
        TestMember member = newMember();
        long loanId = issue(member, newBook(1));
        setDueDate(loanId, LocalDate.now().minusDays(2));
        mockMvc.perform(post("/api/v1/loans/{id}/return", loanId).header("Authorization", adminToken()));
        long fineId = firstFineId(member);

        mockMvc.perform(post("/api/v1/fines/{id}/waive", fineId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/fines/{id}/waive", fineId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"Library was closed for 2 days\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAIVED"));

        mockMvc.perform(get("/api/v1/fines/me").header("Authorization", member.token()))
                .andExpect(jsonPath("$.totalUnpaid").value(0));
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.RequestBuilder borrow(TestMember member, long bookId) {
        return post("/api/v1/loans")
                .header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\": " + bookId + "}");
    }

    private long request(TestMember member, long bookId) throws Exception {
        String body = mockMvc.perform(borrow(member, bookId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private long issue(TestMember member, long bookId) throws Exception {
        long loanId = request(member, bookId);
        mockMvc.perform(post("/api/v1/loans/{id}/approve", loanId).header("Authorization", adminToken()))
                .andExpect(status().isOk());
        return loanId;
    }

    /** Simulates time passing by moving the due date into the past. */
    private void setDueDate(long loanId, LocalDate dueDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDueDate(dueDate);
        loanRepository.save(loan);
    }

    private long firstFineId(TestMember member) throws Exception {
        String body = mockMvc.perform(get("/api/v1/fines/me").header("Authorization", member.token()))
                .andReturn().getResponse().getContentAsString();
        JsonNode fines = json.readTree(body).get("fines");
        return fines.get(0).get("id").asLong();
    }
}
