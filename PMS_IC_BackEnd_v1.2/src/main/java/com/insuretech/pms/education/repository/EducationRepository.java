package com.insuretech.pms.education.repository;

import com.insuretech.pms.education.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, String> {
    List<Education> findByIsActiveTrueOrderByCreatedAtDesc();
    List<Education> findByEducationTypeOrderByCreatedAtDesc(Education.EducationType educationType);
    List<Education> findByCategoryOrderByCreatedAtDesc(Education.EducationCategory category);
    List<Education> findByTargetRoleOrderByCreatedAtDesc(Education.TargetRole targetRole);
}
