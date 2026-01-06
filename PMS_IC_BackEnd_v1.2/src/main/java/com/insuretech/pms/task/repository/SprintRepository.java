package com.insuretech.pms.task.repository;

import com.insuretech.pms.task.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByProjectIdOrderByStartDateDesc(String projectId);
    Optional<Sprint> findByProjectIdAndStatus(String projectId, Sprint.SprintStatus status);
}