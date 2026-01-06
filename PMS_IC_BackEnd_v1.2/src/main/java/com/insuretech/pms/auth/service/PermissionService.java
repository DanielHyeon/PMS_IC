package com.insuretech.pms.auth.service;

import com.insuretech.pms.auth.dto.PermissionDto;
import com.insuretech.pms.auth.dto.UpdateRolePermissionRequest;
import com.insuretech.pms.auth.entity.Permission;
import com.insuretech.pms.auth.entity.RolePermission;
import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.PermissionRepository;
import com.insuretech.pms.auth.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<PermissionDto> getAllPermissionsWithRoles() {
        List<Permission> permissions = permissionRepository.findAllByOrderByCategoryAscNameAsc();
        List<RolePermission> rolePermissions = rolePermissionRepository.findAll();

        // Group role permissions by permission ID
        Map<String, Map<User.UserRole, Boolean>> permissionRoleMap = new HashMap<>();
        for (RolePermission rp : rolePermissions) {
            permissionRoleMap
                    .computeIfAbsent(rp.getPermission().getId(), k -> new HashMap<>())
                    .put(rp.getRole(), rp.getGranted());
        }

        // Build DTOs with role permissions
        return permissions.stream().map(permission -> {
            PermissionDto dto = PermissionDto.fromEntity(permission);
            Map<String, Boolean> roles = new HashMap<>();

            for (User.UserRole role : User.UserRole.values()) {
                boolean granted = permissionRoleMap
                        .getOrDefault(permission.getId(), new HashMap<>())
                        .getOrDefault(role, false);
                roles.put(role.name().toLowerCase(), granted);
            }

            dto.setRoles(roles);
            return dto;
        }).collect(Collectors.toList());
    }

    public void updateRolePermission(UpdateRolePermissionRequest request) {
        User.UserRole role = User.UserRole.valueOf(request.getRole().toUpperCase());
        Permission permission = permissionRepository.findById(request.getPermissionId())
                .orElseThrow(() -> new RuntimeException("Permission not found: " + request.getPermissionId()));

        Optional<RolePermission> existingRp = rolePermissionRepository
                .findByRoleAndPermissionId(role, request.getPermissionId());

        if (request.getGranted()) {
            // Grant permission
            if (existingRp.isPresent()) {
                RolePermission rp = existingRp.get();
                rp.setGranted(true);
                rolePermissionRepository.save(rp);
            } else {
                RolePermission newRp = RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .granted(true)
                        .build();
                rolePermissionRepository.save(newRp);
            }
        } else {
            // Revoke permission
            if (existingRp.isPresent()) {
                RolePermission rp = existingRp.get();
                rp.setGranted(false);
                rolePermissionRepository.save(rp);
            }
        }
    }

    public boolean hasPermission(User.UserRole role, String permissionId) {
        return rolePermissionRepository.findByRoleAndPermissionId(role, permissionId)
                .map(RolePermission::getGranted)
                .orElse(false);
    }
}
