package com.insuretech.pms.rfp.entity;

import com.insuretech.pms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rfps", schema = "project")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rfp extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private RfpStatus status = RfpStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", length = 50)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(name = "processing_message")
    private String processingMessage;

    @Column(name = "submitted_at")
    private java.time.LocalDateTime submittedAt;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @OneToMany(mappedBy = "rfp", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Requirement> requirements = new ArrayList<>();

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.tenantId == null) {
            this.tenantId = this.projectId;
        }
    }

    public void addRequirement(Requirement requirement) {
        requirements.add(requirement);
        requirement.setRfp(this);
    }

    public void removeRequirement(Requirement requirement) {
        requirements.remove(requirement);
        requirement.setRfp(null);
    }
}
