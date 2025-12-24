package com.example.familyhelper.data.models;

public class FamilyInvite {
    private String inviteId;
    private String familyId;
    private String familyName;
    private String fromUserId;
    private String fromUsername;
    private String toUserId;
    private long timestamp;
    private String status;

    public FamilyInvite() {}

    public FamilyInvite(String familyId, String familyName, String fromUserId,
                        String fromUsername, String toUserId) {
        this.familyId = familyId;
        this.familyName = familyName;
        this.fromUserId = fromUserId;
        this.fromUsername = fromUsername;
        this.toUserId = toUserId;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    public String getInviteId() { return inviteId; }
    public void setInviteId(String inviteId) { this.inviteId = inviteId; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}