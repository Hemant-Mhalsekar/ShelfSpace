package com.shelfspace.common.validation;

import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Year;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Each field rule, checked through the real endpoints. */
class FieldValidationIntegrationTest extends IntegrationTest {

    private ResultActions register(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "email": "%s", "password": "%s"}
                        """.formatted(name, email, password)));
    }

    private static String email() {
        return "v-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void commonPasswordsAreRefusedAtSignUp() throws Exception {
        register("Ananya Iyer", email(), "Password123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password", containsString("too common")));
    }

    @Test
    void passwordsNeedMixedCase() throws Exception {
        register("Ananya Iyer", email(), "shelf2read")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("needs both an uppercase and a lowercase letter"));
    }

    @Test
    void passwordsCantContainYourName() throws Exception {
        register("Ananya Iyer", email(), "Ananya2026x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("must not contain your name or email"));
    }

    @Test
    void namesMustLookLikeNames() throws Exception {
        register("12345", email(), "Shelf2Read").andExpect(jsonPath("$.fieldErrors.name").exists());
        register("A", email(), "Shelf2Read").andExpect(jsonPath("$.fieldErrors.name").exists());
        // Accents, apostrophes and hyphens are fine
        register("Zoë O'Brien-Nair", email(), "Shelf2Read").andExpect(status().isCreated());
    }

    @Test
    void emailsNeedAProperDomain() throws Exception {
        register("Ananya Iyer", "ananya@gmail", "Shelf2Read")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").value("must be a valid email address, like name@example.com"));
    }

    @Test
    void changingToAPasswordWithYourNameIsRefused() throws Exception {
        var m = newMember();   // "Test Member"
        mockMvc.perform(post("/api/v1/account/password")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Shelf2Read\", \"newPassword\": \"Member2026x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The new password must not contain your name or email"));
    }

    @Test
    void librarianResetFollowsTheSameRules() throws Exception {
        var m = newMember();
        mockMvc.perform(post("/api/v1/members/{id}/reset-password", m.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"Welcome123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword", containsString("too common")));
    }

    @Test
    void profileNameIsChecked() throws Exception {
        var m = newMember();
        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"!!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void booksCantBeFromTheFarFutureOrHaveNumberOnlyAuthors() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "1984", "author": "123", "isbn": "%s", "category": "Fiction",
                                 "publicationYear": %d, "replacementCost": 2000000, "initialCopies": 1}
                                """.formatted(randomIsbn13(), Year.now().getValue() + 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.author").value("must contain a letter"))
                .andExpect(jsonPath("$.fieldErrors.publicationYear").value("can't be later than next year"))
                .andExpect(jsonPath("$.fieldErrors.replacementCost").value("must be at most 1000000"))
                .andExpect(jsonPath("$.fieldErrors.title").doesNotExist());   // "1984" is a fine title
    }
}
