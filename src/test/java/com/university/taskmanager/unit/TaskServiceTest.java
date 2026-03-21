package com.university.taskmanager.unit;

import com.university.taskmanager.dto.CreateTaskRequest;
import com.university.taskmanager.dto.TaskDTO;
import com.university.taskmanager.exception.BusinessRuleException;
import com.university.taskmanager.exception.ResourceNotFoundException;
import com.university.taskmanager.mapper.TaskMapper;
import com.university.taskmanager.model.Project;
import com.university.taskmanager.model.Task;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.repository.ProjectRepository;
import com.university.taskmanager.repository.TaskRepository;
import com.university.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 * These tests run without a Spring context - only Mockito is used.
 * Dependencies (TaskRepository, ProjectRepository, TaskMapper) are mocked.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    // -------------------------------------------------------------------------
    // createTask tests
    // -------------------------------------------------------------------------

    @Test
    void createTask_whenProjectExistsAndDueDateFuture_returnsTaskDTO() {
        // Arrange
        Project project = new Project("Alpha", "Alpha project");
        project.setId(1L);

        CreateTaskRequest request = new CreateTaskRequest(
                "Implement login", "OAuth2 login page",
                TaskPriority.HIGH, LocalDate.now().plusDays(7), 1L);

        Task savedTask = new Task("Implement login", "OAuth2 login page",
                TaskStatus.OPEN, TaskPriority.HIGH, LocalDate.now().plusDays(7), project);
        savedTask.setId(10L);

        TaskDTO expectedDTO = new TaskDTO(10L, "Implement login", "OAuth2 login page",
                TaskStatus.OPEN, TaskPriority.HIGH, LocalDate.now().plusDays(7),
                null, 1L, "Alpha");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
        when(taskMapper.toDTO(savedTask)).thenReturn(expectedDTO);

        // Act
        TaskDTO result = taskService.createTask(request);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Implement login");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(result.getProjectId()).isEqualTo(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_whenProjectNotFound_throwsResourceNotFoundException() {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest(
                "Some task", null, TaskPriority.MEDIUM, null, 99L);

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_whenDueDateIsInPast_throwsBusinessRuleException() {
        // Arrange
        Project project = new Project("Beta", "Beta project");
        project.setId(2L);

        CreateTaskRequest request = new CreateTaskRequest(
                "Past task", null, TaskPriority.LOW,
                LocalDate.now().minusDays(1), 2L);

        when(projectRepository.findById(2L)).thenReturn(Optional.of(project));

        // Act & Assert
        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("past");

        verify(taskRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateTaskStatus tests
    // -------------------------------------------------------------------------

    @Test
    void updateTaskStatus_toDone_setsCompletedAtTimestamp() {
        // Arrange
        Project project = new Project("Gamma", "Gamma project");
        project.setId(3L);

        Task task = new Task("Fix bug", null, TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH, null, project);
        task.setId(5L);

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDTO expectedDTO = new TaskDTO(5L, "Fix bug", null, TaskStatus.DONE,
                TaskPriority.HIGH, null, LocalDateTime.now(), 3L, "Gamma");
        when(taskMapper.toDTO(any(Task.class))).thenReturn(expectedDTO);

        // Act
        taskService.updateTaskStatus(5L, TaskStatus.DONE);

        // Assert: verify completedAt was set on the saved entity
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getCompletedAt()).isNotNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateTaskStatus_fromDoneToOpen_throwsBusinessRuleException() {
        // Arrange
        Project project = new Project("Delta", "Delta project");
        project.setId(4L);

        Task completedTask = new Task("Done task", null, TaskStatus.DONE,
                TaskPriority.MEDIUM, null, project);
        completedTask.setId(7L);
        completedTask.setCompletedAt(LocalDateTime.now().minusHours(1));

        when(taskRepository.findById(7L)).thenReturn(Optional.of(completedTask));

        // Act & Assert
        assertThatThrownBy(() -> taskService.updateTaskStatus(7L, TaskStatus.OPEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reopen");

        verify(taskRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getTaskById tests
    // -------------------------------------------------------------------------

    @Test
    void getTaskById_whenFound_returnsDTO() {
        // Arrange
        Project project = new Project("Epsilon", "Epsilon project");
        project.setId(5L);

        Task task = new Task("Write tests", "Unit tests", TaskStatus.OPEN,
                TaskPriority.MEDIUM, null, project);
        task.setId(20L);

        TaskDTO expectedDTO = new TaskDTO(20L, "Write tests", "Unit tests",
                TaskStatus.OPEN, TaskPriority.MEDIUM, null, null, 5L, "Epsilon");

        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(taskMapper.toDTO(task)).thenReturn(expectedDTO);

        // Act
        TaskDTO result = taskService.getTaskById(20L);

        // Assert
        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getTitle()).isEqualTo("Write tests");
    }

    @Test
    void getTaskById_whenNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskService.getTaskById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // -------------------------------------------------------------------------
    // deleteTask tests
    // -------------------------------------------------------------------------

    @Test
    void deleteTask_whenTaskExists_callsRepositoryDelete() {
        // Arrange
        when(taskRepository.existsById(15L)).thenReturn(true);

        // Act
        taskService.deleteTask(15L);

        // Assert
        verify(taskRepository).deleteById(15L);
    }
}
