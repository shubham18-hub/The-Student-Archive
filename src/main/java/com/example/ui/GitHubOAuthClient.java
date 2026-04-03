package com.example.ui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpExchange;

// Handles the GitHub OAuth2 login flow for the Swing desktop app.

public class GitHubOAuthClient {

    // GitHub OAuth App credentials — registered at github.com/settings/developers
    private static final String CLIENT_ID = "Ov23liERCYVAAMavkPmf";
    private static final String CLIENT_SECRET = "b66ef775def922da7c4eaeb4d2a6a1fed335898d";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";

    // GitHub API URLs
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_API_URL = "https://api.github.com/user";

    private static final int CALLBACK_PORT = 8888;
    private static final long TIMEOUT_SECONDS = 300; // 5 minutes for user to authorize

    // Starts the OAuth2 flow and returns a CompletableFuture.
    
    public static CompletableFuture<GitHubUserProfile> authorize() {
        CompletableFuture<GitHubUserProfile> result = new CompletableFuture<>();

        try {
            // Start the local server to catch GitHub's redirect
            LocalOAuthServer server = new LocalOAuthServer(CALLBACK_PORT);

            // Run the OAuth flow in a separate thread so the UI doesn't freeze
            new Thread(() -> {
                try {
                    // Wait for GitHub to redirect back to our local server
                    HttpExchange exchange = server.waitForCallback(TIMEOUT_SECONDS);

                    // Extract the "code" from the URL: /callback?code=XXXX
                    String queryString = exchange.getRequestURI().getQuery();
                    Map<String, String> params = parseQueryString(queryString);
                    String code = params.get("code");

                    // Send a success page to the browser
                    server.sendResponse(exchange, 200, getSuccessHtml());
                    server.close();

                    if (code == null) {
                        result.completeExceptionally(new Exception("No code received from GitHub"));
                        return;
                    }

                    System.out.println("Got authorization code from GitHub");

                    // Step 4: Exchange the code for an access token
                    String accessToken = exchangeCodeForToken(code);
                    if (accessToken == null || accessToken.isEmpty()) {
                        result.completeExceptionally(new Exception("Failed to get access token"));
                        return;
                    }

                    System.out.println("Got access token from GitHub");

                    // Step 5: Use the token to get the user's profile
                    GitHubUserProfile profile = fetchUserProfile(accessToken);
                    if (profile == null) {
                        result.completeExceptionally(new Exception("Failed to get user profile"));
                        return;
                    }

                    profile.setAccessToken(accessToken);
                    System.out.println("Logged in as: @" + profile.getLogin());

                    // Complete the future — the SwingWorker's .get() will now return
                    result.complete(profile);

                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            }).start();

            // Step 1: Build the GitHub authorization URL and open it in the browser
            String authUrl = AUTHORIZE_URL
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8")
                + "&scope=user:email,read:user";

            System.out.println("Opening GitHub login in browser...");
            Desktop.getDesktop().browse(new URI(authUrl));

        } catch (Exception e) {
            result.completeExceptionally(e);
        }

        return result;
    }

    // Step 4: Sends the code to GitHub and gets back an access token
    // This is a server-to-server POST request — the user never sees this
    private static String exchangeCodeForToken(String code) {
        try {
            // Build the POST request body
            String requestBody = "client_id=" + CLIENT_ID
                + "&client_secret=" + CLIENT_SECRET
                + "&code=" + code
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, "UTF-8");

            // Open a connection to GitHub's token endpoint
            HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true); // allow sending a request body

            // Write the request body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                System.out.println("Token exchange failed: HTTP " + conn.getResponseCode());
                return null;
            }

            // Read the response and extract the access_token field
            String response = readStream(conn.getInputStream());
            return extractJsonField(response, "access_token");

        } catch (Exception e) {
            System.out.println("Error getting token: " + e.getMessage());
            return null;
        }
    }

    // Step 5: Calls the GitHub API to get the user's profile information
    private static GitHubUserProfile fetchUserProfile(String accessToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(USER_API_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("User API failed: HTTP " + conn.getResponseCode());
                return null;
            }

            String response = readStream(conn.getInputStream());

            // Parse the JSON response and fill a GitHubUserProfile object
            GitHubUserProfile profile = new GitHubUserProfile();
            profile.setId(extractJsonField(response, "id"));
            profile.setLogin(extractJsonField(response, "login"));
            profile.setName(extractJsonField(response, "name"));
            profile.setAvatarUrl(extractJsonField(response, "avatar_url"));
            profile.setProfileUrl(extractJsonField(response, "html_url"));
            profile.setEmail(extractJsonField(response, "email"));
            profile.setBio(extractJsonField(response, "bio"));

            return profile;

        } catch (Exception e) {
            System.out.println("Error fetching profile: " + e.getMessage());
            return null;
        }
    }

    // Deletes the saved token — called when user clicks Logout
    public static void logout() {
        com.example.auth.TokenManager.deleteToken();
        System.out.println("User logged out");
    }

    // Converts a URL query string like "code=abc&state=123" into a Map
    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        for (String part : query.split("&")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    params.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        return params;
    }

    // Extracts a value from a JSON string using regex
    // Example: extractJsonField({"login":"shubham"}, "login") returns "shubham"
    private static String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (matcher.find()) {
            return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        }
        return "";
    }

    // Reads all text from an InputStream and returns it as a String
    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    // The HTML page shown in the browser after successful authorization
    private static String getSuccessHtml() {
        return "<!DOCTYPE html><html><body style='font-family:Arial;text-align:center;padding:50px'>"
             + "<h1>Login Successful!</h1>"
             + "<p>You can close this tab and return to the application.</p>"
             + "</body></html>";
    }
}
