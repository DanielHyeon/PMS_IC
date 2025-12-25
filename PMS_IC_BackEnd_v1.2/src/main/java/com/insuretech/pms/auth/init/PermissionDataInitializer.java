package com.insuretech.pms.auth.init;

import com.insuretech.pms.auth.entity.Permission;
import com.insuretech.pms.auth.entity.RolePermission;
import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.PermissionRepository;
import com.insuretech.pms.auth.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
@RequiredArgsConstructor
public class PermissionDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    public void run(String... args) {
        if (permissionRepository.count() > 0) {
            return; // Already initialized
        }

        List<Permission> permissions = createPermissions();
        permissionRepository.saveAll(permissions);

        List<RolePermission> rolePermissions = createRolePermissions(permissions);
        rolePermissionRepository.saveAll(rolePermissions);
    }

    private List<Permission> createPermissions() {
        List<Permission> permissions = new ArrayList<>();

        permissions.add(Permission.builder()
                .id("view_dashboard")
                .category("대시보드")
                .name("전사 프로젝트 대시보드 조회")
                .description("전체 프로젝트 현황을 대시보드에서 확인")
                .resource("dashboard")
                .action("view")
                .build());

        permissions.add(Permission.builder()
                .id("create_project")
                .category("프로젝트")
                .name("프로젝트 생성")
                .description("새로운 프로젝트 생성")
                .resource("project")
                .action("create")
                .build());

        permissions.add(Permission.builder()
                .id("delete_project")
                .category("프로젝트")
                .name("프로젝트 삭제")
                .description("프로젝트 삭제")
                .resource("project")
                .action("delete")
                .build());

        permissions.add(Permission.builder()
                .id("manage_wbs")
                .category("일정관리")
                .name("WBS 작성 및 수정")
                .description("작업 분해 구조 관리")
                .resource("wbs")
                .action("manage")
                .build());

        permissions.add(Permission.builder()
                .id("manage_budget")
                .category("예산관리")
                .name("예산 편성 및 수정")
                .description("프로젝트 예산 계획 및 수정")
                .resource("budget")
                .action("manage")
                .build());

        permissions.add(Permission.builder()
                .id("approve_budget")
                .category("예산관리")
                .name("예산 최종 승인")
                .description("예산 계획 최종 승인")
                .resource("budget")
                .action("approve")
                .build());

        permissions.add(Permission.builder()
                .id("manage_risk")
                .category("리스크/이슈")
                .name("리스크 및 이슈 등록/수정")
                .description("리스크와 이슈 관리")
                .resource("risk")
                .action("manage")
                .build());

        permissions.add(Permission.builder()
                .id("approve_deliverable")
                .category("산출물")
                .name("산출물 승인/반려")
                .description("프로젝트 산출물 검토 및 승인")
                .resource("deliverable")
                .action("approve")
                .build());

        permissions.add(Permission.builder()
                .id("manage_backlog")
                .category("애자일")
                .name("백로그 관리")
                .description("제품 백로그 생성 및 관리")
                .resource("backlog")
                .action("manage")
                .build());

        permissions.add(Permission.builder()
                .id("manage_sprint")
                .category("애자일")
                .name("스프린트 관리")
                .description("스프린트 계획 및 실행 관리")
                .resource("sprint")
                .action("manage")
                .build());

        permissions.add(Permission.builder()
                .id("use_ai_assistant")
                .category("AI 기능")
                .name("AI 어시스턴트 사용")
                .description("AI 챗봇을 활용한 업무 지원")
                .resource("ai_assistant")
                .action("use")
                .build());

        permissions.add(Permission.builder()
                .id("view_audit_log")
                .category("보안/감사")
                .name("감사 로그 조회")
                .description("시스템 감사 로그 확인")
                .resource("audit_log")
                .action("view")
                .build());

        permissions.add(Permission.builder()
                .id("manage_users")
                .category("보안/감사")
                .name("사용자 및 권한 관리")
                .description("사용자 계정 및 권한 관리")
                .resource("users")
                .action("manage")
                .build());

        return permissions;
    }

    private List<RolePermission> createRolePermissions(List<Permission> permissions) {
        List<RolePermission> rolePermissions = new ArrayList<>();

        // SPONSOR
        addRolePermission(rolePermissions, permissions, User.UserRole.SPONSOR,
                "view_dashboard", "manage_budget", "approve_budget", "approve_deliverable", "use_ai_assistant");

        // PMO_HEAD
        addRolePermission(rolePermissions, permissions, User.UserRole.PMO_HEAD,
                "view_dashboard", "create_project", "delete_project", "manage_wbs", "manage_budget",
                "manage_risk", "approve_deliverable", "use_ai_assistant", "view_audit_log");

        // PM
        addRolePermission(rolePermissions, permissions, User.UserRole.PM,
                "view_dashboard", "create_project", "manage_wbs", "manage_risk", "approve_deliverable",
                "manage_backlog", "manage_sprint", "use_ai_assistant");

        // DEVELOPER
        addRolePermission(rolePermissions, permissions, User.UserRole.DEVELOPER,
                "manage_risk", "manage_backlog", "manage_sprint", "use_ai_assistant");

        // QA
        addRolePermission(rolePermissions, permissions, User.UserRole.QA,
                "manage_risk", "manage_backlog", "manage_sprint", "use_ai_assistant");

        // BUSINESS_ANALYST
        addRolePermission(rolePermissions, permissions, User.UserRole.BUSINESS_ANALYST,
                "manage_backlog", "use_ai_assistant");

        // AUDITOR
        addRolePermission(rolePermissions, permissions, User.UserRole.AUDITOR,
                "view_dashboard", "view_audit_log");

        // ADMIN
        addRolePermission(rolePermissions, permissions, User.UserRole.ADMIN,
                "create_project", "delete_project", "view_audit_log", "manage_users");

        return rolePermissions;
    }

    private void addRolePermission(List<RolePermission> rolePermissions, List<Permission> permissions,
                                   User.UserRole role, String... permissionIds) {
        for (String permissionId : permissionIds) {
            Permission permission = permissions.stream()
                    .filter(p -> p.getId().equals(permissionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

            rolePermissions.add(RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .granted(true)
                    .build());
        }
    }
}
