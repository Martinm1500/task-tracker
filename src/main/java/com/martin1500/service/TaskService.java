package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;

import java.util.List;

public interface TaskService {

    /**
    * Creates a new task.
    *
    * @param taskCreateDTO The task object containing details such as title, description, priority, and due date.
    * @return The created task with a unique ID and timestamps.
    **/
    TaskDTO createTask(TaskCreateDTO taskCreateDTO);

    List<TaskDTO> getTasksForCurrentUser();

    TaskDTO getTaskById(Long id);

    TaskDTO updateTask(Long id, TaskDTO taskDTO);

    TaskDTO updateTaskStatus(Long id, Status status);

    List<TaskDTO> getTasksByStatus(Status status);

    List<TaskDTO> getTasksByPriority(Priority priority);

    List<TaskDTO> getOverdueTasks();
}
