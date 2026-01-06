package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Phase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, String> {
    List<Phase> findByProjectIdOrderByOrderNumAsc(String projectId);
}