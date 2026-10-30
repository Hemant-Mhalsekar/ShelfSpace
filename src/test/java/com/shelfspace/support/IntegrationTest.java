package com.shelfspace.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for tests that run the whole app against a real MySQL in Docker.
 *
 * One container is started for the whole test run and shared by every subclass (the "singleton container"
 * pattern). That matters because Spring caches the app context between test classes: if each class started
 * its own container, the cached context would point at a container that had already stopped.
 *
 * Skipped automatically when Docker isn't running.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@shelfspace.dev";
    protected static final String ADMIN_PASSWORD = "Admin@12345";
    protected static final String MEMBER_EMAIL = "member@shelfspace.dev";
    protected static final String MEMBER_PASSWORD = "Member@12345";

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper json;

    /** Logs in and returns a ready-to-use "Bearer ..." header value. */
    protected String bearer(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    protected String adminToken() throws Exception {
        return bearer(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String memberToken() throws Exception {
        return bearer(MEMBER_EMAIL, MEMBER_PASSWORD);
    }

    /** A random but valid ISBN-13, so tests never collide on the unique ISBN column. */
    protected static String randomIsbn13() {
        StringBuilder sb = new StringBuilder("978");
        for (int i = 0; i < 9; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = sb.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        sb.append((10 - sum % 10) % 10);
        return sb.toString();
    }
}
