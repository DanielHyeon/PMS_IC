package com.insuretech.pms.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProjectMemberRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String userName;

    private String userEmail;

    @NotBlank(message = "Role is required")
    private String role;

    private String department;
}
