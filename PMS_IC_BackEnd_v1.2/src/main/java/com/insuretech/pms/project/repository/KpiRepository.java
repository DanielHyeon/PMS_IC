package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KpiRepository extends JpaRepository<Kpi, String> {
    List<Kpi> findByPhaseId(String phaseId);
}
