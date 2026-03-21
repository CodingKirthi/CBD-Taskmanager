package com.university.taskmanager.dto;

import java.time.LocalDateTime;

public class ProjectDTO {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private int taskCount;

    public ProjectDTO() {}

    public ProjectDTO(Long id, String name, String description, LocalDateTime createdAt, int taskCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.taskCount = taskCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }
}
