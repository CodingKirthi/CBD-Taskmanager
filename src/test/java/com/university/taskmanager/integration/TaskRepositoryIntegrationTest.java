package com.university.taskmanager.integration;

import com.university.taskmanager.model.Project;
import com.university.taskmanager.model.Task;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import com.university.taskmanager.repository.ProjectRepository;
import com.university.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TaskRepository and ProjectRepository.
 *
 * @DataJpaTest loads only the JPA slice: repositories, entity manager, and H2.
 * No web layer, no service layer, no full application context.
 * This is faster than @SpringBootTest and tests only persistence behaviour.
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByProjectId_returnsOnlyTasksBelongingToThatProject() {
        // Arrange: two projects with tasks
        Project projectA = entityManager.persistAndFlush(new Project("Project A", "desc A"));
        Project projectB = entityManager.persistAndFlush(new Project("Project B", "desc B"));

        entityManager.persistAndFlush(new Task("Task A1", null, TaskStatus.OPEN,
                TaskPriority.MEDIUM, null, projectA));
        entityManager.persistAndFlush(new Task("Task A2", null, TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH, null, projectA));
        entityManager.persistAndFlush(new Task("Task B1", null, TaskStatus.OPEN,
                TaskPriority.LOW, null, projectB));

        entityManager.clear();

        // Act
        List<Task> tasksForA = taskRepository.findByProjectId(projectA.getId());

        // Assert
        assertThat(tasksForA).hasSize(2);
        assertThat(tasksForA).allMatch(t -> t.getProject().getId().equals(projectA.getId()));
    }

    @Test
    void findByStatus_returnsOnlyTasksWithMatchingStatus() {
        // Arrange
        Project project = entityManager.persistAndFlush(new Project("Status Project", null));

        entityManager.persistAndFlush(new Task("Open task", null, TaskStatus.OPEN,
                TaskPriority.MEDIUM, null, project));
        entityManager.persistAndFlush(new Task("In progress task", null, TaskStatus.IN_PROGRESS,
                TaskPriority.MEDIUM, null, project));
        entityManager.persistAndFlush(new Task("Another open task", null, TaskStatus.OPEN,
                TaskPriority.HIGH, null, project));

        entityManager.clear();

        // Act
        List<Task> inProgressTasks = taskRepository.findByStatus(TaskStatus.IN_PROGRESS);

        // Assert
        assertThat(inProgressTasks).hasSize(1);
        assertThat(inProgressTasks.get(0).getTitle()).isEqualTo("In progress task");
    }

    @Test
    void countByProjectIdAndStatus_returnsCorrectCount() {
        // Arrange
        Project project = entityManager.persistAndFlush(new Project("Count Project", null));

        entityManager.persistAndFlush(new Task("Done 1", null, TaskStatus.DONE,
                TaskPriority.LOW, null, project));
        entityManager.persistAndFlush(new Task("Done 2", null, TaskStatus.DONE,
                TaskPriority.LOW, null, project));
        entityManager.persistAndFlush(new Task("Open 1", null, TaskStatus.OPEN,
                TaskPriority.MEDIUM, null, project));

        entityManager.clear();

        // Act
        long doneCount = taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE);

        // Assert
        assertThat(doneCount).isEqualTo(2);
    }

    @Test
    void findByPriorityAndStatusNot_returnsCriticalNonDoneTasks() {
        // Arrange
        Project project = entityManager.persistAndFlush(new Project("Critical Project", null));

        entityManager.persistAndFlush(new Task("Critical open", null, TaskStatus.OPEN,
                TaskPriority.CRITICAL, null, project));
        entityManager.persistAndFlush(new Task("Critical done", null, TaskStatus.DONE,
                TaskPriority.CRITICAL, null, project));
        entityManager.persistAndFlush(new Task("Medium open", null, TaskStatus.OPEN,
                TaskPriority.MEDIUM, null, project));

        entityManager.clear();

        // Act
        List<Task> criticalNonDone = taskRepository.findByPriorityAndStatusNot(
                TaskPriority.CRITICAL, TaskStatus.DONE);

        // Assert
        assertThat(criticalNonDone).hasSize(1);
        assertThat(criticalNonDone.get(0).getTitle()).isEqualTo("Critical open");
    }
}
