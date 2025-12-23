package com.example.familyhelper.data.models;

public class Task {
    private String taskId;
    private String title;
    private String description;
    private String area;
    private int priority;
    private boolean isCompleted;
    private String completedBy;
    private String date;
    private String familyId;
    private String createdBy;

    public Task() {}

    public Task(String title, String description, String area, int priority, String date, String familyId, String createdBy) {
        this.title = title;
        this.description = description;
        this.area = area;
        this.priority = priority;
        this.date = date;
        this.familyId = familyId;
        this.createdBy = createdBy;
        this.isCompleted = false;
        this.completedBy = null;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}