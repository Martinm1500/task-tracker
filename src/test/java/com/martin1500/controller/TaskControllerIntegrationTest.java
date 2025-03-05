package com.martin1500.controller;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.dto.TokenPair;
import com.martin1500.model.Project;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Role;
import com.martin1500.model.util.Status;
import com.martin1500.repository.ProjectRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtService jwtService;

    private String accessToken;
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

        TokenPair tokenPair = jwtService.generateTokenPair(authenticatedUser.getUsername());
        accessToken = tokenPair.accessToken();
    }

    @AfterEach
    public void tearDown() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
        projectRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_ShouldReturnTaskDTO() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO("Task 1", Priority.LOW, project.getId(), LocalDate.now().plusDays(1), "test comments");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<TaskCreateDTO> request = new HttpEntity<>(taskCreateDTO, headers);

        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks", HttpMethod.POST, request, TaskDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Task 1", response.getBody().getTitle());
        assertEquals(Priority.LOW, response.getBody().getPriority());
        assertEquals(taskCreateDTO.dueDate(), response.getBody().getDueDate());
        assertEquals(Status.PENDING, response.getBody().getStatus());
        assertEquals(project.getId(), response.getBody().getProjectId());

        Task savedTask = taskRepository.findById(response.getBody().getId()).orElse(null);
        assertNotNull(savedTask);
        assertEquals(authenticatedUser.getId(), savedTask.getCreatedBy().getId());
        assertEquals(project.getId(), savedTask.getProject().getId());
    }

    @Test
    void getTasks_ShouldReturnTasksForAuthenticatedUser() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        taskRepository.save(Task.builder().title("Task 1").createdBy(authenticatedUser).priority(Priority.LOW)
                .dueDate(LocalDate.now().plusDays(1)).comments("Task 1 comments").project(project).build());
        taskRepository.save(Task.builder().title("Task 2").createdBy(authenticatedUser).priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(2)).comments("Task 2 comments").project(project).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List<TaskDTO>> response = restTemplate.exchange(
                "/api/tasks", HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        List<TaskDTO> tasks = response.getBody();
        assertTrue(tasks.stream().anyMatch(t -> t.getComments().equals("Task 1 comments")));
        assertTrue(tasks.stream().anyMatch(t -> t.getComments().equals("Task 2 comments")));
    }

    @Test
    void getTaskById_ShouldReturnTaskDTO() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task = taskRepository.save(Task.builder().title("Task 1").createdBy(authenticatedUser).priority(Priority.LOW)
                .dueDate(LocalDate.now().plusDays(1)).comments("Test task comments").project(project).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId(), HttpMethod.GET, request, TaskDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(task.getId(), response.getBody().getId());
        assertEquals(task.getComments(), response.getBody().getComments());
        assertEquals(task.getPriority(), response.getBody().getPriority());
        assertEquals(task.getDueDate(), response.getBody().getDueDate());
        assertEquals(project.getId(), response.getBody().getProjectId());
    }

    @Test
    void getTaskById_ShouldReturnNotFoundForInvalidId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/999", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTaskById_ShouldReturnForbiddenWhenNotAuthenticated() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/1", HttpMethod.GET, null, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateTask_ShouldReturnUpdatedTaskDTO() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task = taskRepository.save(Task.builder().title("Original Title").description("Original Description")
                .status(Status.PENDING).priority(Priority.LOW).dueDate(LocalDate.now().plusDays(1))
                .comments("Original Comments").createdBy(authenticatedUser).project(project).build());

        TaskDTO updatedTaskDTO = TaskDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .comments("Updated Comments")
                .projectId(project.getId())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<TaskDTO> request = new HttpEntity<>(updatedTaskDTO, headers);

        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId(), HttpMethod.PUT, request, TaskDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Title", response.getBody().getTitle());
        assertEquals("Updated Description", response.getBody().getDescription());
        assertEquals(Status.IN_PROGRESS, response.getBody().getStatus());
        assertEquals(Priority.HIGH, response.getBody().getPriority());
        assertEquals(LocalDate.now().plusDays(2), response.getBody().getDueDate());
        assertEquals("Updated Comments", response.getBody().getComments());
        assertEquals(project.getId(), response.getBody().getProjectId());
    }

    @Test
    void updateTask_ShouldReturnNotFoundForInvalidId() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        TaskDTO updatedTaskDTO = TaskDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(2))
                .comments("Updated Comments")
                .projectId(project.getId())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<TaskDTO> request = new HttpEntity<>(updatedTaskDTO, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/tasks/999", HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getTasksByProject_ShouldReturnTasksForProject() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        taskRepository.save(Task.builder().title("Task 1").createdBy(authenticatedUser).project(project)
                .dueDate(LocalDate.now().plusDays(1)).priority(Priority.LOW).status(Status.PENDING).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List<TaskDTO>> response = restTemplate.exchange(
                "/api/tasks/project/" + project.getId(), HttpMethod.GET, request, new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Task 1", response.getBody().get(0).getTitle());
        assertEquals(project.getId(), response.getBody().get(0).getProjectId());
    }

    @Test
    void addAssignee_ShouldAddUserToTask() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task = taskRepository.save(Task.builder().title("Task 1").createdBy(authenticatedUser).project(project)
                .dueDate(LocalDate.now().plusDays(1)).priority(Priority.LOW).status(Status.PENDING).build());
        User assignee = userRepository.save(User.builder().username("assignee").email("assignee@gmail.com")
                .password("password123").role(Role.USER).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Long> body = Map.of("userId", assignee.getId());
        HttpEntity<Map<String, Long>> request = new HttpEntity<>(body, headers);

        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId() + "/assignees", HttpMethod.POST, request, TaskDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertTrue(updatedTask.getAssignees().contains(assignee));
    }

    @Test
    void removeAssignee_ShouldRemoveUserFromTask() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        User assignee = userRepository.save(User.builder().username("assignee").email("assignee@gmail.com")
                .password("password123").role(Role.USER).build());
        Task task = Task.builder().title("Task 1").createdBy(authenticatedUser).project(project)
                .dueDate(LocalDate.now().plusDays(1)).priority(Priority.LOW).status(Status.PENDING).build();
        task.getAssignees().add(assignee);
        taskRepository.save(task);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<TaskDTO> response = restTemplate.exchange(
                "/api/tasks/" + task.getId() + "/assignees/" + assignee.getId(), HttpMethod.DELETE, request, TaskDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertFalse(updatedTask.getAssignees().contains(assignee));
    }
}
