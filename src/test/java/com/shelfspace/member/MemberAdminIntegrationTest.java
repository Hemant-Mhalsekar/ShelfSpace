package com.shelfspace.member;

import com.shelfspace.circulation.Loan;
import com.shelfspace.circulation.LoanRepository;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberAdminIntegrationTest extends IntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Test
    void searchShowsLoanAndFineCounts() throws Exception {
        TestMember member = newMember();
        long overdueLoan = issueLoan(member, newBook(1));
        issueLoan(member, newBook(1));
        setDueDate(overdueLoan, LocalDate.now().minusDays(2));

        mockMvc.perform(get("/api/v1/members").param("q", member.email()).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(member.email()))
                .andExpect(jsonPath("$.content[0].activeLoans").value(2))
                .andExpect(jsonPath("$.content[0].overdueLoans").value(1))
                .andExpect(jsonPath("$.content[0].unpaidFines").value(0));
    }

    @Test
    void membersCannotManageMembers() throws Exception {
        mockMvc.perform(get("/api/v1/members").header("Authorization", newMember().token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesMemberWhoCanThenLogIn() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/members")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(email, "FACULTY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.membershipTier").value("FACULTY"));

        bearer(email, "Welcome123");   // fails the test if login doesn't work

        mockMvc.perform(post("/api/v1/members")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson(email, "STUDENT")))
                .andExpect(status().isConflict());
    }

    @Test
    void suspendedMemberCannotLogInOrBorrowUntilReactivated() throws Exception {
        TestMember member = newMember();
        long bookId = newBook(1);

        mockMvc.perform(post("/api/v1/members/{id}/suspend", member.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Repeated late returns\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + member.email() + "\", \"password\": \"Secret123\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("suspended")));

        // Suspension signs them out: a token issued before it no longer works
        mockMvc.perform(borrowRequest(member, bookId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/members/{id}/activate", member.id()).header("Authorization", adminToken()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        TestMember again = new TestMember(member.id(), member.email(), bearer(member.email(), "Secret123"));
        mockMvc.perform(borrowRequest(again, bookId)).andExpect(status().isCreated());
    }

    @Test
    void suspendNeedsAReason() throws Exception {
        mockMvc.perform(post("/api/v1/members/{id}/suspend", newMember().id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    void adminCannotSuspendThemselves() throws Exception {
        String admin = adminToken();
        long adminId = json.readTree(mockMvc.perform(get("/api/v1/auth/me").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/members/{id}/suspend", adminId)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"test\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void upgradingTierGivesLongerLoans() throws Exception {
        TestMember member = newMember();   // STUDENT: 14 days

        mockMvc.perform(put("/api/v1/members/{id}", member.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test Member\", \"membershipTier\": \"FACULTY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipTier").value("FACULTY"));

        long loanId = issueLoan(member, newBook(1));
        mockMvc.perform(get("/api/v1/loans/{id}", loanId).header("Authorization", member.token()))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(30).toString()));
    }

    @Test
    void memberProfileShowsCurrentLoans() throws Exception {
        TestMember member = newMember();
        issueLoan(member, newBook(1));

        mockMvc.perform(get("/api/v1/members/{id}", member.id()).header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.email").value(member.email()))
                .andExpect(jsonPath("$.currentLoans.length()").value(1))
                .andExpect(jsonPath("$.currentLoans[0].status").value("ISSUED"));
    }

    private void setDueDate(long loanId, LocalDate dueDate) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDueDate(dueDate);
        loanRepository.save(loan);
    }

    private static String uniqueEmail() {
        return "desk-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String createJson(String email, String tier) {
        return """
                {"name": "Desk Signup", "email": "%s", "phone": "9876543210",
                 "password": "Welcome123", "membershipTier": "%s"}
                """.formatted(email, tier);
    }
}
