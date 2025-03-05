package com.martin1500.service;

import com.martin1500.dto.ProjectDTO;
import com.martin1500.model.Project;
import com.martin1500.model.User;
import com.martin1500.repository.ProjectRepository;
import com.martin1500.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService{

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    @Override
    public ProjectDTO createProject(ProjectDTO dto) {
        User authenticatedUser = userContextService.getAuthenticatedUser();

        Project project = new Project();
        project.setName(dto.getName());
        project.getMembers().add(authenticatedUser);
        return mapToDTO(projectRepository.save(project));
    }

    @Override
    public List<ProjectDTO> getProjectsForCurrentUser() {
        User authenticatedUser = userContextService.getAuthenticatedUser();
        return projectRepository.findByMembersContaining(authenticatedUser).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDTO getProjectById(Long id) {
        return mapToDTO(projectRepository.findById(id).orElseThrow());
    }

    @Override
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project project = projectRepository.findById(id).orElseThrow();
        project.setName(dto.getName());
        return mapToDTO(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    @Override
    public ProjectDTO addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        project.getMembers().add(user);
        return mapToDTO(projectRepository.save(project));
    }

    @Override
    public ProjectDTO removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        project.getMembers().remove(user);
        return mapToDTO(projectRepository.save(project));
    }

    private ProjectDTO mapToDTO(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .members(project.getMembers())
                .build();
    }
}
