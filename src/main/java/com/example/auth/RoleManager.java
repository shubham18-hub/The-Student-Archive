package com.example.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Role-based access control.
 *
 * ADMIN  — can delete records, re-index, view admin panel
 * USER   — can search and view PDFs only
 *
 * Admin list is checked against the GitHub login (username).
 * To add more admins, add their GitHub username to ADMIN_LOGINS.
 */
public class RoleManager {

    public enum Role {
        ADMIN, USER, GUEST   // GUEST = not logged in
    }

    // ── Add GitHub usernames here to grant admin access ──────────────────────
    private static final Set<String> ADMIN_LOGINS = new HashSet<>(Arrays.asList(
        "shubham18-hub",
        "vanshthakur0508",
        "sharma-dikshant",
        "sumeetnegi"
        // Add more GitHub usernames as needed
    ));

    /**
     * Determines the role for a given GitHub login.
     * Returns GUEST if login is null or empty.
     */
    public static Role getRole(String githubLogin) {
        if (githubLogin == null || githubLogin.isBlank()) return Role.GUEST;
        if (ADMIN_LOGINS.contains(githubLogin.toLowerCase().trim())) return Role.ADMIN;
        return Role.USER;
    }

    public static boolean isAdmin(String githubLogin) {
        return getRole(githubLogin) == Role.ADMIN;
    }

    public static boolean isUser(String githubLogin) {
        Role r = getRole(githubLogin);
        return r == Role.USER || r == Role.ADMIN;
    }

    public static String getRoleLabel(String githubLogin) {
        return switch (getRole(githubLogin)) {
            case ADMIN -> "ADMIN";
            case USER  -> "USER";
            case GUEST -> "GUEST";
        };
    }
}
