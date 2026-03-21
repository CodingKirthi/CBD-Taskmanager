package com.university.taskmanager.e2e;

import com.university.taskmanager.dto.CreateProjectRequest;
import com.university.taskmanager.dto.CreateTaskRequest;
import com.university.taskmanager.dto.ProjectDTO;
import com.university.taskmanager.dto.TaskDTO;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.repository.ProjectRepository;
import com.university.taskmanager.repository.TaskRepository;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests using the full Spring Boot application context.
 *
 * @SpringBootTest(webEnvironment = RANDOM_PORT) boots the entire application
 * on a random port, and TestRestTemplate makes real HTTP calls against it.
 * H2 in-memory database is used (application-test.properties).
 * This tests the complete request-to-database-to-response pipeline.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskManagerE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        // Configure TestRestTemplate to use Apache HttpComponents which supports PATCH
        restTemplate.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));
        taskRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void fullWorkflow_createProjectAndTask_thenCompleteTask() {
        // Step 1: Create a project
        CreateProjectRequest projectReq = new CreateProjectRequest("E2E Project", "Full workflow test");
        ResponseEntity<ProjectDTO> projectResponse = restTemplate.postForEntity(
                "/api/projects", projectReq, ProjectDTO.class);

        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long projectId = projectResponse.getBody().getId();
        assertThat(projectId).isNotNull();

        // Step 2: Create a task in that project
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "E2E Task", "A task for e2e testing",
                TaskPriority.HIGH, LocalDate.now().plusDays(14), projectId);
        ResponseEntity<TaskDTO> taskResponse = restTemplate.postForEntity(
                "/api/tasks", taskReq, TaskDTO.class);

        assertThat(taskResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long taskId = taskResponse.getBody().getId();
        assertThat(taskResponse.getBody().getStatus()).isEqualTo(TaskStatus.OPEN);

        // Step 3: Update the task status to DONE
        String statusUrl = UriComponentsBuilder.fromPath("/api/tasks/{id}/status")
                .queryParam("status", "DONE")
                .buildAndExpand(taskId)
                .toUriString();
        restTemplate.exchange(statusUrl, HttpMethod.PATCH, null, TaskDTO.class);

        // Step 4: Retrieve the task and verify it is DONE with completedAt set
        ResponseEntity<TaskDTO> finalTask = restTemplate.getForEntity(
                "/api/tasks/" + taskId, TaskDTO.class);

        assertThat(finalTask.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalTask.getBody().getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(finalTask.getBody().getCompletedAt()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTask_forNonExistentProject_returns404() {
        // Arrange: use a project ID that does not exist
        CreateTaskRequest request = new CreateTaskRequest(
                "Orphan Task", null, TaskPriority.LOW, null, 9999L);

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/tasks", request, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("message").toString()).contains("9999");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deleteProject_withInProgressTask_returns409Conflict() {
        // Step 1: Create project
        CreateProjectRequest projectReq = new CreateProjectRequest("Blocked Delete Project", null);
        ProjectDTO project = restTemplate.postForEntity(
                "/api/projects", projectReq, ProjectDTO.class).getBody();

        // Step 2: Create a task
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "Critical Task", null, TaskPriority.CRITICAL, null, project.getId());
        TaskDTO task = restTemplate.postForEntity(
                "/api/tasks", taskReq, TaskDTO.class).getBody();

        // Step 3: Move task to IN_PROGRESS
        String statusUrl = UriComponentsBuilder.fromPath("/api/tasks/{id}/status")
                .queryParam("status", "IN_PROGRESS")
                .buildAndExpand(task.getId())
                .toUriString();
        restTemplate.exchange(statusUrl, HttpMethod.PATCH, null, TaskDTO.class);

        // Step 4: Attempt to delete the project - should be blocked
        ResponseEntity<Map> deleteResponse = restTemplate.exchange(
                "/api/projects/" + project.getId(),
                org.springframework.http.HttpMethod.DELETE,
                null,
                Map.class);

        // Assert
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deleteResponse.getBody().get("message").toString())
                .contains("in-progress");
    }

    @Test
    void getCriticalOpenTasks_returnsOnlyCriticalNonDoneTasks() {
        // Arrange: create a project and 3 tasks
        CreateProjectRequest projectReq = new CreateProjectRequest("Critical Filter Project", null);
        ProjectDTO project = restTemplate.postForEntity(
                "/api/projects", projectReq, ProjectDTO.class).getBody();

        // Task 1: CRITICAL, status will become OPEN
        CreateTaskRequest critical1 = new CreateTaskRequest(
                "Critical Open Task", null, TaskPriority.CRITICAL, null, project.getId());
        TaskDTO task1 = restTemplate.postForEntity("/api/tasks", critical1, TaskDTO.class).getBody();

        // Task 2: MEDIUM priority, OPEN status
        CreateTaskRequest medium = new CreateTaskRequest(
                "Medium Open Task", null, TaskPriority.MEDIUM, null, project.getId());
        restTemplate.postForEntity("/api/tasks", medium, TaskDTO.class);

        // Task 3: CRITICAL priority, move to DONE
        CreateTaskRequest critical2 = new CreateTaskRequest(
                "Critical Done Task", null, TaskPriority.CRITICAL, null, project.getId());
        TaskDTO task3 = restTemplate.postForEntity("/api/tasks", critical2, TaskDTO.class).getBody();
        String doneUrl = UriComponentsBuilder.fromPath("/api/tasks/{id}/status")
                .queryParam("status", "DONE")
                .buildAndExpand(task3.getId())
                .toUriString();
        restTemplate.patchForObject(doneUrl, null, TaskDTO.class);

        // Act
        ResponseEntity<TaskDTO[]> response = restTemplate.getForEntity(
                "/api/tasks/critical", TaskDTO[].class);

        // Assert: only the CRITICAL + non-DONE task is returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getId()).isEqualTo(task1.getId());
        assertThat(response.getBody()[0].getPriority()).isEqualTo(TaskPriority.CRITICAL);
    }
}
