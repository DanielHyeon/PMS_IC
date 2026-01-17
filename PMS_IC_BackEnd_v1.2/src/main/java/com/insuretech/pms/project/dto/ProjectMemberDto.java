package com.insuretech.pms.project.dto;

import com.insuretech.pms.project.entity.ProjectMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private String id;
    private String projectId;
    private String userId;
    private String userName;
    private String userEmail;
    private String role;
    private String department;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProjectMemberDto from(ProjectMember member) {
        return ProjectMemberDto.builder()
                .id(member.getId())
                .projectId(member.getProject() != null ? member.getProject().getId() : null)
                .userId(member.getUserId())
                .userName(member.getUserName())
                .userEmail(member.getUserEmail())
                .role(member.getRole().name())
                .department(member.getDepartment())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}
