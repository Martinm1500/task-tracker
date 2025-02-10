package com.martin1500.repository;

import com.martin1500.model.Task;
import com.martin1500.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long > {
    List<Task> findByUserOrderByPriorityAscDueDateAsc(User user);
}