package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliverableRepository extends JpaRepository<Deliverable, String> {
    List<Deliverable> findByPhaseId(String phaseId);
}