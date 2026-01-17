package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.Epic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Epic entity
 *
 * Provides data access operations for Epic management
 */
@Repository
public interface EpicRepository extends JpaRepository<Epic, String> {

    /**
     * Find all epics for a specific project
     *
     * @param projectId the project ID
     * @return list of epics for the project
     */
    List<Epic> findByProjectId(String projectId);

    /**
     * Find all active epics for a specific project
     *
     * @param projectId the project ID
     * @return list of active epics
     */
    @Query("SELECT e FROM Epic e WHERE e.projectId = :projectId AND e.status = 'ACTIVE' ORDER BY e.businessValue DESC NULLS LAST")
    List<Epic> findActiveEpicsByProjectId(@Param("projectId") String projectId);

    /**
     * Find epic by project ID and name
     *
     * @param projectId the project ID
     * @param name the epic name
     * @return optional containing the epic if found
     */
    Optional<Epic> findByProjectIdAndName(String projectId, String name);

    /**
     * Find epics by status for a project
     *
     * @param projectId the project ID
     * @param status the epic status
     * @return list of epics with the specified status
     */
    @Query("SELECT e FROM Epic e WHERE e.projectId = :projectId AND e.status = :status ORDER BY e.name ASC")
    List<Epic> findByProjectIdAndStatus(@Param("projectId") String projectId, @Param("status") String status);

    /**
     * Count epics by status for a project
     *
     * @param projectId the project ID
     * @param status the epic status
     * @return count of epics with the specified status
     */
    @Query("SELECT COUNT(e) FROM Epic e WHERE e.projectId = :projectId AND e.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") String projectId, @Param("status") String status);

    /**
     * Find epics owned by a specific user
     *
     * @param projectId the project ID
     * @param ownerId the owner ID
     * @return list of epics owned by the user
     */
    List<Epic> findByProjectIdAndOwnerId(String projectId, String ownerId);

    /**
     * Check if an epic exists for a project with the given name
     *
     * @param projectId the project ID
     * @param name the epic name
     * @return true if the epic exists
     */
    boolean existsByProjectIdAndName(String projectId, String name);
}
