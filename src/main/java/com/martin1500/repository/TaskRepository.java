package com.martin1500.repository;

import com.martin1500.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface TaskRepository extends JpaRepository<Task, Long > {
}
