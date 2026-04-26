package com.example.auth;

import com.example.ui.GitHubUserProfile;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// Saves and loads the GitHub login token from a JSON file in the user's home directory
public class TokenManager {

    private static final String TOKEN_FILE =
        System.getProperty("user.home") + File.separator + ".academic-search-auth.json";

    public static void saveToken(GitHubUserProfile profile) {
        try {
            JSONObject json = new JSONObject();
            json.put("token",     profile.getAccessToken());
            json.put("login",     profile.getLogin());
            json.put("name",      profile.getName());
            json.put("email",     profile.getEmail());
            json.put("avatarUrl", profile.getAvatarUrl());
            json.put("savedAt",   System.currentTimeMillis());
            Files.write(Paths.get(TOKEN_FILE), json.toString(2).getBytes());
        } catch (IOException e) {
            System.out.println("Could not save token: " + e.getMessage());
        }
    }

    public static String loadToken() {
        try {
            if (!Files.exists(Paths.get(TOKEN_FILE))) return null;
            return new JSONObject(Files.readString(Paths.get(TOKEN_FILE))).optString("token", null);
        } catch (Exception e) {
            return null;
        }
    }

    public static GitHubUserProfile loadProfile() {
        try {
            if (!Files.exists(Paths.get(TOKEN_FILE))) return null;

            JSONObject json = new JSONObject(Files.readString(Paths.get(TOKEN_FILE)));
            GitHubUserProfile profile = new GitHubUserProfile();
            profile.setLogin(json.optString("login"));
            profile.setName(json.optString("name"));
            profile.setEmail(json.optString("email"));
            profile.setAvatarUrl(json.optString("avatarUrl"));
            profile.setAccessToken(json.optString("token"));
            return profile;
        } catch (Exception e) {
            return null;
        }
    }

    public static void deleteToken() {
        try {
            Files.deleteIfExists(Paths.get(TOKEN_FILE));
        } catch (IOException e) {
            System.out.println("Could not delete token: " + e.getMessage());
        }
    }
}
