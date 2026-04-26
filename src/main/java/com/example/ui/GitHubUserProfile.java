package com.example.ui;

import com.example.auth.RoleManager;

// Holds the logged-in GitHub user's info + their resolved role
public class GitHubUserProfile {

    private String id;
    private String login;
    private String name;
    private String email;
    private String avatarUrl;
    private String profileUrl;
    private String bio;
    private String accessToken;

    public String getId() { return id; }
    public String getLogin() { return login; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getProfileUrl() { return profileUrl; }
    public String getBio() { return bio; }
    public String getAccessToken() { return accessToken; }

    public void setId(String id) { this.id = id; }
    public void setLogin(String login) { this.login = login; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    // Falls back to username if no display name is set
    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : login;
    }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }

    // Role is derived live from RoleManager — never stale
    public RoleManager.Role getRole() {
        return RoleManager.getRole(login);
    }

    public boolean isAdmin() {
        return RoleManager.isAdmin(login);
    }

    public String getRoleLabel() {
        return RoleManager.getRoleLabel(login);
    }
}
