package com.example.ui;

import com.example.auth.TokenManager;
import com.sun.net.httpserver.HttpExchange;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.*;

// Handles the GitHub OAuth2 login flow — opens the browser, waits for the callback, fetches the user profile
public class GitHubOAuthClient {

    private static final String CLIENT_ID     = "Ov23liERCYVAAMavkPmf";
    private static final String CLIENT_SECRET = "b66ef775def922da7c4eaeb4d2a6a1fed335898d";
    private static final String REDIRECT_URI  = "http://localhost:8888/callback";
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL     = "https://github.com/login/oauth/access_token";
    private static final String USER_API_URL  = "https://api.github.com/user";
    private static final int    CALLBACK_PORT = 8888;
    private static final long   TIMEOUT_SECS  = 300;

    public static CompletableFuture<GitHubUserProfile> authorize() {
        CompletableFuture<GitHubUserProfile> result = new CompletableFuture<>();

        try {
            LocalOAuthServer server = new LocalOAuthServer(CALLBACK_PORT);

            new Thread(() -> {
                try {
                    HttpExchange exchange = server.waitForCallback(TIMEOUT_SECS);

                    Map<String, String> params = parseQueryString(exchange.getRequestURI().getQuery());
                    String code = params.get("code");

                    server.sendResponse(exchange, 200, successHtml());
                    server.close();

                    if (code == null) {
                        result.completeExceptionally(new Exception("No code from GitHub"));
                        return;
                    }

                    String token = exchangeCodeForToken(code);
                    if (token == null || token.isEmpty()) {
                        result.completeExceptionally(new Exception("Failed to get access token"));
                        return;
                    }

                    GitHubUserProfile profile = fetchUserProfile(token);
                    if (profile == null) {
                        result.completeExceptionally(new Exception("Failed to fetch user profile"));
                        return;
                    }

                    profile.setAccessToken(token);
                    result.complete(profile);

                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }).start();

            String authUrl = AUTHORIZE_URL
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&scope=user:email,read:user";

            Desktop.getDesktop().browse(new URI(authUrl));

        } catch (Exception e) {
            result.completeExceptionally(e);
        }

        return result;
    }

    private static String exchangeCodeForToken(String code) {
        try {
            String body = "client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&code=" + code
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8");

            HttpURLConnection conn = (HttpURLConnection) new URI(TOKEN_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) return null;
            return extractJsonField(readStream(conn.getInputStream()), "access_token");

        } catch (Exception e) {
            System.out.println("Token exchange failed: " + e.getMessage());
            return null;
        }
    }

    private static GitHubUserProfile fetchUserProfile(String token) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(USER_API_URL).toURL().openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) return null;

            String json = readStream(conn.getInputStream());
            GitHubUserProfile profile = new GitHubUserProfile();
            profile.setId(extractJsonField(json, "id"));
            profile.setLogin(extractJsonField(json, "login"));
            profile.setName(extractJsonField(json, "name"));
            profile.setAvatarUrl(extractJsonField(json, "avatar_url"));
            profile.setProfileUrl(extractJsonField(json, "html_url"));
            profile.setEmail(extractJsonField(json, "email"));
            profile.setBio(extractJsonField(json, "bio"));
            return profile;

        } catch (Exception e) {
            System.out.println("Profile fetch failed: " + e.getMessage());
            return null;
        }
    }

    public static void logout() {
        TokenManager.deleteToken();
    }

    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                try { params.put(kv[0], URLDecoder.decode(kv[1], "UTF-8")); }
                catch (UnsupportedEncodingException ignored) {}
            }
        }
        return params;
    }

    private static String extractJsonField(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : "";
    }

    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String successHtml() {
        return "<!DOCTYPE html><html><body style='font-family:Arial;text-align:center;padding:50px'>"
             + "<h2>Login successful — you can close this tab.</h2></body></html>";
    }
}
