package com.shelfspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShelfSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShelfSpaceApplication.class, args);
    }
}
