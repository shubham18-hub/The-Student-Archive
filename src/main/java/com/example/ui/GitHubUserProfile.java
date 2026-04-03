package com.example.ui;

// This class stores the information about the logged-in GitHub user.
public class GitHubUserProfile {

    private String id;           // GitHub user ID number
    private String login;        // GitHub username 
    private String name;         // Full name (
    private String email;        // Email address
    private String avatarUrl;    // URL of the profile picture
    private String profileUrl;   // URL of the GitHub profile 
    private String bio;          // Bio text from GitHub profile
    private String accessToken;  // The OAuth2 token — used to make API calls on behalf of the user

    // Getters — used to read the values
    public String getId() { return id; }
    public String getLogin() { return login; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public String getBio() { return bio; }
    public String getAccessToken() { return accessToken; }

    // Setters — used to store the values after fetching from GitHub API
    public void setId(String id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    // Returns the display name — uses full name if available, otherwise uses username
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return login;
    }

    // Returns true if the user has a valid access token (is logged in)
    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }
}
