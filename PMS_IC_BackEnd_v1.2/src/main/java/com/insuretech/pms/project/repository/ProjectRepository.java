package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByStatusOrderByCreatedAtDesc(Project.ProjectStatus status);
}