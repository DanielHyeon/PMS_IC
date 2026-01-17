package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Backlog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Backlog entity
 *
 * Provides data access operations for Product Backlog management
 */
@Repository
public interface BacklogRepository extends JpaRepository<Backlog, String> {

    /**
     * Find all backlogs for a specific project
     *
     * @param projectId the project ID
     * @return list of backlogs for the project
     */
    List<Backlog> findByProjectId(String projectId);

    /**
     * Find active backlog for a specific project
     *
     * @param projectId the project ID
     * @return optional containing the active backlog if found
     */
    @Query("SELECT b FROM Backlog b WHERE b.projectId = :projectId AND b.status = 'ACTIVE'")
    Optional<Backlog> findActiveBacklogByProjectId(@Param("projectId") String projectId);

    /**
     * Check if a backlog exists for a project
     *
     * @param projectId the project ID
     * @return true if at least one backlog exists for the project
     */
    boolean existsByProjectId(String projectId);

    /**
     * Find backlog by project ID and name
     *
     * @param projectId the project ID
     * @param name the backlog name
     * @return optional containing the backlog if found
     */
    Optional<Backlog> findByProjectIdAndName(String projectId, String name);
}
