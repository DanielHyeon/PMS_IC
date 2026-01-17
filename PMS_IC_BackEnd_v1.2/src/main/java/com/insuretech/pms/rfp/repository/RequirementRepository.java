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

    /**
     * Find requirements without story points (for backlog planning)
     *
     * @param projectId the project ID
     * @return list of requirements without story points
     */
    @Query("SELECT r FROM Requirement r WHERE r.projectId = :projectId AND r.storyPoints IS NULL")
    List<Requirement> findRequirementsWithoutStoryPoints(@Param("projectId") String projectId);

    /**
     * Get sum of story points for all requirements
     *
     * @param projectId the project ID
     * @return sum of story points
     */
    @Query("SELECT COALESCE(SUM(r.storyPoints), 0) FROM Requirement r WHERE r.projectId = :projectId")
    Integer sumStoryPointsByProjectId(@Param("projectId") String projectId);

    /**
     * Find requirements with incomplete progress
     *
     * @param projectId the project ID
     * @return list of requirements not completed (progress < 100%)
     */
    @Query("SELECT r FROM Requirement r WHERE r.projectId = :projectId AND r.progressPercentage < 100 ORDER BY r.progressPercentage ASC")
    List<Requirement> findIncompleteRequirements(@Param("projectId") String projectId);

    /**
     * Count requirements by progress stage
     *
     * @param projectId the project ID
     * @return count of requirements with progress = 0 (NOT_STARTED)
     */
    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.projectId = :projectId AND r.progressPercentage = 0")
    long countNotStartedRequirements(@Param("projectId") String projectId);

    /**
     * Count requirements by progress stage
     *
     * @param projectId the project ID
     * @return count of requirements with progress > 0 and < 100 (IN_PROGRESS)
     */
    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.projectId = :projectId AND r.progressPercentage > 0 AND r.progressPercentage < 100")
    long countInProgressRequirements(@Param("projectId") String projectId);

    /**
     * Count requirements by progress stage
     *
     * @param projectId the project ID
     * @return count of completed requirements (progress = 100%)
     */
    @Query("SELECT COUNT(r) FROM Requirement r WHERE r.projectId = :projectId AND r.progressPercentage = 100")
    long countCompletedRequirements(@Param("projectId") String projectId);

    /**
     * Find requirements by epic
     *
     * @param projectId the project ID
     * @param epic the epic name
     * @return list of requirements in the epic
     */
    @Query("SELECT r FROM Requirement r WHERE r.projectId = :projectId AND r.neo4jNodeId LIKE CONCAT('%', :epic, '%')")
    List<Requirement> findByProjectIdAndEpic(@Param("projectId") String projectId, @Param("epic") String epic);
}
