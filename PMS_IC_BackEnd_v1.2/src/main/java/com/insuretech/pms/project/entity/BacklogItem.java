package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import com.insuretech.pms.rfp.entity.Requirement;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * BacklogItem Entity - Represents an item in the Product Backlog
 *
 * A BacklogItem can be:
 * - Linked to a Requirement (extracted from RFP or manually created)
 * - Manually created without a Requirement
 *
 * Status values:
 * - BACKLOG: Not yet selected for any sprint
 * - SELECTED: Selected and ready to be moved to sprint during sprint planning
 * - SPRINT: Currently in an active sprint
 * - COMPLETED: Work on this item is completed
 *
 * The priority_order field enables drag-and-drop prioritization (lower values = higher priority)
 */
@Entity
@Table(name = "backlog_items", schema = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacklogItem extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "backlog_id", nullable = false)
    private Backlog backlog;

    /**
     * Reference to associated Requirement (optional - can be null for manually created items)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    /**
     * Origin type of this backlog item
     * REQUIREMENT: Extracted from Requirement (linked to requirement_id)
     * MANUAL: Manually created without requirement
     * Determines validation requirements when transitioning to Committed state
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", length = 50, nullable = false)
    @Builder.Default
    private BacklogItemOrigin originType = BacklogItemOrigin.MANUAL;

    /**
     * Reference to associated Epic (optional - enables enterprise scaling)
     * If null, can use epicId string field for lightweight tagging
     * When Epic entity is available, prefer using this relationship
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id_ref")
    private Epic epic;

    /**
     * Epic identifier string - used for lightweight tagging when epic relationship is not used
     * Can be linked to an Epic entity via epicIdRef relationship
     * Alternative lightweight approach to epic management
     */
    @Column(name = "epic_id", length = 100)
    private String epicId;

    /**
     * Priority order for sorting (0 = highest priority)
     * Used for drag-and-drop prioritization
     */
    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder;

    /**
     * BacklogItem status
     * BACKLOG: In backlog, not selected
     * SELECTED: Selected for sprint planning
     * SPRINT: Currently in sprint
     * COMPLETED: Work completed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private BacklogItemStatus status = BacklogItemStatus.BACKLOG;

    /**
     * Story Points estimation (Fibonacci sequence: 1, 2, 3, 5, 8, 13, 21)
     * When linked to a Requirement, this should be synchronized with requirement.storyPoints
     */
    @Column(name = "story_points")
    private Integer storyPoints;

    /**
     * Estimated effort in hours
     * Used for capacity planning during sprint planning
     */
    @Column(name = "estimated_effort_hours")
    private Integer estimatedEffortHours;

    /**
     * Acceptance criteria for this backlog item
     * If linked to Requirement, this may be synchronized with requirement.acceptanceCriteria
     */
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    /**
     * Reference to Sprint when this item is moved to SPRINT status
     * null when status is BACKLOG, SELECTED, or COMPLETED
     * Set when transitioning from SELECTED to SPRINT
     */
    @Column(name = "sprint_id", length = 50)
    private String sprintId;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * BacklogItem status enumeration
     */
    public enum BacklogItemStatus {
        BACKLOG,
        SELECTED,
        SPRINT,
        COMPLETED
    }

    /**
     * BacklogItem origin type enumeration
     * Determines whether item was extracted from Requirement or manually created
     */
    public enum BacklogItemOrigin {
        REQUIREMENT,
        MANUAL
    }
}
