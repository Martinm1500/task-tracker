package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;

import java.util.List;

public interface TaskService {

    TaskDTO createTask(TaskCreateDTO taskCreateDTO);

    List<TaskDTO> getTasksForCurrentUser();

    TaskDTO getTaskById(Long id);

    TaskDTO updateTask(Long id, TaskDTO taskDTO);
}
