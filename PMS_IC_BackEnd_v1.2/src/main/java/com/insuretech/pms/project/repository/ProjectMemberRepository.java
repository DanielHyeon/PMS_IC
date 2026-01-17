package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
