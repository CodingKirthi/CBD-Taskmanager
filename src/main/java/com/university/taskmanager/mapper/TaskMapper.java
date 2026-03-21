package com.university.taskmanager.mapper;

import com.university.taskmanager.dto.TaskDTO;
import com.university.taskmanager.model.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskDTO toDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setStatus(task.getStatus());
        dto.setPriority(task.getPriority());
        dto.setDueDate(task.getDueDate());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setProjectId(task.getProject().getId());
        dto.setProjectName(task.getProject().getName());
        return dto;
    }
}
