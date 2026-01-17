package com.insuretech.pms.rfp.dto;

import com.insuretech.pms.rfp.entity.ProcessingStatus;
import com.insuretech.pms.rfp.entity.Rfp;
import com.insuretech.pms.rfp.entity.RfpStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RfpDto {
    private String id;
    private String projectId;
    private String title;
    private String content;
    private String filePath;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private RfpStatus status;
    private ProcessingStatus processingStatus;
    private String processingMessage;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int requirementCount;

    public static RfpDto fromEntity(Rfp rfp) {
        return RfpDto.builder()
                .id(rfp.getId())
                .projectId(rfp.getProjectId())
                .title(rfp.getTitle())
                .content(rfp.getContent())
                .filePath(rfp.getFilePath())
                .fileName(rfp.getFileName())
                .fileType(rfp.getFileType())
                .fileSize(rfp.getFileSize())
                .status(rfp.getStatus())
                .processingStatus(rfp.getProcessingStatus())
                .processingMessage(rfp.getProcessingMessage())
                .submittedAt(rfp.getSubmittedAt())
                .createdAt(rfp.getCreatedAt())
                .updatedAt(rfp.getUpdatedAt())
                .requirementCount(rfp.getRequirements() != null ? rfp.getRequirements().size() : 0)
                .build();
    }
}
