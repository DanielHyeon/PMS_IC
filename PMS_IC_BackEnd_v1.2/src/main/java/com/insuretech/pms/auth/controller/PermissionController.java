package com.insuretech.pms.auth.controller;

import com.insuretech.pms.auth.dto.PermissionDto;
import com.insuretech.pms.auth.dto.UpdateRolePermissionRequest;
import com.insuretech.pms.auth.service.PermissionService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "권한 관리 API")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "모든 권한 조회", description = "모든 권한과 역할별 권한 매핑을 조회합니다")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        List<PermissionDto> permissions = permissionService.getAllPermissionsWithRoles();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }

    @PutMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "역할 권한 수정", description = "특정 역할의 권한을 허용하거나 거부합니다 (Admin 전용)")
    public ResponseEntity<ApiResponse<String>> updateRolePermission(
            @Valid @RequestBody UpdateRolePermissionRequest request) {
        permissionService.updateRolePermission(request);
        return ResponseEntity.ok(ApiResponse.success("권한이 성공적으로 업데이트되었습니다", "OK"));
    }
}
