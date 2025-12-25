package com.insuretech.pms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRolePermissionRequest {

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Permission ID is required")
    private String permissionId;

    @NotNull(message = "Granted status is required")
    private Boolean granted;
}
