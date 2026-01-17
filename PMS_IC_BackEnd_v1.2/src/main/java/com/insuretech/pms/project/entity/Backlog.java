package com.insuretech.pms.project.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Backlog Entity - Represents a Product Backlog for managing requirements and features
 *
 * The backlog serves as a collection point for:
 * - Requirements extracted from RFPs
 * - Manually created requirements
 * - Prioritized items ready for sprint planning
 *
 * Status values:
 * - ACTIVE: Backlog is active and items can be added/modified
 * - ARCHIVED: Backlog is archived and no longer in use
 */
@Entity
@Table(name = "backlogs", schema = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Backlog extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "name", length = 255)
    @Builder.Default
    private String name = "Product Backlog";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Backlog status
     * ACTIVE: Currently in use
     * ARCHIVED: No longer in use
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private BacklogStatus status = BacklogStatus.ACTIVE;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    /**
     * Backlog status enumeration
     */
    public enum BacklogStatus {
        ACTIVE,
        ARCHIVED
    }
}
