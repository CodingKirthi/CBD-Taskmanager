package com.university.taskmanager.unit;

import com.university.taskmanager.dto.TaskDTO;
import com.university.taskmanager.mapper.TaskMapper;
import com.university.taskmanager.model.Project;
import com.university.taskmanager.model.Task;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for TaskMapper.
 * No mocks or Spring context required - TaskMapper has zero dependencies.
 * These tests run as fast as any plain Java test.
 */
class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapper();

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        // Arrange
        Project project = new Project("Mapper Project", "desc");
        project.setId(5L);

        Task task = new Task("Map me", "description here",
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                LocalDate.of(2026, 6, 1), project);
        task.setId(99L);
        task.setCompletedAt(LocalDateTime.of(2026, 5, 20, 10, 0));

        // Act
        TaskDTO dto = mapper.toDTO(task);

        // Assert
        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getTitle()).isEqualTo("Map me");
        assertThat(dto.getDescription()).isEqualTo("description here");
        assertThat(dto.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(dto.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(dto.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 10, 0));
        assertThat(dto.getProjectId()).isEqualTo(5L);
        assertThat(dto.getProjectName()).isEqualTo("Mapper Project");
    }

    @Test
    void toDTO_whenCompletedAtIsNull_dtoCompletedAtIsNull() {
        // Arrange
        Project project = new Project("Null Test Project", null);
        project.setId(6L);

        Task task = new Task("Incomplete task", null,
                TaskStatus.OPEN, TaskPriority.LOW, null, project);
        task.setId(100L);
        // completedAt intentionally not set

        // Act
        TaskDTO dto = mapper.toDTO(task);

        // Assert
        assertThat(dto.getCompletedAt()).isNull();
        assertThat(dto.getDueDate()).isNull();
    }
}
