package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.BacklogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BacklogItem entity
 *
 * Provides data access operations for Product Backlog item management
 */
@Repository
public interface BacklogItemRepository extends JpaRepository<BacklogItem, String> {

    /**
     * Find all items in a specific backlog
     *
     * @param backlogId the backlog ID
     * @return list of backlog items sorted by priority order
     */
    List<BacklogItem> findByBacklogIdOrderByPriorityOrderAsc(String backlogId);

    /**
     * Find all items in a backlog with a specific status
     *
     * @param backlogId the backlog ID
     * @param status the backlog item status
     * @return list of backlog items with the specified status
     */
    @Query("SELECT bi FROM BacklogItem bi WHERE bi.backlog.id = :backlogId AND bi.status = :status ORDER BY bi.priorityOrder ASC")
    List<BacklogItem> findByBacklogIdAndStatus(@Param("backlogId") String backlogId, @Param("status") String status);

    /**
     * Find backlog item by requirement ID
     *
     * @param requirementId the requirement ID
     * @return optional containing the backlog item if found
     */
    Optional<BacklogItem> findByRequirementId(String requirementId);

    /**
     * Find all selected items (SELECTED status) in a backlog for sprint planning
     *
     * @param backlogId the backlog ID
     * @return list of selected backlog items sorted by priority
     */
    @Query("SELECT bi FROM BacklogItem bi WHERE bi.backlog.id = :backlogId AND bi.status = 'SELECTED' ORDER BY bi.priorityOrder ASC")
    List<BacklogItem> findSelectedItemsForSprintPlanning(@Param("backlogId") String backlogId);

    /**
     * Get the highest priority order in a backlog
     *
     * @param backlogId the backlog ID
     * @return the maximum priority order value
     */
    @Query("SELECT COALESCE(MAX(bi.priorityOrder), 0) FROM BacklogItem bi WHERE bi.backlog.id = :backlogId")
    Integer findMaxPriorityOrderByBacklogId(@Param("backlogId") String backlogId);

    /**
     * Count items in a backlog
     *
     * @param backlogId the backlog ID
     * @return count of items in the backlog
     */
    long countByBacklogId(String backlogId);

    /**
     * Count items in a backlog with a specific status
     *
     * @param backlogId the backlog ID
     * @param status the backlog item status
     * @return count of items with the specified status
     */
    @Query("SELECT COUNT(bi) FROM BacklogItem bi WHERE bi.backlog.id = :backlogId AND bi.status = :status")
    long countByBacklogIdAndStatus(@Param("backlogId") String backlogId, @Param("status") String status);

    /**
     * Get sum of story points for selected items
     *
     * @param backlogId the backlog ID
     * @return sum of story points for selected items
     */
    @Query("SELECT COALESCE(SUM(bi.storyPoints), 0) FROM BacklogItem bi WHERE bi.backlog.id = :backlogId AND bi.status = 'SELECTED'")
    Integer sumStoryPointsForSelectedItems(@Param("backlogId") String backlogId);

    /**
     * Delete all items in a backlog
     *
     * @param backlogId the backlog ID
     */
    void deleteByBacklogId(String backlogId);

    /**
     * Find all items in a specific sprint
     *
     * @param sprintId the sprint ID
     * @return list of backlog items in the sprint
     */
    @Query("SELECT bi FROM BacklogItem bi WHERE bi.sprintId = :sprintId ORDER BY bi.priorityOrder ASC")
    List<BacklogItem> findBySprintId(@Param("sprintId") String sprintId);

    /**
     * Count items in a specific sprint
     *
     * @param sprintId the sprint ID
     * @return count of items in the sprint
     */
    @Query("SELECT COUNT(bi) FROM BacklogItem bi WHERE bi.sprintId = :sprintId")
    long countBySprintId(@Param("sprintId") String sprintId);
}
