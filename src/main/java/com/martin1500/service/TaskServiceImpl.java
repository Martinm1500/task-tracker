package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;

    @Override
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {

        User user = getAuthenticatedUser();

        Task newTask = taskDTOtoTask(taskCreateDTO);
        newTask.setUser(user);

        Task createdTask = repository.save(newTask);

        return taskToTaskDTO(createdTask);
    }

    private TaskDTO taskToTaskDTO(Task task){
        return TaskDTO.builder()
                .id(task.getId())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .comments(task.getComments())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private Task taskDTOtoTask(TaskCreateDTO taskDTO){
        return Task.builder()
                .priority(taskDTO.priority())
                .dueDate(taskDTO.dueDate())
                .comments(taskDTO.comments())
                .build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }
}
