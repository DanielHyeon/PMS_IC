package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.Deliverable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverableDto {
    private String id;
    private String phaseId;
    private String name;
    private String description;
    private String type;
    private String status;
    private String fileName;
    private Long fileSize;
    private String uploadedBy;
    private String approver;
    private LocalDateTime uploadedAt;
    private LocalDateTime approvedAt;

    public static DeliverableDto from(Deliverable deliverable) {
        return DeliverableDto.builder()
                .id(deliverable.getId())
                .phaseId(deliverable.getPhase() != null ? deliverable.getPhase().getId() : null)
                .name(deliverable.getName())
                .description(deliverable.getDescription())
                .type(deliverable.getType() != null ? deliverable.getType().name() : null)
                .status(deliverable.getStatus() != null ? deliverable.getStatus().name() : null)
                .fileName(deliverable.getFileName())
                .fileSize(deliverable.getFileSize())
                .uploadedBy(deliverable.getUploadedBy())
                .approver(deliverable.getApprover())
                .uploadedAt(deliverable.getCreatedAt())
                .approvedAt(deliverable.getApprovedAt())
                .build();
    }
}
