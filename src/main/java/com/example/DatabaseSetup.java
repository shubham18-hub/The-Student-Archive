package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// This is the starting point of our Spring Boot application.
// @SpringBootApplication tells Spring Boot to:
//   1. Scan all classes in this package for @Service, @Controller, @Repository
//   2. Auto-configure everything (database, server, security)
@SpringBootApplication
public class DatabaseSetup {

    public static void main(String[] args) {
        // This one line starts the entire application
        SpringApplication.run(DatabaseSetup.class, args);
    }
}
