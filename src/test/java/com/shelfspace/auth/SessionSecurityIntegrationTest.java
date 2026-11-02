package com.shelfspace.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelfspace.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Token rotation, revocation and sign-in lockout. */
class SessionSecurityIntegrationTest extends IntegrationTest {

    // ---------- helpers ----------

    private ResultActions login(String email, String password, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    /** Logs in and returns the full token pair. */
    private JsonNode signIn(String email) throws Exception {
        return json.readTree(login(email, "Secret123", "10.0.0.1")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private ResultActions me(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken));
    }

    // ---------- refresh token rotation ----------

    @Test
    void refreshTokenWorksOnlyOnce() throws Exception {
        var m = newMember();
        JsonNode first = signIn(m.email());

        JsonNode second = body(refresh(first.get("refreshToken").asText()).andExpect(status().isOk()));
        refresh(second.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void reusingARefreshTokenRevokesThatSignIn() throws Exception {
        var m = newMember();
        JsonNode stolen = signIn(m.email());
        JsonNode legit = body(refresh(stolen.get("refreshToken").asText()).andExpect(status().isOk()));

        // The copied token comes back: rejected, and the whole family is revoked
        refresh(stolen.get("refreshToken").asText()).andExpect(status().isUnauthorized());
        refresh(legit.get("refreshToken").asText()).andExpect(status().isUnauthorized());

        // Other sign-ins are untouched
        JsonNode other = signIn(m.email());
        refresh(other.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void logoutEndsThatSignInOnly() throws Exception {
        var m = newMember();
        JsonNode phone = signIn(m.email());
        JsonNode laptop = signIn(m.email());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + phone.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());

        refresh(phone.get("refreshToken").asText()).andExpect(status().isUnauthorized());
        refresh(laptop.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void logoutWithGarbageTokenIsHarmless() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"not-a-token\"}"))
                .andExpect(status().isNoContent());
    }

    // ---------- signing out everywhere ----------

    @Test
    void changingPasswordSignsOutOtherDevicesButKeepsThisOne() throws Exception {
        var m = newMember();
        JsonNode otherDevice = signIn(m.email());
        JsonNode thisDevice = signIn(m.email());

        JsonNode fresh = body(mockMvc.perform(post("/api/v1/account/password")
                        .header("Authorization", "Bearer " + thisDevice.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"Secret123\", \"newPassword\": \"Brand1New\"}"))
                .andExpect(status().isOk()));

        me(otherDevice.get("accessToken").asText()).andExpect(status().isUnauthorized());
        refresh(otherDevice.get("refreshToken").asText()).andExpect(status().isUnauthorized());
        me(thisDevice.get("accessToken").asText()).andExpect(status().isUnauthorized());
        me(fresh.get("accessToken").asText()).andExpect(status().isOk());
        refresh(fresh.get("refreshToken").asText()).andExpect(status().isOk());
    }

    @Test
    void signOutEverywhereKillsOldTokens() throws Exception {
        var m = newMember();
        JsonNode other = signIn(m.email());

        JsonNode fresh = body(mockMvc.perform(post("/api/v1/account/sign-out-everywhere").header("Authorization", m.token()))
                .andExpect(status().isOk()));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", m.token())).andExpect(status().isUnauthorized());
        me(other.get("accessToken").asText()).andExpect(status().isUnauthorized());
        me(fresh.get("accessToken").asText()).andExpect(status().isOk());
    }

    @Test
    void librarianResetSignsTheMemberOut() throws Exception {
        var m = newMember();
        mockMvc.perform(post("/api/v1/members/{id}/reset-password", m.id())
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"Desk2024x\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", m.token())).andExpect(status().isUnauthorized());
    }

    // ---------- sign-in lockout ----------

    @Test
    void fiveWrongPasswordsLockThatEmailFromThatIp() throws Exception {
        var m = newMember();
        for (int i = 0; i < 5; i++) {
            login(m.email(), "Wrong1234", "10.1.1.1").andExpect(status().isUnauthorized());
        }

        // Locked, even with the right password
        login(m.email(), "Secret123", "10.1.1.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message", containsString("Try again in 15 minutes")));

        // The real owner elsewhere isn't locked out
        login(m.email(), "Secret123", "10.2.2.2").andExpect(status().isOk());
    }

    @Test
    void successfulSignInResetsTheCount() throws Exception {
        var m = newMember();
        for (int i = 0; i < 4; i++) {
            login(m.email(), "Wrong1234", "10.3.3.3").andExpect(status().isUnauthorized());
        }
        login(m.email(), "Secret123", "10.3.3.3").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) {
            login(m.email(), "Wrong1234", "10.3.3.3").andExpect(status().isUnauthorized());
        }
        login(m.email(), "Secret123", "10.3.3.3").andExpect(status().isOk());
    }

    @Test
    void unknownEmailsAreCountedToo() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("nobody-lock@example.com", "Wrong1234", "10.4.4.4").andExpect(status().isUnauthorized());
        }
        login("nobody-lock@example.com", "Wrong1234", "10.4.4.4").andExpect(status().isTooManyRequests());
    }
}
