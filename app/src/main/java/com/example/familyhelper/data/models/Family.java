package com.example.familyhelper.data.models;

import java.util.ArrayList;
import java.util.List;

public class Family {
    private String familyId;
    private String familyName;
    private String creatorId;
    private long createdAt;
    private List<String> memberIds;
    private List<String> pendingInvites;

    public Family() {
        this.memberIds = new ArrayList<>();
        this.pendingInvites = new ArrayList<>();
    }

    public Family(String familyName, String creatorId) {
        this.familyName = familyName;
        this.creatorId = creatorId;
        this.createdAt = System.currentTimeMillis();
        this.memberIds = new ArrayList<>();
        this.memberIds.add(creatorId);
        this.pendingInvites = new ArrayList<>();
    }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public List<String> getMemberIds() {
        return memberIds != null ? memberIds : new ArrayList<>();
    }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }

    public List<String> getPendingInvites() {
        return pendingInvites != null ? pendingInvites : new ArrayList<>();
    }
    public void setPendingInvites(List<String> pendingInvites) {
        this.pendingInvites = pendingInvites;
    }

    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    public void addPendingInvite(String userId) {
        if (!pendingInvites.contains(userId)) {
            pendingInvites.add(userId);
        }
    }

    public void removePendingInvite(String userId) {
        pendingInvites.remove(userId);
    }

    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }

    public boolean isCreator(String userId) {
        return creatorId != null && creatorId.equals(userId);
    }
}