package com.martin1500.service;

import com.martin1500.dto.ProjectDTO;
import com.martin1500.repository.ProjectRepository;
import com.martin1500.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService{

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public ProjectDTO createProject(ProjectDTO dto) {
        return null;
    }

    @Override
    public List<ProjectDTO> getProjectsForCurrentUser() {
        return null;
    }

    @Override
    public ProjectDTO getProjectById(Long id) {
        return null;
    }

    @Override
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        return null;
    }

    @Override
    public void deleteProject(Long id) {

    }

    @Override
    public ProjectDTO addMember(Long projectId, Long userId) {
        return null;
    }

    @Override
    public ProjectDTO removeMember(Long projectId, Long userId) {
        return null;
    }
}
