package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// @Controller (not @RestController) means this class returns HTML pages, not JSON
// Each method returns a String — the name of the HTML template to show
// Spring Boot + Thymeleaf looks for the file in src/main/resources/templates/
@Controller
public class WebController {

    // When user visits http://localhost:9090/
    // Spring shows the file: src/main/resources/templates/index.html
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // When user visits http://localhost:9090/home
    // Spring Security checks if user is logged in first
    // If not logged in, redirects to /login automatically
    @GetMapping("/home")
    public String home() {
        return "home";
    }

    // When user visits http://localhost:9090/login
    // Shows our custom login page with the GitHub button
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
