package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Epic Entity - Represents a grouping of related backlog items and requirements
 *
 * An Epic is a large body of work that can be broken down into smaller BacklogItems.
 * Epics enable higher-level tracking and organization of related work.
 *
 * Status values:
 * - DRAFT: Epic is being defined
 * - ACTIVE: Epic is in progress
 * - COMPLETED: Epic work is completed
 * - CANCELLED: Epic is no longer needed
 *
 * Note: This entity supports Phase 2/3 enterprise scaling.
 * For quick implementations, string-based tagging via BacklogItem.epicId is sufficient.
 */
@Entity
@Table(name = "epics", schema = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Epic extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Epic status enumeration
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private EpicStatus status = EpicStatus.DRAFT;

    /**
     * Optional goal or objective for this epic
     */
    @Column(name = "goal", columnDefinition = "TEXT")
    private String goal;

    /**
     * Owner or product manager responsible for this epic
     */
    @Column(name = "owner_id", length = 36)
    private String ownerId;

    /**
     * Target completion for this epic (optional)
     */
    @Column(name = "target_completion_date")
    private java.time.LocalDate targetCompletionDate;

    /**
     * Business value or priority score
     */
    @Column(name = "business_value")
    private Integer businessValue;

    /**
     * Cumulative story points for all backlog items in this epic
     * Calculated field - should be updated when items are added/removed
     */
    @Column(name = "total_story_points")
    private Integer totalStoryPoints;

    /**
     * Number of backlog items currently in this epic
     * Calculated field - should be updated when items are added/removed
     */
    @Column(name = "item_count")
    private Integer itemCount;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.totalStoryPoints == null) {
            this.totalStoryPoints = 0;
        }
        if (this.itemCount == null) {
            this.itemCount = 0;
        }
    }

    /**
     * Epic status enumeration
     */
    public enum EpicStatus {
        DRAFT,
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}
