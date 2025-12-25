package com.example.familyhelper.data.models;

public class User {
    private String userId;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String birthday;
    private String profileImageUrl;
    private String familyId;
    private long joinedFamilyAt;

    public User() {}

    public User(String userId, String username, String email, String name, String phone) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.name = name;
        this.phone = phone;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public long getJoinedFamilyAt() { return joinedFamilyAt; }
    public void setJoinedFamilyAt(long joinedFamilyAt) { this.joinedFamilyAt = joinedFamilyAt; }
}