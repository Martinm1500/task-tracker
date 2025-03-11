package com.martin1500.service;

import com.martin1500.dto.TaskCreateDTO;
import com.martin1500.dto.TaskDTO;
import com.martin1500.model.Project;
import com.martin1500.model.Task;
import com.martin1500.model.User;
import com.martin1500.model.util.Priority;
import com.martin1500.model.util.Role;
import com.martin1500.model.util.Status;
import com.martin1500.repository.ProjectRepository;
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
import java.util.HashSet;
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

    @Autowired
    private ProjectRepository projectRepository;

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
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        TaskCreateDTO taskCreateDTO = new TaskCreateDTO("Task 1", Priority.LOW, project.getId(), LocalDate.now().plusDays(1), "test comments");

        TaskDTO result = taskService.createTask(taskCreateDTO);

        assertNotNull(result);
        assertEquals("Task 1", result.getTitle());
        assertEquals(Priority.LOW, result.getPriority());
        assertEquals(taskCreateDTO.dueDate(), result.getDueDate());
        assertEquals(Status.PENDING, result.getStatus());

        Task savedTask = taskRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedTask);
        assertEquals(authenticatedUser.getId(), savedTask.getCreatedBy().getId());
        assertEquals(project.getId(), savedTask.getProject().getId());
    }

    @Test
    void getTasksForCurrentUser_ShouldReturnTasksDTO() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task1 = taskRepository.save(Task.builder().title("Task 1").description("Description 1").project(project)
                .priority(Priority.HIGH).dueDate(LocalDate.now().plusDays(1)).status(Status.PENDING).createdBy(authenticatedUser).build());
        Task task2 = taskRepository.save(Task.builder().title("Task 2").description("Description 2").project(project)
                .priority(Priority.MEDIUM).dueDate(LocalDate.now().plusDays(1)).status(Status.PENDING).createdBy(authenticatedUser).build());

        List<TaskDTO> result = taskService.getTasksForCurrentUser();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals("Task 2", result.get(1).getTitle());
    }

    @Test
    void getTaskById_ShouldReturnTaskDTO() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task1 = taskRepository.save(Task.builder().title("Task 1").description("Description 1").project(project)
                .priority(Priority.HIGH).dueDate(LocalDate.now().plusDays(1)).status(Status.PENDING).createdBy(authenticatedUser).build());

        TaskDTO result = taskService.getTaskById(task1.getId());

        assertNotNull(result);
        assertEquals("Task 1", result.getTitle());
        assertEquals(task1.getDueDate(), result.getDueDate());
        assertEquals(project.getId(), result.getProjectId());
    }

    @Test
    void updateTask_ShouldReturnUpdatedTask() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task1 = taskRepository.save(Task.builder().title("Task 1").description("Description 1").status(Status.PENDING)
                .priority(Priority.LOW).project(project).dueDate(LocalDate.now().plusDays(1)).comments("First comments")
                .createdBy(authenticatedUser).build());
        Long taskId = task1.getId();

        TaskDTO updatedTaskDTO = TaskDTO.builder()
                .title("Updated Title")
                .description("New description")
                .status(Status.IN_PROGRESS)
                .priority(Priority.HIGH)
                .projectId(project.getId())
                .dueDate(LocalDate.now().plusDays(5))
                .comments("Updated comments")
                .build();

        TaskDTO result = taskService.updateTask(taskId, updatedTaskDTO);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(Status.IN_PROGRESS, result.getStatus());
        assertEquals(Priority.HIGH, result.getPriority());
        assertEquals(LocalDate.now().plusDays(5), result.getDueDate());
        assertEquals("Updated comments", result.getComments());

        Task updatedTask = taskRepository.findById(taskId).orElseThrow();
        assertEquals(project.getId(), updatedTask.getProject().getId());
    }

    @Test
    void getTasksByStatus_ShouldReturnOnlyTasksWithGivenStatus() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.IN_PROGRESS).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());

        List<TaskDTO> result = taskService.getTasksByStatus(Status.PENDING);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getStatus() == Status.PENDING));
    }

    @Test
    void getTasksByPriority_ShouldReturnOnlyTasksWithGivenPriority() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.IN_PROGRESS).priority(Priority.HIGH).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());

        List<TaskDTO> result = taskService.getTasksByPriority(Priority.LOW);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getPriority() == Priority.LOW));
    }

    @Test
    void getOverdueTasks_ShouldReturnOnlyOverdueTasks() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().minusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().minusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().minusDays(1)).createdBy(authenticatedUser).build());
        taskRepository.save(Task.builder().status(Status.PENDING).priority(Priority.LOW).project(project)
                .dueDate(LocalDate.now().plusDays(1)).createdBy(authenticatedUser).build());

        List<TaskDTO> result = taskService.getOverdueTasks();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(task -> task.getDueDate().isBefore(LocalDate.now())));
    }

    @Test
    void getTasksByProject_ShouldReturnTasksForProject() {
        Project project = projectRepository.save(Project.builder().name("Project 1").build());
        Task task = taskRepository.save(Task.builder().title("Task 1").project(project).createdBy(authenticatedUser)
                .dueDate(LocalDate.now().plusDays(1)).priority(Priority.LOW).status(Status.PENDING).build());

        List<TaskDTO> result = taskService.getTasksByProject(project.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Task 1", result.get(0).getTitle());
        assertEquals(project.getId(), result.get(0).getProjectId());
    }

    @Test
    @Transactional
    void addAssignee_ShouldAddUserToTask() {
        Project project = new Project();
        project.setName("Project 1");
        project.getMembers().add(authenticatedUser);

        projectRepository.save(project);

        Task task = taskRepository.save(Task.builder().title("Task 1").project(project).createdBy(authenticatedUser)
                .dueDate(LocalDate.now().plusDays(1)).priority(Priority.LOW).status(Status.PENDING)
                .assignees(new HashSet<>()).build());

        User assignee = userRepository.save(User.builder().username("assignee").email("assignee@gmail.com")
                .password("password123").role(Role.USER).build());

        TaskDTO result = taskService.addAssignee(task.getId(), assignee.getId());

        assertNotNull(result);
        Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertTrue(updatedTask.getAssignees().contains(assignee));
    }
}





