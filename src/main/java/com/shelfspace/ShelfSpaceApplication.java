package com.shelfspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Users come from our own database via JWT, so Spring's default in-memory user is turned off.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class ShelfSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShelfSpaceApplication.class, args);
    }
}
