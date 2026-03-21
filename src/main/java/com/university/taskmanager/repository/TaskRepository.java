package com.university.taskmanager.repository;

import com.university.taskmanager.model.Task;
import com.university.taskmanager.model.TaskPriority;
import com.university.taskmanager.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Task> findByPriorityAndStatusNot(TaskPriority priority, TaskStatus status);

    long countByProjectIdAndStatus(Long projectId, TaskStatus status);
}
