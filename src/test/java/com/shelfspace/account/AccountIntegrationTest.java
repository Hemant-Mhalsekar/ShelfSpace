package com.shelfspace.account;

import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountIntegrationTest extends IntegrationTest {

    private void login(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().is(expectedStatus));
    }

    private org.springframework.test.web.servlet.RequestBuilder changePassword(String token, String current, String next) {
        return post("/api/v1/account/password")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(current, next));
    }

    @Test
    void memberUpdatesOwnProfile() throws Exception {
        var m = newMember();
        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  Asha Rao  ", "phone": "+919876543210"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asha Rao"))
                .andExpect(jsonPath("$.phone").value("+919876543210"))
                .andExpect(jsonPath("$.email").value(m.email()));

        // A blank phone clears it.
        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Asha Rao", "phone": ""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void invalidProfileIsRejected() throws Exception {
        var m = newMember();
        mockMvc.perform(put("/api/v1/account/profile")
                        .header("Authorization", m.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "phone": "12ab"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void afterChangingPasswordOnlyTheNewOneWorks() throws Exception {
        var m = newMember();
        mockMvc.perform(changePassword(m.token(), "Secret123", "Brand1New"))
                .andExpect(status().isOk());

        login(m.email(), "Secret123", 401);
        login(m.email(), "Brand1New", 200);
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        var m = newMember();
        mockMvc.perform(changePassword(m.token(), "Wrong1234", "Brand1New"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Your current password is incorrect"));
        login(m.email(), "Secret123", 200);
    }

    @Test
    void reusingTheSamePasswordIsRejected() throws Exception {
        var m = newMember();
        mockMvc.perform(changePassword(m.token(), "Secret123", "Secret123"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void weakNewPasswordFailsValidation() throws Exception {
        var m = newMember();
        mockMvc.perform(changePassword(m.token(), "Secret123", "short"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void librarianResetLetsMemberLogIn() throws Exception {
        var m = newMember();
        mockMvc.perform(post("/api/v1/members/{id}/reset-password", m.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword": "Desk2024x"}
                                """))
                .andExpect(status().isNoContent());

        login(m.email(), "Secret123", 401);
        login(m.email(), "Desk2024x", 200);
    }

    @Test
    void memberCannotResetSomeoneElsesPassword() throws Exception {
        var a = newMember();
        var b = newMember();
        mockMvc.perform(post("/api/v1/members/{id}/reset-password", b.id())
                        .header("Authorization", a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword": "Hacked123"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountEndpointsNeedLogin() throws Exception {
        mockMvc.perform(put("/api/v1/account/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
