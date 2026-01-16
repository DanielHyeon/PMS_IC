package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
