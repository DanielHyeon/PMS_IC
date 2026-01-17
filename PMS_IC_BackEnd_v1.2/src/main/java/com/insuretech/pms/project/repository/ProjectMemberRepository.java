package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

    List<ProjectMember> findByProjectIdOrderByCreatedAtDesc(String projectId);

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    boolean existsByProjectIdAndUserId(String projectId, String userId);

    List<ProjectMember> findByUserId(String userId);

    void deleteByProjectIdAndUserId(String projectId, String userId);

    // Project-scoped authorization methods

    Optional<ProjectMember> findByProjectIdAndUserIdAndActiveTrue(String projectId, String userId);

    boolean existsByProjectIdAndUserIdAndActiveTrue(String projectId, String userId);

    List<ProjectMember> findByUserIdAndActiveTrue(String userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.userId = :userId AND pm.active = true")
    List<ProjectMember> findActiveProjectsForUser(@Param("userId") String userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.active = true")
    List<ProjectMember> findActiveMembers(@Param("projectId") String projectId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.active = true ORDER BY pm.createdAt DESC")
    List<ProjectMember> findByProjectIdAndActiveTrueOrderByCreatedAtDesc(@Param("projectId") String projectId);
}
