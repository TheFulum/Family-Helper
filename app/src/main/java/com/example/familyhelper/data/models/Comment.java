package com.example.familyhelper.data.models;

public class Comment {
    private String id;
    private String text;
    private String userId;
    private long timestamp;

    public Comment() {}

    public Comment(String text, String userId, long timestamp) {
        this.text = text;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}