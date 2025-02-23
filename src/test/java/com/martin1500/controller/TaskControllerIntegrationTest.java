package com.martin1500.controller;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Role;
import com.martin1500.model.util.Status;
import com.martin1500.repository.TaskRepository;
import com.martin1500.repository.UserRepository;
import com.martin1500.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public class TaskControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;

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

        jwtToken = jwtService.generateToken(authenticatedUser.getUsername());
    }

    @AfterEach
    public void tearDown() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createTask_ShouldReturnTaskDTO() {
        // Arrange
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO("Task 1",Priority.LOW, LocalDate.now().plusDays(1), "test comments");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        HttpEntity<TaskCreateDTO> request = new HttpEntity<>(taskCreateDTO, headers);

        // Act
        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                request,
                TaskDTO.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Priority.LOW, response.getBody().getPriority());
        assertEquals(taskCreateDTO.dueDate(), response.getBody().getDueDate());
        assertEquals(Status.PENDING, response.getBody().getStatus());

        // Verify
        Task savedTask = taskRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(savedTask);
        assertEquals(authenticatedUser.getId(), savedTask.getUser().getId());
    }

    @Test
    void getTasks_ShouldReturnTasksForAuthenticatedUser() {
        // Arrange
        Task task1 = new Task();
        task1.setUser(authenticatedUser);
        task1.setPriority(Priority.LOW);
        task1.setDueDate(LocalDate.now().plusDays(1));
        task1.setComments("Task 1 comments");
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setUser(authenticatedUser);
        task2.setPriority(Priority.HIGH);
        task2.setDueDate(LocalDate.now().plusDays(2));
        task2.setComments("Task 2 comments");
        taskRepository.save(task2);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<List<TaskDTO>> response = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        // Verify
        List<TaskDTO> tasks = response.getBody();
        assertTrue(tasks.stream().anyMatch(t -> t.getComments().equals("Task 1 comments")));
        assertTrue(tasks.stream().anyMatch(t -> t.getComments().equals("Task 2 comments")));
    }

    @Test
    void getTaskById_ShouldReturnTaskDTO() {
        // Arrange
        Task task = new Task();
        task.setUser(authenticatedUser);
        task.setPriority(Priority.LOW);
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setComments("Test task comments");
        task = taskRepository.save(task);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId(),
                HttpMethod.GET,
                request,
                TaskDTO.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(task.getId(), response.getBody().getId());
        assertEquals(task.getComments(), response.getBody().getComments());
        assertEquals(task.getPriority(), response.getBody().getPriority());
        assertEquals(task.getDueDate(), response.getBody().getDueDate());
    }

    @Test
    void getTaskById_ShouldReturnNotFoundForInvalidId() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/999",
                HttpMethod.GET,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTaskById_ShouldReturnForbiddenWhenNotAuthenticated() {
        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/1",
                HttpMethod.GET,
                null,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateTask_ShouldReturnUpdatedTaskDTO() {
        // Arrange
        Task task = new Task();
        task.setUser(authenticatedUser);
        task.setTitle("Original Title");
        task.setDescription("Original Description");
        task.setStatus(Status.PENDING);
        task.setPriority(Priority.LOW);
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setComments("Original Comments");
        task = taskRepository.save(task);

        TaskDTO updatedTaskDTO = new TaskDTO();
        updatedTaskDTO.setTitle("Updated Title");
        updatedTaskDTO.setDescription("Updated Description");
        updatedTaskDTO.setStatus(Status.IN_PROGRESS);
        updatedTaskDTO.setPriority(Priority.HIGH);
        updatedTaskDTO.setDueDate(LocalDate.now().plusDays(2));
        updatedTaskDTO.setComments("Updated Comments");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        HttpEntity<TaskDTO> request = new HttpEntity<>(updatedTaskDTO, headers);

        // Act
        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId(),
                HttpMethod.PUT,
                request,
                TaskDTO.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Description", response.getBody().getDescription());
        assertEquals(Status.IN_PROGRESS, response.getBody().getStatus());
        assertEquals(Priority.HIGH, response.getBody().getPriority());
        assertEquals(LocalDate.now().plusDays(2), response.getBody().getDueDate());
        assertEquals("Updated Comments", response.getBody().getComments());
    }

    @Test
    void updateTask_ShouldReturnNotFoundForInvalidId() {
        // Arrange
        TaskDTO updatedTaskDTO = new TaskDTO();
        updatedTaskDTO.setTitle("Updated Title");
        updatedTaskDTO.setDescription("Updated Description");
        updatedTaskDTO.setStatus(Status.IN_PROGRESS);
        updatedTaskDTO.setPriority(Priority.HIGH);
        updatedTaskDTO.setDueDate(LocalDate.now().plusDays(2));
        updatedTaskDTO.setComments("Updated Comments");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        HttpEntity<TaskDTO> request = new HttpEntity<>(updatedTaskDTO, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/999",
                HttpMethod.PUT,
                request,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
