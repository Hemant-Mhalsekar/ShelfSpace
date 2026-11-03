package com.shelfspace.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.circulation.Loan;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationIntegrationTest extends IntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void cannotReserveWhileACopyIsAvailable() throws Exception {
        mockMvc.perform(reserveRequest(newMember(), newBook(1)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("borrow it directly")));
    }

    @Test
    void queueIsFirstComeFirstServed() throws Exception {
        long bookId = newBook(1);
        TestMember holder = newMember();
        TestMember first = newMember();
        TestMember second = newMember();
        TestMember outsider = newMember();
        long loanId = issueLoan(holder, bookId);

        long firstRes = reserve(first, bookId);
        long secondRes = reserve(second, bookId);

        mockMvc.perform(get("/api/v1/reservations/{id}", firstRes).header("Authorization", first.token()))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.estimatedAvailableDate").value(LocalDate.now().plusDays(14).toString()));
        mockMvc.perform(get("/api/v1/reservations/{id}", secondRes).header("Authorization", second.token()))
                .andExpect(jsonPath("$.position").value(2));

        returnLoan(loanId);

        // First in line now has the copy held for them
        mockMvc.perform(get("/api/v1/reservations/{id}", firstRes).header("Authorization", first.token()))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.heldCopyBarcode", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()));
        mockMvc.perform(get("/api/v1/reservations/{id}", secondRes).header("Authorization", second.token()))
                .andExpect(jsonPath("$.position").value(1));
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", first.token()))
                .andExpect(jsonPath("$.content[*].type", hasItem("RESERVATION_READY")));

        // Someone not in the queue can't take it
        mockMvc.perform(borrowRequest(outsider, bookId))
                .andExpect(status().isUnprocessableEntity());

        // The member it's held for can
        mockMvc.perform(borrowRequest(first, bookId)).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/reservations/{id}", firstRes).header("Authorization", first.token()))
                .andExpect(jsonPath("$.status").value("FULFILLED"));
    }

    @Test
    void expiredHoldPassesToNextMember() throws Exception {
        long bookId = newBook(1);
        long loanId = issueLoan(newMember(), bookId);
        TestMember first = newMember();
        TestMember second = newMember();
        long firstRes = reserve(first, bookId);
        long secondRes = reserve(second, bookId);
        returnLoan(loanId);

        Reservation held = reservationRepository.findById(firstRes).orElseThrow();
        held.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        reservationRepository.save(held);

        assertThat(reservationService.expireHolds()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/v1/reservations/{id}", firstRes).header("Authorization", first.token()))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        mockMvc.perform(get("/api/v1/reservations/{id}", secondRes).header("Authorization", second.token()))
                .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(get("/api/v1/notifications/me").header("Authorization", first.token()))
                .andExpect(jsonPath("$.content[*].type", hasItem("HOLD_EXPIRED")));
    }

    @Test
    void cancellingAHeldReservationPassesTheCopyOn() throws Exception {
        long bookId = newBook(1);
        long loanId = issueLoan(newMember(), bookId);
        TestMember first = newMember();
        TestMember second = newMember();
        long firstRes = reserve(first, bookId);
        long secondRes = reserve(second, bookId);
        returnLoan(loanId);

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", firstRes).header("Authorization", first.token()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/v1/reservations/{id}", secondRes).header("Authorization", second.token()))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void lastCancellationPutsCopyBackOnShelf() throws Exception {
        long bookId = newBook(1);
        long loanId = issueLoan(newMember(), bookId);
        TestMember waiting = newMember();
        long res = reserve(waiting, bookId);
        returnLoan(loanId);

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", res).header("Authorization", waiting.token()));
        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", waiting.token()))
                .andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void newCopiesGoToTheQueueFirst() throws Exception {
        long bookId = newBook(1);
        issueLoan(newMember(), bookId);
        TestMember waiting = newMember();
        long res = reserve(waiting, bookId);

        mockMvc.perform(post("/api/v1/books/{id}/copies", bookId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("ON_HOLD"));

        mockMvc.perform(get("/api/v1/reservations/{id}", res).header("Authorization", waiting.token()))
                .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(get("/api/v1/books/{id}", bookId).header("Authorization", waiting.token()))
                .andExpect(jsonPath("$.availableCopies").value(0));
    }

    @Test
    void duplicateAndPointlessReservationsAreRejected() throws Exception {
        long bookId = newBook(1);
        TestMember holder = newMember();
        issueLoan(holder, bookId);
        TestMember waiting = newMember();
        reserve(waiting, bookId);

        mockMvc.perform(reserveRequest(waiting, bookId)).andExpect(status().isConflict());
        mockMvc.perform(reserveRequest(holder, bookId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("already have")));
    }

    @Test
    void cannotRenewWhileSomeoneIsWaiting() throws Exception {
        long bookId = newBook(1);
        TestMember holder = newMember();
        long loanId = issueLoan(holder, bookId);
        reserve(newMember(), bookId);

        mockMvc.perform(post("/api/v1/loans/{id}/renew", loanId).header("Authorization", holder.token()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("waiting")));
    }

    @Test
    void membersCannotSeeOthersReservations() throws Exception {
        long bookId = newBook(1);
        issueLoan(newMember(), bookId);
        long res = reserve(newMember(), bookId);

        mockMvc.perform(get("/api/v1/reservations/{id}", res).header("Authorization", newMember().token()))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.RequestBuilder reserveRequest(TestMember member, long bookId) {
        return post("/api/v1/reservations")
                .header("Authorization", member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\": " + bookId + "}");
    }

    private long reserve(TestMember member, long bookId) throws Exception {
        String body = mockMvc.perform(reserveRequest(member, bookId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asLong();
    }
}
