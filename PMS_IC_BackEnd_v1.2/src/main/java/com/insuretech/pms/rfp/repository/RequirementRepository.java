package com.insuretech.pms.rfp.repository;

import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.entity.RequirementCategory;
import com.insuretech.pms.rfp.entity.RequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, String> {

    List<Requirement> findByProjectIdOrderByCodeAsc(String projectId);

    List<Requirement> findByProjectIdAndStatusOrderByCodeAsc(String projectId, RequirementStatus status);

    List<Requirement> findByProjectIdAndCategoryOrderByCodeAsc(String projectId, RequirementCategory category);

    List<Requirement> findByRfpIdOrderByCodeAsc(String rfpId);

    Optional<Requirement> findByIdAndProjectId(String id, String projectId);

    Optional<Requirement> findByCode(String code);

    @Query("SELECT r FROM Requirement r WHERE r.projectId = :projectId AND " +
           "(LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Requirement> searchByKeyword(@Param("projectId") String projectId, @Param("keyword") String keyword);

    @Query("SELECT r FROM Requirement r WHERE r.assigneeId = :assigneeId AND r.projectId = :projectId")
    List<Requirement> findByAssigneeAndProject(@Param("assigneeId") String assigneeId, @Param("projectId") String projectId);

    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.projectId = :projectId AND r.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") String projectId, @Param("status") RequirementStatus status);

    @Query("SELECT MAX(CAST(SUBSTRING(r.code, LENGTH(:prefix) + 1) AS integer)) FROM Requirement r " +
           "WHERE r.projectId = :projectId AND r.code LIKE CONCAT(:prefix, '%')")
    Integer findMaxCodeNumber(@Param("projectId") String projectId, @Param("prefix") String prefix);

    long countByProjectId(String projectId);

    long countByRfpId(String rfpId);
}
