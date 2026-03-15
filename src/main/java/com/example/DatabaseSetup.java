package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseSetup {
    public static void main(String[] args) {
        // Spring Boot automatically connects to port 5432 using your application.properties!
        SpringApplication.run(DatabaseSetup.class, args);
        System.out.println("✅ Academic Resource Search Engine Backend is Running!");
    }
}