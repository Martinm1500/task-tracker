package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Role;
import com.martin1500.model.util.Status;
import com.martin1500.repository.TaskRepository;
import com.martin1500.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class TaskServiceImplTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User authenticatedUser;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    public void setUp() {

        authenticatedUser = new User();
        authenticatedUser.setUsername("testUser");
        authenticatedUser.setPassword("password");
        authenticatedUser.setEmail("testemail@gmail.com");
        authenticatedUser.setRole(Role.USER);
        authenticatedUser = userRepository.save(authenticatedUser);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createTask_ShouldReturnTaskDTO() {
        //Arrange
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO(Priority.LOW, LocalDate.now().plusDays(1),"test comments");

        // Act
        TaskDTO result = taskService.createTask(taskCreateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(Priority.LOW, result.getPriority());
        assertEquals(taskCreateDTO.dueDate(), result.getDueDate());
        assertEquals(Status.PENDING, result.getStatus());

        //Verify
        Task savedTask = taskRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedTask);
        assertEquals(authenticatedUser.getId(), savedTask.getUser().getId());
    }

    @Test
    public void getTasksForCurrentUser_ShouldReturnTasksDTO() {
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setPriority(Priority.HIGH);
        task1.setDueDate(LocalDate.now().plusDays(1));
        task1.setStatus(Status.PENDING);
        task1.setUser(authenticatedUser);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task 2");
        task2.setDescription("Description 2");
        task2.setPriority(Priority.MEDIUM);
        task2.setDueDate(LocalDate.now().plusDays(1));
        task2.setStatus(Status.PENDING);
        task2.setUser(authenticatedUser);
        taskRepository.save(task2);

        // Act
        List<TaskDTO> result = taskService.getTasksForCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());
    }

    @Test
    void getTaskById_ShouldReturnTaskDTO(){
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setPriority(Priority.HIGH);
        task1.setDueDate(LocalDate.now().plusDays(1));
        task1.setStatus(Status.PENDING);
        task1.setUser(authenticatedUser);
        Task savedTask = taskRepository.save(task1);

        // Act
        System.out.println(savedTask.getId());
        TaskDTO result = taskService.getTaskById(savedTask.getId());

        // Assert
        assertNotNull(result);
        assertEquals("Task 1",result.getTitle());
        assertEquals(task1.getDueDate(), result.getDueDate());
    }
}
