package com.martin1500.controller;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Status;
import com.martin1500.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody TaskCreateDTO taskCreateDTO) {
        return ResponseEntity.ok(taskService.createTask(taskCreateDTO));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<TaskDTO>> getTasks() {
        List<TaskDTO> tasks = taskService.getTasksForCurrentUser();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO taskDTO = taskService.getTaskById(id);
        return ResponseEntity.ok(taskDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
        TaskDTO updatedTaskDTO = taskService.updateTask(id, taskDTO);
        return ResponseEntity.ok(updatedTaskDTO);
    }

    @PutMapping("/{id}/status/{status}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id, @PathVariable Status status) {
        TaskDTO updatedTaskDTO = taskService.updateTaskStatus(id, status);
        return ResponseEntity.ok(updatedTaskDTO);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable Status status) {
        List<TaskDTO> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<TaskDTO>> getTasksByPriority(@PathVariable Priority priority) {
        List<TaskDTO> tasks = taskService.getTasksByPriority(priority);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<TaskDTO>> getOverdueTasks() {
        List<TaskDTO> tasks = taskService.getOverdueTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @PostMapping("/{id}/assignees/{userId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> addAssignee(@PathVariable Long id, @PathVariable Long userId ) {
        return ResponseEntity.ok(taskService.addAssignee(id, userId));
    }

    @DeleteMapping("/{id}/assignees/{userId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<TaskDTO> removeAssignee(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(taskService.removeAssignee(id, userId));
    }
}
