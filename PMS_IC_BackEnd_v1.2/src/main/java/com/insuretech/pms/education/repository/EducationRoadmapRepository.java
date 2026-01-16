package com.insuretech.pms.education.repository;

import com.insuretech.pms.education.entity.Education;
import com.insuretech.pms.education.entity.EducationRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRoadmapRepository extends JpaRepository<EducationRoadmap, String> {
    List<EducationRoadmap> findByTargetRoleOrderByLevelAscOrderNumAsc(Education.TargetRole targetRole);
    List<EducationRoadmap> findByLevelOrderByTargetRoleAscOrderNumAsc(EducationRoadmap.EducationLevel level);
    List<EducationRoadmap> findAllByOrderByTargetRoleAscLevelAscOrderNumAsc();
}
