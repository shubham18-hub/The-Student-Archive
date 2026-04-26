package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseSetup {

    public static void main(String[] args) {
        // No web server needed — this app runs as a desktop Swing UI
        SpringApplication app = new SpringApplication(DatabaseSetup.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
