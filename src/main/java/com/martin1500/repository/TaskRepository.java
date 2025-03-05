package com.martin1500.repository;

import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long > {
    List<Task> findByCreatedByOrderByPriorityAscDueDateAsc(User user);

    Optional<Task> findByIdAndCreatedBy(Long id, User user);

    List<Task> findByCreatedByAndStatus(User user, Status status);

    List<Task> findByCreatedByAndPriority(User user, Priority priority);

    List<Task> findByCreatedByAndDueDateBefore(User user, LocalDate dueDate);

    boolean existsByTitleAndCreatedBy(String title, User user);

    List<Task> findByProjectIdAndCreatedBy(Long projectId, User createdBy);
}