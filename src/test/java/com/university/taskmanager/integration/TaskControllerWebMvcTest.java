package com.university.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.taskmanager.controller.TaskController;
import com.university.taskmanager.dto.CreateTaskRequest;
import com.university.taskmanager.dto.TaskDTO;
import com.university.taskmanager.exception.GlobalExceptionHandler;
import com.university.taskmanager.exception.ResourceNotFoundException;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer integration tests using @WebMvcTest.
 *
 * @WebMvcTest loads only the web layer: controllers, filters, and exception handlers.
 * The service is replaced with a @MockBean - no real database or service logic runs.
 * This tests HTTP mapping, request validation, and response serialisation.
 */
@WebMvcTest(TaskController.class)
class TaskControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createTask_withValidRequest_returns201WithTaskDTO() throws Exception {
        // Arrange
        CreateTaskRequest request = new CreateTaskRequest(
                "Implement feature", "details", TaskPriority.HIGH,
                LocalDate.now().plusDays(5), 1L);

        TaskDTO responseDTO = new TaskDTO(1L, "Implement feature", "details",
                TaskStatus.OPEN, TaskPriority.HIGH, LocalDate.now().plusDays(5),
                null, 1L, "Test Project");

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Implement feature"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createTask_withBlankTitle_returns400() throws Exception {
        // Arrange: title is blank, which violates @NotBlank
        CreateTaskRequest request = new CreateTaskRequest(
                "", "description", TaskPriority.MEDIUM, null, 1L);

        // No mock needed - Spring validation rejects the request before it reaches the service

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTask_withNullProjectId_returns400() throws Exception {
        // Arrange: projectId is null, which violates @NotNull
        CreateTaskRequest request = new CreateTaskRequest(
                "Valid title", null, TaskPriority.LOW, null, null);

        // Act & Assert
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTaskById_whenTaskNotFound_returns404() throws Exception {
        // Arrange
        when(taskService.getTaskById(999L))
                .thenThrow(new ResourceNotFoundException("Task", 999L));

        // Act & Assert
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999")));
    }

    @Test
    void updateTaskStatus_withValidStatus_returns200() throws Exception {
        // Arrange
        TaskDTO updatedDTO = new TaskDTO(1L, "Some task", null, TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM, null, null, 1L, "Project");

        when(taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(updatedDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/tasks/1/status")
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
