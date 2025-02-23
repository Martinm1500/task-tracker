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

import static org.junit.jupiter.api.Assertions.*;

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
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO("Task 1", Priority.LOW, LocalDate.now().plusDays(1),"test comments");

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
        TaskDTO result = taskService.getTaskById(savedTask.getId());

        // Assert
        assertNotNull(result);
        assertEquals("Task 1",result.getTitle());
        assertEquals(task1.getDueDate(), result.getDueDate());
    }

    @Test
    void updateTask_ShouldReturnUpdatedTask(){
        // Arrange
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setDescription("Description 1");
        task1.setStatus(Status.PENDING);
        task1.setPriority(Priority.LOW);
        task1.setDueDate(LocalDate.now().plusDays(1));
        task1.setComments("First comments");

        task1.setUser(authenticatedUser);
        Task savedTask = taskRepository.save(task1);

        Long taskId = savedTask.getId();

        TaskDTO updatedTaskDTO = TaskDTO.builder()
                .description("New description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .dueDate(LocalDate.now().plusDays(5))
                .comments("Updated comments")
                .build();

        //Act
        TaskDTO result = taskService.updateTask(taskId, updatedTaskDTO);

        //Assert
        assertNotNull(result);
        assertEquals("New description", result.getDescription());
        assertEquals(Status.IN_PROGRESS, result.getStatus());
        assertEquals(Priority.HIGH, result.getPriority());
        assertEquals(LocalDate.now().plusDays(5), result.getDueDate());
        assertEquals("Updated comments", result.getComments());

        // Verify in database
        Task updatedTask = taskRepository.findById(taskId).orElseThrow();
        assertEquals("New description", updatedTask.getDescription());
        assertEquals(Status.IN_PROGRESS, updatedTask.getStatus());
        assertEquals(Priority.HIGH, updatedTask.getPriority());
        assertEquals(LocalDate.now().plusDays(5), updatedTask.getDueDate());
        assertEquals("Updated comments", updatedTask.getComments());
    }

    @Test
    void getTasksByStatus_ShouldReturnOnlyTasksWithGivenStatus(){
        // Arrange
        Task task1 = new Task();

        task1.setStatus(Status.PENDING);
        task1.setPriority(Priority.LOW);
        task1.setDueDate(LocalDate.now().plusDays(1));

        task1.setUser(authenticatedUser);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setStatus(Status.PENDING);
        task2.setPriority(Priority.LOW);
        task2.setDueDate(LocalDate.now().plusDays(1));

        task2.setUser(authenticatedUser);
        taskRepository.save(task2);

        Task task3 = new Task();
        task3.setStatus(Status.PENDING);
        task3.setPriority(Priority.LOW);
        task3.setDueDate(LocalDate.now().plusDays(1));

        task3.setUser(authenticatedUser);
        taskRepository.save(task3);

        Task task4 = new Task();
        task4.setStatus(Status.IN_PROGRESS);
        task4.setPriority(Priority.LOW);
        task4.setDueDate(LocalDate.now().plusDays(1));

        task4.setUser(authenticatedUser);
        taskRepository.save(task4);

        //Act
        List<TaskDTO> result = taskService.getTasksByStatus(Status.PENDING);

        //Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getStatus() == Status.PENDING));
    }

    @Test
    void getTasksByPriority_ShouldReturnOnlyTasksWithGivenPriority(){
        // Arrange
        Task task1 = new Task();

        task1.setStatus(Status.PENDING);
        task1.setPriority(Priority.LOW);
        task1.setDueDate(LocalDate.now().plusDays(1));

        task1.setUser(authenticatedUser);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setStatus(Status.PENDING);
        task2.setPriority(Priority.LOW);
        task2.setDueDate(LocalDate.now().plusDays(1));

        task2.setUser(authenticatedUser);
        taskRepository.save(task2);

        Task task3 = new Task();
        task3.setStatus(Status.PENDING);
        task3.setPriority(Priority.LOW);
        task3.setDueDate(LocalDate.now().plusDays(1));

        task3.setUser(authenticatedUser);
        taskRepository.save(task3);

        Task task4 = new Task();
        task4.setStatus(Status.IN_PROGRESS);
        task4.setPriority(Priority.HIGH);
        task4.setDueDate(LocalDate.now().plusDays(1));

        task4.setUser(authenticatedUser);
        taskRepository.save(task4);

        //Act
        List<TaskDTO> result = taskService.getTasksByPriority(Priority.LOW);

        //Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getPriority() == Priority.LOW));
    }

    @Test
    void getOverdueTasks_ShouldReturnOnlyOverdueTasks(){
        // Arrange
        Task task1 = new Task();

        task1.setStatus(Status.PENDING);
        task1.setPriority(Priority.LOW);
        task1.setDueDate(LocalDate.now().minusDays(1));

        task1.setUser(authenticatedUser);
        taskRepository.save(task1);

        Task task2 = new Task();

        task2.setStatus(Status.PENDING);
        task2.setPriority(Priority.LOW);
        task2.setDueDate(LocalDate.now().minusDays(1));

        task2.setUser(authenticatedUser);
        taskRepository.save(task2);

        Task task3 = new Task();

        task3.setStatus(Status.PENDING);
        task3.setPriority(Priority.LOW);
        task3.setDueDate(LocalDate.now().minusDays(1));

        task3.setUser(authenticatedUser);
        taskRepository.save(task3);

        Task task4 = new Task();

        task4.setStatus(Status.PENDING);
        task4.setPriority(Priority.LOW);
        task4.setDueDate(LocalDate.now().plusDays(1));

        task4.setUser(authenticatedUser);
        taskRepository.save(task4);

        //Act
        List<TaskDTO> result = taskService.getOverdueTasks();

        //Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getDueDate().isBefore(LocalDate.now())));
    }
}





