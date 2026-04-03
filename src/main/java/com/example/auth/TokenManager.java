package com.example.auth;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;

// TokenManager saves the GitHub login token to a file on the user's computer.
// This way, when the user opens the app again, they are automatically logged in
// without having to go through GitHub again.
//
// Token file location: C:\Users\<username>\.academic-search-auth.json (on Windows)
public class TokenManager {

    // Path to the token file in the user's home directory
    private static final String TOKEN_FILE =
        System.getProperty("user.home") + File.separator + ".academic-search-auth.json";

    // Saves the user's login info to a JSON file on disk
    // Called after successful GitHub login
    public static void saveToken(com.example.ui.GitHubUserProfile profile) {
        try {
            JSONObject json = new JSONObject();
            json.put("token",     profile.getAccessToken());
            json.put("login",     profile.getLogin());
            json.put("name",      profile.getName());
            json.put("email",     profile.getEmail());
            json.put("avatarUrl", profile.getAvatarUrl());
            json.put("savedAt",   System.currentTimeMillis());

            Files.write(Paths.get(TOKEN_FILE), json.toString(2).getBytes());
            System.out.println("Token saved to: " + TOKEN_FILE);
        } catch (IOException e) {
            System.out.println("Could not save token: " + e.getMessage());
        }
    }

    // Returns just the raw token string — used for quick token checks
    public static String loadToken() {
        try {
            if (!Files.exists(Paths.get(TOKEN_FILE))) return null;
            String content = Files.readString(Paths.get(TOKEN_FILE));
            JSONObject json = new JSONObject(content);
            return json.optString("token", null);
        } catch (Exception e) {
            System.out.println("Could not load token: " + e.getMessage());
            return null;
        }
    }

    // Loads the saved user profile from disk
    // Called when the app starts — if a token exists, auto-login the user
    // Returns null if no token file exists (user has never logged in)
    public static com.example.ui.GitHubUserProfile loadProfile() {
        try {
            if (!Files.exists(Paths.get(TOKEN_FILE))) return null;

            String fileContent = Files.readString(Paths.get(TOKEN_FILE));
            JSONObject json = new JSONObject(fileContent);

            com.example.ui.GitHubUserProfile profile = new com.example.ui.GitHubUserProfile();
            profile.setLogin(json.optString("login"));
            profile.setName(json.optString("name"));
            profile.setEmail(json.optString("email"));
            profile.setAvatarUrl(json.optString("avatarUrl"));
            profile.setAccessToken(json.optString("token"));

            System.out.println("Auto-login: loaded profile for " + profile.getLogin());
            return profile;
        } catch (Exception e) {
            System.out.println("Could not load saved profile: " + e.getMessage());
            return null;
        }
    }

    // Deletes the token file from disk
    // Called when the user clicks Logout
    public static void deleteToken() {
        try {
            Files.deleteIfExists(Paths.get(TOKEN_FILE));
            System.out.println("Token deleted — user logged out");
        } catch (IOException e) {
            System.out.println("Could not delete token: " + e.getMessage());
        }
    }
}
