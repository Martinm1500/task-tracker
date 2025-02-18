package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.repository.TaskRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestTaskServiceImpl {

    @Mock
    private TaskRepository repository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(1L);
    }

    @Test
    void createTask_ShouldReturnTaskDTO() {
        //Arrange
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO(Priority.LOW,LocalDate.now().plusDays(1),"test comments");

        Task savedTask = Task.builder()
                .id(1L)
                .priority(taskCreateDTO.priority())
                .dueDate(taskCreateDTO.dueDate())
                .comments(taskCreateDTO.comments())
                .user(user)
                .build();

        when(repository.save(any(Task.class))).thenReturn(savedTask);
        when(userContextService.getAuthenticatedUser()).thenReturn(user);

        // Act
        TaskDTO result = taskService.createTask(taskCreateDTO);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.getId());
        Assertions.assertEquals(taskCreateDTO.priority(), result.getPriority());
        Assertions.assertEquals(taskCreateDTO.dueDate(), result.getDueDate());
        Assertions.assertEquals(taskCreateDTO.comments(), result.getComments());
    }

}
