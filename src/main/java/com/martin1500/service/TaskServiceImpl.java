package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.exception.ResourceNotFoundException;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import com.martin1500.repository.TaskRepository;
import com.martin1500.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;

    private final UserRepository userRepository;

    @Override
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {
        User user = getAuthenticatedUser();
        Task newTask = taskDTOtoTask(taskCreateDTO);
        newTask.setUser(user);
        newTask.setStatus(Status.PENDING);
        Task createdTask = repository.save(newTask);
        return taskToTaskDTO(createdTask);
    }

    @Override
    public List<TaskDTO> getTasksForCurrentUser() {
        User currentUser = getAuthenticatedUser();
        List<Task> tasks = repository.findByUserOrderByPriorityAscDueDateAsc(currentUser);

        return tasks.stream()
                .map(this::taskToTaskDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        User currentUser = getAuthenticatedUser();
        Task task = repository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskToTaskDTO(task);
    }

    @Override
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        User currentUser = getAuthenticatedUser();

        Task task = repository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setPriority(taskDTO.getPriority());
        task.setDueDate(taskDTO.getDueDate());
        task.setComments(taskDTO.getComments());

        Task updatedTask = repository.save(task);

        return taskToTaskDTO(updatedTask);
    }

    @Override
    public List<TaskDTO> getTasksByStatus(Status status) {
        User currentUser = getAuthenticatedUser();

        List<Task> tasks = repository.findByUserAndStatus(currentUser, status);

        return tasks.stream()
                .map(this::taskToTaskDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaskDTO> getTasksByPriority(Priority priority) {
        User currentUser = getAuthenticatedUser();

        List<Task> tasks = repository.findByUserAndPriority(currentUser, priority);

        return tasks.stream()
                .map(this::taskToTaskDTO)
                .collect(Collectors.toList());
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

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userDetails.getUsername()));
        }
        throw new IllegalStateException("No authenticated user found.");
    }
}