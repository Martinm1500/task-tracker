package com.martin1500.repository;

import com.martin1500.model.Project;
import com.martin1500.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByMembersContaining(User user);
}