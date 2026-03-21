package com.university.taskmanager.service;

import com.university.taskmanager.dto.CreateProjectRequest;
import com.university.taskmanager.dto.ProjectDTO;
import com.university.taskmanager.exception.BusinessRuleException;
import com.university.taskmanager.exception.ResourceNotFoundException;
import com.university.taskmanager.mapper.ProjectMapper;
import com.university.taskmanager.model.Project;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.repository.ProjectRepository;
import com.university.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectMapper = projectMapper;
    }

    public ProjectDTO createProject(CreateProjectRequest request) {
        if (projectRepository.existsByName(request.getName())) {
            throw new BusinessRuleException("A project with name '" + request.getName() + "' already exists");
        }

        Project project = new Project(request.getName(), request.getDescription());
        Project saved = projectRepository.save(project);
        return projectMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return projectMapper.toDTO(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .sorted(Comparator.comparing(Project::getName))
                .map(projectMapper::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project", id);
        }

        long inProgressCount = taskRepository.countByProjectIdAndStatus(id, TaskStatus.IN_PROGRESS);
        if (inProgressCount > 0) {
            throw new BusinessRuleException(
                    "Cannot delete project with " + inProgressCount + " in-progress task(s)");
        }

        projectRepository.deleteById(id);
    }
}
