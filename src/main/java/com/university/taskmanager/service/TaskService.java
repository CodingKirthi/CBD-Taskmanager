package com.university.taskmanager.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.taskMapper = taskMapper;
    }

    public TaskDTO createTask(CreateTaskRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Due date cannot be in the past");
        }

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                TaskStatus.OPEN,
                request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM,
                request.getDueDate(),
                project
        );

        Task saved = taskRepository.save(task);
        return taskMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        return taskMapper.toDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        return taskRepository.findByProjectId(projectId).stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TaskDTO updateTaskStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (task.getStatus() == TaskStatus.DONE && newStatus == TaskStatus.OPEN) {
            throw new BusinessRuleException("Cannot reopen a completed task");
        }

        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }

        Task saved = taskRepository.save(task);
        return taskMapper.toDTO(saved);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> getCriticalOpenTasks() {
        return taskRepository.findByPriorityAndStatusNot(TaskPriority.CRITICAL, TaskStatus.DONE).stream()
                .map(taskMapper::toDTO)
                .collect(Collectors.toList());
    }
}
