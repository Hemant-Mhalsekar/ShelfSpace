package com.shelfspace.policy;

import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Only touches the GUEST tier (other tests use STUDENT and FACULTY) and restores it afterwards. */
class PolicyIntegrationTest extends IntegrationTest {

    private static final String GUEST_DEFAULTS = policyJson(2, 7, 0, "20.00", "500.00", "50.00", 3, 48);

    @AfterEach
    void restoreGuestPolicy() throws Exception {
        mockMvc.perform(put("/api/v1/policies/GUEST")
                .header("Authorization", adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(GUEST_DEFAULTS));
    }

    @Test
    void anyMemberCanReadTheRules() throws Exception {
        mockMvc.perform(get("/api/v1/policies").header("Authorization", newMember().token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].membershipTier").value("STUDENT"))
                .andExpect(jsonPath("$[0].maxBooks").value(5))
                .andExpect(jsonPath("$[0].loanDays").value(14));
    }

    @Test
    void membersCannotChangeTheRules() throws Exception {
        mockMvc.perform(put("/api/v1/policies/GUEST")
                        .header("Authorization", newMember().token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GUEST_DEFAULTS))
                .andExpect(status().isForbidden());
    }

    @Test
    void newLoansUseTheUpdatedRule() throws Exception {
        mockMvc.perform(put("/api/v1/policies/GUEST")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson(2, 21, 0, "20.00", "500.00", "50.00", 3, 48)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanDays").value(21));

        TestMember guest = newGuest();
        long loanId = issueLoan(guest, newBook(1));
        mockMvc.perform(get("/api/v1/loans/{id}", loanId).header("Authorization", guest.token()))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(21).toString()));
    }

    @Test
    void fineCapCannotBeBelowDailyFine() throws Exception {
        mockMvc.perform(put("/api/v1/policies/GUEST")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson(2, 7, 0, "20.00", "10.00", "50.00", 3, 48)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void outOfRangeValuesAreRejected() throws Exception {
        mockMvc.perform(put("/api/v1/policies/GUEST")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson(2, 0, 0, "20.00", "500.00", "50.00", 3, 48)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.loanDays").exists());
    }

    private TestMember newGuest() throws Exception {
        String email = "guest-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        mockMvc.perform(post("/api/v1/members")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Guest", "email": "%s", "password": "Visit0rPass", "membershipTier": "GUEST"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        String token = bearer(email, "Visit0rPass");
        long id = json.readTree(mockMvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        return new TestMember(id, email, token);
    }

    private static String policyJson(int maxBooks, int loanDays, int maxRenewals, String finePerDay, String fineCap,
                                     String maxUnpaid, int maxReservations, int holdHours) {
        return """
                {"maxBooks": %d, "loanDays": %d, "maxRenewals": %d, "finePerDay": %s, "fineCap": %s,
                 "maxUnpaidFines": %s, "maxReservations": %d, "holdHours": %d}
                """.formatted(maxBooks, loanDays, maxRenewals, finePerDay, fineCap, maxUnpaid, maxReservations, holdHours);
    }
}
