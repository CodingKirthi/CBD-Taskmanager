package com.university.taskmanager.unit;

import com.university.taskmanager.dto.CreateProjectRequest;
import com.university.taskmanager.dto.ProjectDTO;
import com.university.taskmanager.exception.BusinessRuleException;
import com.university.taskmanager.exception.ResourceNotFoundException;
import com.university.taskmanager.mapper.ProjectMapper;
import com.university.taskmanager.model.Project;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.repository.ProjectRepository;
import com.university.taskmanager.repository.TaskRepository;
import com.university.taskmanager.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService.
 * No Spring context is loaded - Mockito handles all dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProject_whenNameIsUnique_returnsProjectDTO() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("My Project", "A test project");

        Project savedProject = new Project("My Project", "A test project");
        savedProject.setId(1L);
        savedProject.setCreatedAt(LocalDateTime.now());

        ProjectDTO expectedDTO = new ProjectDTO(1L, "My Project", "A test project",
                savedProject.getCreatedAt(), 0);

        when(projectRepository.existsByName("My Project")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMapper.toDTO(savedProject)).thenReturn(expectedDTO);

        // Act
        ProjectDTO result = projectService.createProject(request);

        // Assert
        assertThat(result.getName()).isEqualTo("My Project");
        assertThat(result.getId()).isEqualTo(1L);
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_whenNameIsDuplicate_throwsBusinessRuleException() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest("Existing Project", null);
        when(projectRepository.existsByName("Existing Project")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Existing Project");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_whenHasInProgressTasks_throwsBusinessRuleException() {
        // Arrange
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(taskRepository.countByProjectIdAndStatus(10L, TaskStatus.IN_PROGRESS)).thenReturn(2L);

        // Act & Assert
        assertThatThrownBy(() -> projectService.deleteProject(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("in-progress");

        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void deleteProject_whenNoInProgressTasks_callsRepositoryDelete() {
        // Arrange
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(taskRepository.countByProjectIdAndStatus(10L, TaskStatus.IN_PROGRESS)).thenReturn(0L);

        // Act
        projectService.deleteProject(10L);

        // Assert
        verify(projectRepository).deleteById(10L);
    }

    @Test
    void getProjectById_whenNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(projectRepository.findById(42L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> projectService.getProjectById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}
