package com.university.taskmanager.mapper;

import com.university.taskmanager.dto.ProjectDTO;
import com.university.taskmanager.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectDTO toDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setTaskCount(project.getTasks().size());
        return dto;
    }
}
