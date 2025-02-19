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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    @Test
    void createTask_ShouldReturnTaskDTO() {
        // Arrange
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO(Priority.LOW, LocalDate.now().plusDays(1), "test comments");

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
}
