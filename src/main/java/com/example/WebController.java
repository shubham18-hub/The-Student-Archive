package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String landing() {
        return "index"; // serves templates/index.html
    }

    @GetMapping("/home")
    public String home() {
        return "home"; // serves templates/home.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // serves templates/login.html
    }
}
