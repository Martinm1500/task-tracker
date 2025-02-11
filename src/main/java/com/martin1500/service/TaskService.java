package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;

import java.util.List;

public interface TaskService {

    TaskDTO createTask(TaskCreateDTO taskCreateDTO);

    List<TaskDTO> getTasksForCurrentUser();

    TaskDTO getTaskById(Long id);

    TaskDTO updateTask(Long id, TaskDTO taskDTO);

    List<TaskDTO> getTasksByStatus(Status status);

    List<TaskDTO> getTasksByPriority(Priority priority);

    List<TaskDTO> getOverdueTasks();
}
