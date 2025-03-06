package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.exception.ResourceNotFoundException;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import com.martin1500.repository.ProjectRepository;
import com.martin1500.repository.TaskRepository;
import com.martin1500.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final UserContextService userContextService;

    @Override
    @Transactional
    public TaskDTO createTask(TaskCreateDTO taskCreateDTO) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task newTask = taskCreateDTOtoTask(taskCreateDTO);
        newTask.setProject(projectRepository.findById(taskCreateDTO.projectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + taskCreateDTO.projectId())));
        newTask.setCreatedBy(authenticatedUser);
        newTask.setTitle(taskCreateDTO.title());
        newTask.setStatus(Status.PENDING);

        Task createdTask = taskRepository.save(newTask);
        return taskToTaskDTO(createdTask);
    }

    @Override
    public List<TaskDTO> getTasksForCurrentUser() {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        List<Task> tasks = taskRepository.findByCreatedByOrderByPriorityAscDueDateAsc(authenticatedUser);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public TaskDTO getTaskById(Long id) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task task = taskRepository.findByIdAndCreatedBy(id, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskToTaskDTO(task);
    }

    @Override
    @Transactional
    public TaskDTO updateTask(Long id, TaskDTO taskDTO) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        Task task = taskRepository.findByIdAndCreatedBy(id, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(taskDTO.getTitle());
        task.setDescription(taskDTO.getDescription());
        task.setStatus(taskDTO.getStatus());
        task.setPriority(taskDTO.getPriority());
        task.setDueDate(taskDTO.getDueDate());
        task.setUpdatedAt(LocalDateTime.now());
        task.setComments(taskDTO.getComments());

        Task updatedTask = taskRepository.save(task);

        return taskToTaskDTO(updatedTask);
    }

    @Override
    @Transactional
    public TaskDTO updateTaskStatus(Long id, Status status) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        Task task = taskRepository.findByIdAndCreatedBy(id, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());

        Task updatedTask = taskRepository.save(task);
        return taskToTaskDTO(updatedTask);
    }

    @Override
    public List<TaskDTO> getTasksByStatus(Status status) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        List<Task> tasks = taskRepository.findByCreatedByAndStatus(authenticatedUser, status);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public List<TaskDTO> getTasksByPriority(Priority priority) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        List<Task> tasks = taskRepository.findByCreatedByAndPriority(authenticatedUser, priority);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public List<TaskDTO> getOverdueTasks() {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        LocalDate currentDate = LocalDate.now();

        List<Task> tasks = taskRepository.findByCreatedByAndDueDateBefore(authenticatedUser, currentDate);

        return convertTasksToDTOs(tasks);
    }

    @Override
    public List<TaskDTO> getTasksByProject(Long projectId) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        List<Task> tasks = taskRepository.findByProjectIdAndCreatedBy(projectId, authenticatedUser);
        return convertTasksToDTOs(tasks);
    }

    @Override
    @Transactional
    public TaskDTO addAssignee(Long taskId, Long userId) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task task = taskRepository.findByIdAndCreatedBy(taskId, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        task.getAssignees().add(assignee);
        task.getProject().getMembers().add(assignee);
        Task updatedTask = taskRepository.save(task);
        return taskToTaskDTO(updatedTask);
    }

    @Override
    @Transactional
    public TaskDTO removeAssignee(Long taskId, Long userId) {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        Task task = taskRepository.findByIdAndCreatedBy(taskId, authenticatedUser)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        task.getAssignees().remove(assignee);
        Task updatedTask = taskRepository.save(task);
        return taskToTaskDTO(updatedTask);
    }

    private TaskDTO taskToTaskDTO(Task task){
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .projectId(task.getProject().getId())
                .comments(task.getComments())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private Task taskCreateDTOtoTask(TaskCreateDTO taskDTO){
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