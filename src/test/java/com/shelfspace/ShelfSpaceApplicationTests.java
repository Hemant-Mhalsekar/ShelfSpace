package com.shelfspace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Boots the app against a real MySQL; checks Flyway migrations match the JPA entities. Skipped if Docker isn't running. */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ShelfSpaceApplicationTests {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    void contextLoads() {
    }
}
