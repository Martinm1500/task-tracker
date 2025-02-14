package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.exception.ResourceNotFoundException;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import com.martin1500.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    private final UserContextService userContextService;

    @Override
    @Transactional
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task newTask = taskDTOtoTask(taskCreateDTO);
        newTask.setUser(authenticatedUser);
        newTask.setStatus(Status.PENDING);
        Task createdTask = taskRepository.save(newTask);
        return taskToTaskDTO(createdTask);
    }

    @Override
    public List<TaskDTO> getTasksForCurrentUser() {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        List<Task> tasks = taskRepository.findByUserOrderByPriorityAscDueDateAsc(authenticatedUser);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task task = taskRepository.findByIdAndUser(id, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskToTaskDTO(task);
    }

    @Override
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        if (taskRepository.existsByTitleAndUser(taskDTO.getTitle(), authenticatedUser)) {
            throw new IllegalArgumentException("You already have a task with the same title.");
        }

        Task task = taskRepository.findByIdAndUser(id, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setPriority(taskDTO.getPriority());
        task.setDueDate(taskDTO.getDueDate());
        task.setComments(taskDTO.getComments());

        Task updatedTask = taskRepository.save(task);

        return taskToTaskDTO(updatedTask);
    }

    @Override
    public List<TaskDTO> getTasksByStatus(Status status) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        List<Task> tasks = taskRepository.findByUserAndStatus(authenticatedUser, status);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public List<TaskDTO> getTasksByPriority(Priority priority) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        List<Task> tasks = taskRepository.findByUserAndPriority(authenticatedUser, priority);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public List<TaskDTO> getOverdueTasks() {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        LocalDate currentDate = LocalDate.now();

        List<Task> tasks = taskRepository.findByUserAndDueDateBefore(authenticatedUser, currentDate);

        return convertTasksToDTOs(tasks);
    }

    private TaskDTO taskToTaskDTO(Task task){
        return TaskDTO.builder()
                .id(task.getId())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
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

    private List<TaskDTO> convertTasksToDTOs(List<Task> tasks) {
        return tasks.stream()
                .map(this::taskToTaskDTO)
                .collect(Collectors.toList());
    }
}