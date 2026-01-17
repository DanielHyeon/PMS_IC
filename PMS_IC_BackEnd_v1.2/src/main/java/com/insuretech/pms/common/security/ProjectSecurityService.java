package com.insuretech.pms.common.security;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.project.entity.ProjectMember;
import com.insuretech.pms.project.entity.ProjectMember.ProjectRole;
import com.insuretech.pms.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service for project-scoped authorization checks.
 * Used via SpEL in @PreAuthorize annotations.
 *
 * Usage examples:
 * - @PreAuthorize("@projectSecurity.hasRole(#projectId, 'PM')")
 * - @PreAuthorize("@projectSecurity.hasAnyRole(#projectId, 'PM', 'PMO_HEAD')")
 * - @PreAuthorize("@projectSecurity.isProjectMember(#projectId)")
 * - @PreAuthorize("@projectSecurity.hasSystemRole('ADMIN')")
 */
@Slf4j
@Service("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurityService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    /**
     * Check if current user has the specified role on the project.
     *
     * @param projectId the project ID to check
     * @param requiredRole the required role name (e.g., "PM", "DEVELOPER")
     * @return true if user has the role on this project
     */
    public boolean hasRole(String projectId, String requiredRole) {
        return hasAnyRole(projectId, requiredRole);
    }

    /**
     * Check if current user has any of the specified roles on the project.
     *
     * @param projectId the project ID to check
     * @param requiredRoles the roles to check (any match grants access)
     * @return true if user has any of the specified roles on this project
     */
    public boolean hasAnyRole(String projectId, String... requiredRoles) {
        if (projectId == null || requiredRoles == null || requiredRoles.length == 0) {
            log.debug("hasAnyRole: Invalid parameters - projectId={}, roles={}",
                projectId, Arrays.toString(requiredRoles));
            return false;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            log.debug("hasAnyRole: No authenticated user");
            return false;
        }

        // System admins have access to all projects
        if (hasSystemRole("ADMIN")) {
            log.debug("hasAnyRole: User {} is ADMIN, granting access", userId);
            return true;
        }

        Optional<ProjectMember> memberOpt = projectMemberRepository
            .findByProjectIdAndUserIdAndActiveTrue(projectId, userId);

        if (memberOpt.isEmpty()) {
            log.debug("hasAnyRole: User {} is not an active member of project {}",
                userId, projectId);
            return false;
        }

        String userRole = memberOpt.get().getRole().name();
        boolean hasAccess = Arrays.asList(requiredRoles).contains(userRole);

        log.debug("hasAnyRole: User {} has role {} on project {}, required={}, granted={}",
            userId, userRole, projectId, Arrays.toString(requiredRoles), hasAccess);

        return hasAccess;
    }

    /**
     * Check if current user is a member of the project (any role).
     *
     * @param projectId the project ID to check
     * @return true if user is an active member of this project
     */
    public boolean isProjectMember(String projectId) {
        if (projectId == null) {
            return false;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            return false;
        }

        // System admins and auditors can view all projects
        if (hasSystemRole("ADMIN") || hasSystemRole("AUDITOR")) {
            log.debug("isProjectMember: User {} has system role, granting access", userId);
            return true;
        }

        boolean isMember = projectMemberRepository
            .existsByProjectIdAndUserIdAndActiveTrue(projectId, userId);

        log.debug("isProjectMember: User {} membership on project {}: {}",
            userId, projectId, isMember);

        return isMember;
    }

    /**
     * Check if current user has a system-wide role (ADMIN, AUDITOR).
     * System roles are stored in User.role and apply globally.
     *
     * @param role the system role to check
     * @return true if user has the specified system role
     */
    public boolean hasSystemRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Check if current user has any of the specified system-wide roles.
     *
     * @param roles the system roles to check
     * @return true if user has any of the specified system roles
     */
    public boolean hasAnySystemRole(String... roles) {
        for (String role : roles) {
            if (hasSystemRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the project role of current user for the specified project.
     *
     * @param projectId the project ID
     * @return Optional containing the user's project role, or empty if not a member
     */
    public Optional<ProjectRole> getProjectRole(String projectId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }

        return projectMemberRepository
            .findByProjectIdAndUserIdAndActiveTrue(projectId, userId)
            .map(ProjectMember::getRole);
    }

    /**
     * Check if current user can manage project members (add/remove/update roles).
     * Only PM and PMO_HEAD on the project can manage members.
     *
     * @param projectId the project ID
     * @return true if user can manage members on this project
     */
    public boolean canManageMembers(String projectId) {
        return hasAnyRole(projectId, "PM", "PMO_HEAD");
    }

    /**
     * Check if current user can modify project settings.
     *
     * @param projectId the project ID
     * @return true if user can modify project settings
     */
    public boolean canModifyProject(String projectId) {
        return hasAnyRole(projectId, "PM", "PMO_HEAD", "SPONSOR");
    }

    /**
     * Check if current user can create/modify tasks in the project.
     *
     * @param projectId the project ID
     * @return true if user can work on tasks
     */
    public boolean canWorkOnTasks(String projectId) {
        return hasAnyRole(projectId, "PM", "PMO_HEAD", "DEVELOPER", "QA", "BUSINESS_ANALYST");
    }

    /**
     * Check if current user can create/modify issues in the project.
     *
     * @param projectId the project ID
     * @return true if user can work on issues
     */
    public boolean canWorkOnIssues(String projectId) {
        return hasAnyRole(projectId, "PM", "PMO_HEAD", "DEVELOPER", "QA", "BUSINESS_ANALYST", "SPONSOR");
    }

    /**
     * Check if current user can approve deliverables in the project.
     *
     * @param projectId the project ID
     * @return true if user can approve deliverables
     */
    public boolean canApproveDeliverables(String projectId) {
        return hasAnyRole(projectId, "PM", "PMO_HEAD", "SPONSOR");
    }

    /**
     * Get the current authenticated user's ID.
     *
     * @return the user ID, or null if not authenticated
     */
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        String username = auth.getName();
        return userRepository.findByEmail(username)
            .map(User::getId)
            .orElse(null);
    }

    /**
     * Get the current authenticated user's ID (public version for services).
     *
     * @return the user ID
     * @throws IllegalStateException if not authenticated
     */
    public String getAuthenticatedUserId() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return userId;
    }
}
