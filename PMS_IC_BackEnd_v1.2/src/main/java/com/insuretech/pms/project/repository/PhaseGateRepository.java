package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.PhaseGate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhaseGateRepository extends JpaRepository<PhaseGate, String> {
    Optional<PhaseGate> findByPhaseId(String phaseId);
}