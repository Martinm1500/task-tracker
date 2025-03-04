package com.martin1500.service;

import com.martin1500.dto.ProjectDTO;

import java.util.List;

public interface ProjectService {
    ProjectDTO createProject(ProjectDTO dto);
    List<ProjectDTO> getProjectsForCurrentUser();
    ProjectDTO getProjectById(Long id);
    ProjectDTO updateProject(Long id, ProjectDTO dto);
    void deleteProject(Long id);
    ProjectDTO addMember(Long projectId, Long userId);
    ProjectDTO removeMember(Long projectId, Long userId);
}
