package com.insuretech.pms.auth.dto;

import com.insuretech.pms.auth.entity.Permission;
import com.insuretech.pms.auth.entity.User;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class PermissionDto {

    private String id;
    private String category;
    private String name;
    private String description;
    private String resource;
    private String action;
    private Map<String, Boolean> roles;

    public static PermissionDto fromEntity(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .category(permission.getCategory())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .roles(new HashMap<>())
                .build();
    }
}
