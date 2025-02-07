package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;

public interface TaskService {

    TaskDTO createTask(TaskCreateDTO taskCreateDTO);
}
