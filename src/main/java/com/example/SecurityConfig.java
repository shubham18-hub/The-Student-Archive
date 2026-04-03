package com.example;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration means this class contains Spring configuration (settings)
// @EnableWebSecurity activates Spring Security for this application
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // SecurityFilterChain defines the security rules for every HTTP request
    // Every request passes through this filter before reaching any controller
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // These pages are PUBLIC — anyone can access without login
                .requestMatchers("/", "/login", "/error").permitAll()
                // The search API is also public — Swing UI uses it without login
                .requestMatchers("/api/**").permitAll()
                // Everything else (like /home) requires the user to be logged in
                .anyRequest().authenticated()
            )
            // oauth2Login sets up GitHub login
            // When user clicks "Login with GitHub", Spring Security handles everything:
            // opening GitHub, getting the token, fetching the user profile
            .oauth2Login(oauth -> oauth
                .loginPage("/login")                  // our custom login page
                .defaultSuccessUrl("/home", true)     // go to /home after login
            )
            // Logout clears the session and sends user back to the home page
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
