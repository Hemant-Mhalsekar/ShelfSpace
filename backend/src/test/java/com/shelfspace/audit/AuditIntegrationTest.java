package com.shelfspace.audit;

import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditIntegrationTest extends IntegrationTest {

    @Test
    void loanEntriesShowWhoAndWhatByName() throws Exception {
        var m = newMember();
        long bookId = newBook(1);
        long loanId = issueLoan(m, bookId);

        mockMvc.perform(get("/api/v1/audit")
                        .param("category", "LOANS").param("entityId", String.valueOf(loanId))
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("LOAN_APPROVED"))
                .andExpect(jsonPath("$.content[0].actorName").value("Library Admin"))
                .andExpect(jsonPath("$.content[0].entityLabel", containsString("Test Member")))
                .andExpect(jsonPath("$.content[0].bookId").value(bookId))
                .andExpect(jsonPath("$.content[0].memberId").value(m.id()));
    }

    @Test
    void deletedBooksKeepTheirTitle() throws Exception {
        long bookId = newBook(1);
        mockMvc.perform(delete("/api/v1/books/{id}", bookId).header("Authorization", adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/audit")
                        .param("entityType", "Book").param("entityId", String.valueOf(bookId))
                        .header("Authorization", adminToken()))
                .andExpect(jsonPath("$.content[0].action").value("BOOK_DELETED"))
                .andExpect(jsonPath("$.content[0].entityLabel", containsString("Book ")))
                .andExpect(jsonPath("$.content[1].action").value("BOOK_CREATED"));
    }

    @Test
    void filterByWhoDidIt() throws Exception {
        var m = newMember();
        issueLoan(m, newBook(1));   // done by the librarian, so not counted below
        mockMvc.perform(post("/api/v1/account/password")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Shelf2Read\", \"newPassword\": \"Brand1New\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit").param("actorId", String.valueOf(m.id())).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("PASSWORD_CHANGED"))
                .andExpect(jsonPath("$.content[*].actorId", everyItem(is((int) m.id()))));
    }

    @Test
    void membersRenewingTheirOwnLoanAreRecorded() throws Exception {
        var m = newMember();
        long loanId = issueLoan(m, newBook(1));
        mockMvc.perform(post("/api/v1/loans/{id}/renew", loanId).header("Authorization", m.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit").param("actorId", String.valueOf(m.id())).param("action", "LOAN_RENEWED")
                        .header("Authorization", adminToken()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actorName").value("Test Member"))
                .andExpect(jsonPath("$.content[0].memberId").value(m.id()))
                .andExpect(jsonPath("$.content[0].details", containsString("newDueDate=")));
    }

    @Test
    void dateRangeFilters() throws Exception {
        long bookId = newBook(1);
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/v1/audit").param("entityType", "Book").param("entityId", String.valueOf(bookId))
                        .param("from", today).param("to", today).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/audit").param("entityType", "Book").param("entityId", String.valueOf(bookId))
                        .param("from", tomorrow).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/audit").param("from", tomorrow).param("to", today).header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCategoryIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit").param("category", "NOPE").header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actionsListForTheFilterMenu() throws Exception {
        newBook(1);
        mockMvc.perform(get("/api/v1/audit/actions").header("Authorization", adminToken()))
                .andExpect(jsonPath("$", hasItem("BOOK_CREATED")));
    }

    @Test
    void exportIsCsvAndNeutralisesFormulas() throws Exception {
        var m = newMember();
        long loanId = requestLoan(m, newBook(1));
        mockMvc.perform(post("/api/v1/loans/{id}/reject", loanId)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"=HYPERLINK(\\\"http://evil\\\")\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit/export").param("category", "LOANS").param("entityId", String.valueOf(loanId))
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("time,who,action,type,id,about,details")))
                .andExpect(content().string(containsString("\"LOAN_REJECTED\"")))
                .andExpect(content().string(containsString("\"'=HYPERLINK(\"\"http://evil\"\")\"")));
    }

    @Test
    void membersCannotReadTheLog() throws Exception {
        var m = newMember();
        mockMvc.perform(get("/api/v1/audit").header("Authorization", m.token())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit/export").header("Authorization", m.token())).andExpect(status().isForbidden());
    }
}
