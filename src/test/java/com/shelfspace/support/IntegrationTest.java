package com.shelfspace.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    static {
        MYSQL.start();
    }
}
