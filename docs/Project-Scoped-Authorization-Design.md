# Project-Scoped Authorization System Design

> **Version**: 1.0
> **Date**: 2026-01-17
> **Author**: PMS Insurance Claims Team
> **Status**: Draft - Pending Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Current State Analysis](#3-current-state-analysis)
4. [Proposed Solution Architecture](#4-proposed-solution-architecture)
5. [Database Schema Changes](#5-database-schema-changes)
6. [Backend Implementation](#6-backend-implementation)
7. [Frontend Changes](#7-frontend-changes)
8. [Migration Strategy](#8-migration-strategy)
9. [Security Considerations](#9-security-considerations)
10. [Testing Strategy](#10-testing-strategy)
11. [Implementation Phases](#11-implementation-phases)
12. [Appendix](#appendix)

---

## 1. Executive Summary

### 1.1 Purpose

This document outlines the design for implementing **project-scoped authorization** in PMS Insurance Claims. The current system uses a single global role per user, which prevents users from having different roles on different projects.

### 1.2 Goals

| Goal | Description |
|------|-------------|
| **Multi-role Support** | Allow a user to be PM on Project A and Developer on Project B simultaneously |
| **Project Isolation** | Ensure authorization checks verify roles within the project context |
| **Granular Permissions** | Enable per-project permission management beyond simple role checks |
| **Backward Compatibility** | Maintain global roles for system-wide operations (ADMIN, AUDITOR) |
| **Frontend Alignment** | Provide project-specific role information for UI rendering |

### 1.3 Key Changes

```
BEFORE                              AFTER
======                              =====
User.role (global)                  User.role (system-wide: ADMIN, AUDITOR only)
                                    ProjectMember.role (project-scoped)

@PreAuthorize("hasRole('PM')")      @PreAuthorize("@projectSecurity.hasRole(#projectId, 'PM')")

JWT: {sub: user}                    JWT: {sub: user, projectRoles: {projId: role}}

LoginResponse: {role: "pm"}         LoginResponse: {systemRole, projectRoles: [...]}
```

---

## 2. Problem Statement

### 2.1 Current Limitation

A user assigned to multiple projects with different roles experiences incorrect authorization:

```
Scenario:
- Alice is PM on "Insurance Claims System" project
- Alice is DEVELOPER on "Data Analytics Platform" project

Current Behavior:
- Alice's global role is PM (set during account creation or last update)
- When accessing "Data Analytics Platform", Alice has PM permissions
- This violates the intended access control model

Expected Behavior:
- On "Insurance Claims System": Alice has PM permissions
- On "Data Analytics Platform": Alice has DEVELOPER permissions
```

### 2.2 Root Cause Analysis

| Component | Issue |
|-----------|-------|
| `User.role` | Single role stored globally in `auth.users` table |
| `ProjectMember.role` | Per-project role exists but is NEVER used for authorization |
| `JwtTokenProvider` | Creates JWT with only username, no role claims |
| `UserDetailsServiceImpl` | Loads only `User.role` as `GrantedAuthority` |
| `@PreAuthorize` | All annotations check global `User.role` only |
| Service Layer | No authorization checks at service level |
| `LoginResponse` | Returns only global `User.role` to frontend |

### 2.3 Affected Controllers

Based on investigation, these controllers use role-based authorization:

| Controller | Current Authorization | Impact |
|------------|----------------------|--------|
| `ProjectController` | `@PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")` | PM on any project can modify all projects |
| `DeliverableController` | `@PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")` | Cross-project access possible |
| `IssueController` | `@PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")` | Cross-project access possible |
| `TaskController` | `@PreAuthorize("hasAnyRole('PM', 'DEVELOPER')")` | Cross-project access possible |
| `PhaseController` | `@PreAuthorize("hasRole('PM')")` | Cross-project access possible |
| `UserStoryController` | `@PreAuthorize("hasAnyRole('PM', 'BUSINESS_ANALYST')")` | Cross-project access possible |

---

## 3. Current State Analysis

### 3.1 Existing Entity Relationships

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CURRENT STATE                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  AUTH SCHEMA                          PROJECT SCHEMA                         │
│  ────────────                         ──────────────                         │
│                                                                              │
│  ┌─────────────────┐                  ┌─────────────────┐                   │
│  │     users       │                  │    projects     │                   │
│  ├─────────────────┤                  ├─────────────────┤                   │
│  │ id (PK)        │                  │ id (PK)        │                   │
│  │ email          │                  │ name           │                   │
│  │ role ◄─────────┼── GLOBAL ROLE    │ status         │                   │
│  │ ...            │   (UserRole)     │ ...            │                   │
│  └─────────────────┘                  └────────┬────────┘                   │
│         │                                      │                             │
│         │ (user_id)                           │ (project_id)                │
│         │                                      │                             │
│         │           ┌──────────────────────────┘                             │
│         │           │                                                        │
│         │           ▼                                                        │
│         │  ┌─────────────────────┐                                          │
│         └──│  project_members    │                                          │
│            ├─────────────────────┤                                          │
│            │ id (PK)            │                                          │
│            │ project_id (FK)    │                                          │
│            │ user_id            │─────── NOT FK (denormalized)             │
│            │ role ◄─────────────┼──── PROJECT ROLE (ProjectRole)           │
│            │ user_name          │       ⚠️ NEVER USED FOR AUTHZ            │
│            │ user_email         │                                          │
│            └─────────────────────┘                                          │
│                                                                              │
│  ┌─────────────────┐   ┌─────────────────────┐                              │
│  │   permissions   │   │  role_permissions   │                              │
│  ├─────────────────┤   ├─────────────────────┤                              │
│  │ id (PK)        │◄──│ permission_id (FK)  │                              │
│  │ category       │   │ role (UserRole)     │─── GLOBAL ROLE ONLY          │
│  │ name           │   │ granted             │                              │
│  │ resource       │   └─────────────────────┘                              │
│  │ action         │        ⚠️ NEVER USED                                    │
│  └─────────────────┘                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Role Enums Comparison

| `User.UserRole` (auth schema) | `ProjectMember.ProjectRole` (project schema) |
|-------------------------------|---------------------------------------------|
| SPONSOR | SPONSOR |
| PMO_HEAD | PMO_HEAD |
| PM | PM |
| DEVELOPER | DEVELOPER |
| QA | QA |
| BUSINESS_ANALYST | BUSINESS_ANALYST |
| AUDITOR | AUDITOR |
| ADMIN | MEMBER (additional) |

**Observations:**
- `ADMIN` exists only in global role (correct - system admin)
- `MEMBER` exists only in project role (generic project membership)
- Other roles are duplicated, which creates confusion

### 3.3 JWT Token Structure (Current)

```java
// JwtTokenProvider.java
return Jwts.builder()
    .subject(username)           // Only username
    .issuedAt(now)
    .expiration(expiryDate)
    .signWith(secretKey)
    .compact();
// NO ROLES INCLUDED
```

### 3.4 Spring Security Configuration (Current)

```java
// UserDetailsServiceImpl.java
private Collection<? extends GrantedAuthority> getAuthorities(User user) {
    return Collections.singletonList(
        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
    );
}
// Only User.role is loaded as authority
```

---

## 4. Proposed Solution Architecture

### 4.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PROPOSED ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          Controller Layer                            │   │
│  │  ┌───────────────────────────────────────────────────────────────┐  │   │
│  │  │ @PreAuthorize("@projectSecurity.hasRole(#projectId, 'PM')")   │  │   │
│  │  │ public ResponseEntity<...> updateProject(@PathVariable        │  │   │
│  │  │     String projectId, ...) { ... }                            │  │   │
│  │  └───────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────────┬─────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      ProjectSecurityService                          │   │
│  │  ┌───────────────────────────────────────────────────────────────┐  │   │
│  │  │ • hasRole(projectId, requiredRole)                            │  │   │
│  │  │ • hasAnyRole(projectId, roles...)                             │  │   │
│  │  │ • hasPermission(projectId, resource, action)                  │  │   │
│  │  │ • isProjectMember(projectId)                                  │  │   │
│  │  │ • hasSystemRole(requiredRole) - for ADMIN/AUDITOR            │  │   │
│  │  └───────────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────────────┬─────────────────────────────────┘   │
│                                      │                                      │
│          ┌───────────────────────────┴───────────────────────────┐         │
│          │                                                        │         │
│          ▼                                                        ▼         │
│  ┌─────────────────────────┐               ┌─────────────────────────┐     │
│  │ ProjectMemberRepository │               │ ProjectRolePermission   │     │
│  │                         │               │ Repository              │     │
│  │ findByProjectIdAnd      │               │                         │     │
│  │   UserId()              │               │ findByProjectIdAndRole  │     │
│  └───────────┬─────────────┘               │   AndPermission()       │     │
│              │                              └───────────┬─────────────┘     │
│              │                                          │                   │
│              ▼                                          ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                          DATABASE LAYER                              │   │
│  │                                                                      │   │
│  │   PROJECT SCHEMA                                                     │   │
│  │   ┌─────────────────────┐    ┌─────────────────────────┐            │   │
│  │   │  project_members    │    │ project_role_permissions │            │   │
│  │   ├─────────────────────┤    ├─────────────────────────┤            │   │
│  │   │ project_id          │    │ project_id              │            │   │
│  │   │ user_id             │    │ role                    │            │   │
│  │   │ role (ProjectRole)  │    │ permission_id           │            │   │
│  │   └─────────────────────┘    │ granted                 │            │   │
│  │                              └─────────────────────────┘            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Role Classification

| Role Type | Where Stored | Purpose | Example Roles |
|-----------|--------------|---------|---------------|
| **System Role** | `auth.users.role` | System-wide access | ADMIN, AUDITOR |
| **Project Role** | `project.project_members.role` | Project-scoped access | PM, DEVELOPER, QA, BA, SPONSOR, PMO_HEAD |

### 4.3 Authorization Decision Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        AUTHORIZATION DECISION FLOW                          │
└────────────────────────────────────────────────────────────────────────────┘

Request: PUT /api/projects/{projectId}/issues/{issueId}
Required: PM or DEVELOPER role on this project

      ┌─────────────────────────────┐
      │      Incoming Request       │
      │  Authorization: Bearer JWT  │
      └──────────────┬──────────────┘
                     │
                     ▼
      ┌─────────────────────────────┐
      │   JwtAuthenticationFilter   │
      │ Extract username from JWT   │
      └──────────────┬──────────────┘
                     │
                     ▼
      ┌─────────────────────────────┐
      │   SecurityContextHolder     │
      │ Set Authentication object   │
      │ (username + system role)    │
      └──────────────┬──────────────┘
                     │
                     ▼
      ┌─────────────────────────────┐
      │   @PreAuthorize Evaluation  │
      │ @projectSecurity.hasAnyRole │
      │ (#projectId, 'PM', 'DEV')   │
      └──────────────┬──────────────┘
                     │
                     ▼
      ┌─────────────────────────────┐
      │   ProjectSecurityService    │
      │                             │
      │ 1. Get current user ID      │
      │ 2. Query project_members    │
      │ 3. Check if user's role     │
      │    matches required roles   │
      └──────────────┬──────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
   ┌─────────────┐       ┌─────────────┐
   │   GRANTED   │       │   DENIED    │
   │ Continue to │       │ 403 Forbid  │
   │ Controller  │       │             │
   └─────────────┘       └─────────────┘
```

---

## 5. Database Schema Changes

### 5.1 New Tables

#### 5.1.1 Project Role Permissions Table

```sql
-- Per-project role-permission mapping (optional granular control)
CREATE TABLE IF NOT EXISTS project.project_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(50) NOT NULL REFERENCES project.projects(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    permission_id VARCHAR(100) NOT NULL REFERENCES auth.permissions(id),
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, role, permission_id)
);

-- Index for efficient lookups
CREATE INDEX idx_prp_project_role ON project.project_role_permissions(project_id, role);
CREATE INDEX idx_prp_permission ON project.project_role_permissions(permission_id);
```

#### 5.1.2 Default Role Permissions Table

```sql
-- Default permissions for each project role (template for new projects)
CREATE TABLE IF NOT EXISTS project.default_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role VARCHAR(50) NOT NULL,
    permission_id VARCHAR(100) NOT NULL REFERENCES auth.permissions(id),
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role, permission_id)
);
```

### 5.2 Modified Tables

#### 5.2.1 User Table Update

```sql
-- Update auth.users.role to only store system roles
-- Existing roles become "legacy" and project-specific

-- Add constraint or modify column comment
COMMENT ON COLUMN auth.users.role IS
    'System-wide role only: ADMIN, AUDITOR, or NULL for project-only users';

-- For new users without system role, default to null or remove NOT NULL
ALTER TABLE auth.users ALTER COLUMN role DROP NOT NULL;
```

#### 5.2.2 ProjectMember Table Enhancement

```sql
-- Add user foreign key relationship (currently denormalized)
ALTER TABLE project.project_members
ADD CONSTRAINT fk_project_members_user
FOREIGN KEY (user_id) REFERENCES auth.users(id);

-- Add active status for soft-delete membership
ALTER TABLE project.project_members
ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;

-- Add joined_at timestamp
ALTER TABLE project.project_members
ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
```

### 5.3 Updated ERD

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PROPOSED STATE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  AUTH SCHEMA                          PROJECT SCHEMA                         │
│  ────────────                         ──────────────                         │
│                                                                              │
│  ┌─────────────────┐                  ┌─────────────────┐                   │
│  │     users       │                  │    projects     │                   │
│  ├─────────────────┤                  ├─────────────────┤                   │
│  │ id (PK)        │◄─────────────────┤ id (PK)        │                   │
│  │ email          │                  │ name           │                   │
│  │ role ◄─────────┼── SYSTEM ROLE    │ status         │                   │
│  │ (ADMIN|AUDITOR │   ONLY (nullable)│ ...            │                   │
│  │  |NULL)        │                  └────────┬────────┘                   │
│  └────────┬────────┘                          │                             │
│           │                                    │                             │
│           │ (user_id - FK)                    │ (project_id - FK)           │
│           │                                    │                             │
│           │           ┌────────────────────────┘                             │
│           │           │                                                      │
│           │           ▼                                                      │
│           │  ┌─────────────────────┐        ┌─────────────────────────┐    │
│           └─►│  project_members    │        │ project_role_permissions │    │
│              ├─────────────────────┤        ├─────────────────────────┤    │
│              │ id (PK)            │        │ id (PK)                 │    │
│              │ project_id (FK)────┼────────┤ project_id (FK)         │    │
│              │ user_id (FK)       │        │ role                    │    │
│              │ role ◄─────────────┼─ PROJECT ROLE (used for authz)   │    │
│              │ active             │        │ permission_id (FK)      │    │
│              │ joined_at          │        │ granted                 │    │
│              └─────────────────────┘        └───────────┬─────────────┘    │
│                                                         │                   │
│  ┌─────────────────┐                                    │                   │
│  │   permissions   │◄───────────────────────────────────┘                   │
│  ├─────────────────┤                                                        │
│  │ id (PK)        │                                                        │
│  │ category       │◄─── Shared permission definitions                      │
│  │ name           │     (resource + action based)                          │
│  │ resource       │                                                        │
│  │ action         │                                                        │
│  └─────────────────┘                                                        │
│                                                                              │
│  ┌─────────────────────────┐                                                │
│  │ default_role_permissions │                                               │
│  ├─────────────────────────┤                                                │
│  │ role                    │◄─── Template for new projects                  │
│  │ permission_id (FK)      │                                                │
│  │ granted                 │                                                │
│  └─────────────────────────┘                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Backend Implementation

### 6.1 New Components

#### 6.1.1 ProjectSecurityService

```java
package com.insuretech.pms.common.security;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.project.entity.ProjectMember;
import com.insuretech.pms.project.entity.ProjectMember.ProjectRole;
import com.insuretech.pms.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Service for project-scoped authorization checks.
 * Used via SpEL in @PreAuthorize annotations.
 *
 * Usage: @PreAuthorize("@projectSecurity.hasRole(#projectId, 'PM')")
 */
@Service("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurityService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    /**
     * Check if current user has the specified role on the project.
     */
    public boolean hasRole(String projectId, String requiredRole) {
        return hasAnyRole(projectId, requiredRole);
    }

    /**
     * Check if current user has any of the specified roles on the project.
     */
    public boolean hasAnyRole(String projectId, String... requiredRoles) {
        String userId = getCurrentUserId();
        if (userId == null) return false;

        // System admins have access to all projects
        if (hasSystemRole("ADMIN")) return true;

        Optional<ProjectMember> member = projectMemberRepository
            .findByProjectIdAndUserIdAndActiveTrue(projectId, userId);

        if (member.isEmpty()) return false;

        String userRole = member.get().getRole().name();
        return Arrays.asList(requiredRoles).contains(userRole);
    }

    /**
     * Check if current user is a member of the project (any role).
     */
    public boolean isProjectMember(String projectId) {
        String userId = getCurrentUserId();
        if (userId == null) return false;

        if (hasSystemRole("ADMIN") || hasSystemRole("AUDITOR")) return true;

        return projectMemberRepository
            .existsByProjectIdAndUserIdAndActiveTrue(projectId, userId);
    }

    /**
     * Check if current user has specific permission on the project.
     */
    public boolean hasPermission(String projectId, String resource, String action) {
        String userId = getCurrentUserId();
        if (userId == null) return false;

        if (hasSystemRole("ADMIN")) return true;

        // TODO: Implement granular permission check via project_role_permissions
        // For MVP, fall back to role-based check
        return isProjectMember(projectId);
    }

    /**
     * Check if current user has a system-wide role (ADMIN, AUDITOR).
     */
    public boolean hasSystemRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Get the project role of current user for the specified project.
     */
    public Optional<ProjectRole> getProjectRole(String projectId) {
        String userId = getCurrentUserId();
        if (userId == null) return Optional.empty();

        return projectMemberRepository
            .findByProjectIdAndUserIdAndActiveTrue(projectId, userId)
            .map(ProjectMember::getRole);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        String username = auth.getName();
        return userRepository.findByEmail(username)
            .map(User::getId)
            .orElse(null);
    }
}
```

#### 6.1.2 ProjectMemberRepository Updates

```java
package com.insuretech.pms.project.repository;

import com.insuretech.pms.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {

    Optional<ProjectMember> findByProjectIdAndUserId(String projectId, String userId);

    Optional<ProjectMember> findByProjectIdAndUserIdAndActiveTrue(
        String projectId, String userId);

    boolean existsByProjectIdAndUserIdAndActiveTrue(String projectId, String userId);

    List<ProjectMember> findByUserIdAndActiveTrue(String userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.userId = :userId AND pm.active = true")
    List<ProjectMember> findActiveProjectsForUser(@Param("userId") String userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.active = true")
    List<ProjectMember> findActiveMembers(@Param("projectId") String projectId);
}
```

#### 6.1.3 Enhanced LoginResponse

```java
package com.insuretech.pms.auth.dto;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.project.entity.ProjectMember;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private UserInfo user;
    private List<ProjectRoleInfo> projectRoles;

    @Getter
    @Builder
    public static class UserInfo {
        private String id;
        private String email;
        private String name;
        private String systemRole;  // Only ADMIN or AUDITOR, null otherwise
        private String department;

        public static UserInfo from(User user) {
            String systemRole = null;
            if (user.getRole() != null) {
                String role = user.getRole().name();
                if ("ADMIN".equals(role) || "AUDITOR".equals(role)) {
                    systemRole = role.toLowerCase();
                }
            }
            return UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .systemRole(systemRole)
                .department(user.getDepartment())
                .build();
        }
    }

    @Getter
    @Builder
    public static class ProjectRoleInfo {
        private String projectId;
        private String projectName;
        private String role;

        public static ProjectRoleInfo from(ProjectMember member) {
            return ProjectRoleInfo.builder()
                .projectId(member.getProject().getId())
                .projectName(member.getProject().getName())
                .role(member.getRole().name().toLowerCase())
                .build();
        }
    }
}
```

### 6.2 Controller Migration Examples

#### 6.2.1 ProjectController (Before)

```java
@PreAuthorize("hasAnyRole('PMO_HEAD', 'PM')")
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
    @PathVariable String id,
    @Valid @RequestBody ProjectDto projectDto
) {
    // ...
}
```

#### 6.2.2 ProjectController (After)

```java
@PreAuthorize("@projectSecurity.hasAnyRole(#id, 'PMO_HEAD', 'PM')")
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
    @PathVariable String id,
    @Valid @RequestBody ProjectDto projectDto
) {
    // ...
}
```

#### 6.2.3 IssueController Migration

```java
// Before
@PreAuthorize("hasAnyRole('PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
@PostMapping("/{projectId}/issues")
public ResponseEntity<ApiResponse<IssueDto>> createIssue(
    @PathVariable String projectId,
    @Valid @RequestBody IssueDto issueDto
) { ... }

// After
@PreAuthorize("@projectSecurity.hasAnyRole(#projectId, 'PMO_HEAD', 'PM', 'DEVELOPER', 'QA')")
@PostMapping("/{projectId}/issues")
public ResponseEntity<ApiResponse<IssueDto>> createIssue(
    @PathVariable String projectId,
    @Valid @RequestBody IssueDto issueDto
) { ... }
```

### 6.3 AuthService Updates

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> CustomException.unauthorized("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw CustomException.unauthorized("Invalid credentials");
        }

        if (!user.getActive()) {
            throw CustomException.unauthorized("Account is inactive");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate token
        String token = jwtTokenProvider.createToken(user.getEmail());

        // Get all project memberships for this user
        List<ProjectMember> memberships = projectMemberRepository
            .findByUserIdAndActiveTrue(user.getId());

        List<LoginResponse.ProjectRoleInfo> projectRoles = memberships.stream()
            .map(LoginResponse.ProjectRoleInfo::from)
            .toList();

        return LoginResponse.builder()
            .accessToken(token)
            .user(LoginResponse.UserInfo.from(user))
            .projectRoles(projectRoles)
            .build();
    }
}
```

### 6.4 Optional: JWT Enhancement

For better frontend performance, project roles can be included in JWT:

```java
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey secretKey;
    private final ProjectMemberRepository projectMemberRepository;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String username, String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        // Build project roles map
        Map<String, String> projectRoles = projectMemberRepository
            .findByUserIdAndActiveTrue(userId)
            .stream()
            .collect(Collectors.toMap(
                pm -> pm.getProject().getId(),
                pm -> pm.getRole().name()
            ));

        return Jwts.builder()
            .subject(username)
            .claim("projectRoles", projectRoles)  // Add project roles
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact();
    }
}
```

> **Note**: Including project roles in JWT means the token must be refreshed when roles change. This is a trade-off between performance and real-time accuracy.

---

## 7. Frontend Changes

### 7.1 Auth Context Updates

```typescript
// types/auth.ts
export interface User {
  id: string;
  email: string;
  name: string;
  systemRole: 'admin' | 'auditor' | null;
  department: string;
}

export interface ProjectRole {
  projectId: string;
  projectName: string;
  role: 'sponsor' | 'pmo_head' | 'pm' | 'developer' | 'qa' | 'business_analyst' | 'member';
}

export interface AuthState {
  user: User | null;
  projectRoles: ProjectRole[];
  token: string | null;
  currentProjectId: string | null;
}
```

### 7.2 Permission Helpers

```typescript
// utils/permissions.ts
export function hasProjectRole(
  projectRoles: ProjectRole[],
  projectId: string,
  ...requiredRoles: string[]
): boolean {
  const membership = projectRoles.find(pr => pr.projectId === projectId);
  if (!membership) return false;
  return requiredRoles.includes(membership.role);
}

export function isProjectMember(
  projectRoles: ProjectRole[],
  projectId: string
): boolean {
  return projectRoles.some(pr => pr.projectId === projectId);
}

export function getCurrentProjectRole(
  projectRoles: ProjectRole[],
  projectId: string
): string | null {
  return projectRoles.find(pr => pr.projectId === projectId)?.role ?? null;
}
```

### 7.3 UI Conditional Rendering

```tsx
// components/IssueActions.tsx
function IssueActions({ projectId, issue }: IssueActionsProps) {
  const { projectRoles, user } = useAuth();

  const canEdit = user?.systemRole === 'admin' ||
    hasProjectRole(projectRoles, projectId, 'pm', 'pmo_head');

  const canDelete = user?.systemRole === 'admin' ||
    hasProjectRole(projectRoles, projectId, 'pm');

  return (
    <div>
      {canEdit && <Button onClick={handleEdit}>Edit</Button>}
      {canDelete && <Button variant="destructive" onClick={handleDelete}>Delete</Button>}
    </div>
  );
}
```

---

## 8. Migration Strategy

### 8.1 Migration Phases

```
Phase 1: Schema Migration (Non-breaking)
├── Add new tables (project_role_permissions, default_role_permissions)
├── Add columns to project_members (active, joined_at)
├── Create indexes
└── Backfill active = true for existing members

Phase 2: Backend Implementation
├── Implement ProjectSecurityService
├── Update ProjectMemberRepository
├── Update AuthService for LoginResponse
└── Unit tests for security service

Phase 3: Controller Migration (Gradual)
├── Start with least-used controllers
├── Update @PreAuthorize annotations one by one
├── Integration tests for each controller
└── Monitor for 403 errors in staging

Phase 4: Frontend Updates
├── Update auth types and context
├── Implement permission helpers
├── Update UI components
└── E2E tests

Phase 5: Cleanup
├── Make User.role nullable
├── Remove unused global role logic
├── Update documentation
└── Performance optimization
```

### 8.2 Data Migration Script

```sql
-- Migration Script: Project-Scoped Authorization
-- Version: 1.0
-- Date: 2026-01-17

BEGIN;

-- 1. Add new columns to project_members
ALTER TABLE project.project_members
ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. Backfill active status for existing members
UPDATE project.project_members SET active = TRUE WHERE active IS NULL;

-- 3. Create project_role_permissions table
CREATE TABLE IF NOT EXISTS project.project_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    permission_id VARCHAR(100) NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prp_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_prp_permission FOREIGN KEY (permission_id)
        REFERENCES auth.permissions(id),
    CONSTRAINT uq_prp_project_role_perm UNIQUE(project_id, role, permission_id)
);

-- 4. Create indexes
CREATE INDEX IF NOT EXISTS idx_prp_project_role
    ON project.project_role_permissions(project_id, role);
CREATE INDEX IF NOT EXISTS idx_pm_user_active
    ON project.project_members(user_id, active);
CREATE INDEX IF NOT EXISTS idx_pm_project_active
    ON project.project_members(project_id, active);

-- 5. Create default_role_permissions table
CREATE TABLE IF NOT EXISTS project.default_role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role VARCHAR(50) NOT NULL,
    permission_id VARCHAR(100) NOT NULL,
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_drp_permission FOREIGN KEY (permission_id)
        REFERENCES auth.permissions(id),
    CONSTRAINT uq_drp_role_perm UNIQUE(role, permission_id)
);

COMMIT;
```

### 8.3 Rollback Script

```sql
-- Rollback Script: Project-Scoped Authorization
-- Use in case of issues

BEGIN;

-- Remove new tables
DROP TABLE IF EXISTS project.project_role_permissions;
DROP TABLE IF EXISTS project.default_role_permissions;

-- Remove new columns (optional - can keep for data preservation)
-- ALTER TABLE project.project_members DROP COLUMN IF EXISTS active;
-- ALTER TABLE project.project_members DROP COLUMN IF EXISTS joined_at;

COMMIT;
```

---

## 9. Security Considerations

### 9.1 Authorization Bypass Prevention

| Threat | Mitigation |
|--------|------------|
| Direct object reference | Always validate project membership before resource access |
| Role escalation | Validate role changes at service layer |
| Cross-project access | Include projectId validation in every check |
| JWT tampering | Roles in JWT are read-only, actual check uses DB |

### 9.2 Audit Logging

```java
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditSecurityCheck(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String username = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        try {
            Object result = joinPoint.proceed();
            logSuccess(username, methodName);
            return result;
        } catch (AccessDeniedException e) {
            logDenied(username, methodName);
            throw e;
        }
    }
}
```

### 9.3 Rate Limiting

Consider implementing rate limiting for authorization-heavy endpoints to prevent enumeration attacks.

---

## 10. Testing Strategy

### 10.1 Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class ProjectSecurityServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectSecurityService projectSecurityService;

    @Test
    void hasRole_whenUserIsPmOnProject_shouldReturnTrue() {
        // Given
        String projectId = "proj-001";
        String userId = "user-001";

        ProjectMember member = ProjectMember.builder()
            .projectId(projectId)
            .userId(userId)
            .role(ProjectRole.PM)
            .active(true)
            .build();

        when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(projectId, userId))
            .thenReturn(Optional.of(member));

        // When (with mocked SecurityContext)
        boolean result = projectSecurityService.hasRole(projectId, "PM");

        // Then
        assertTrue(result);
    }

    @Test
    void hasRole_whenUserIsDeveloperButPmRequired_shouldReturnFalse() {
        // Given
        String projectId = "proj-001";
        String userId = "user-001";

        ProjectMember member = ProjectMember.builder()
            .role(ProjectRole.DEVELOPER)
            .active(true)
            .build();

        when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(projectId, userId))
            .thenReturn(Optional.of(member));

        // When
        boolean result = projectSecurityService.hasRole(projectId, "PM");

        // Then
        assertFalse(result);
    }

    @Test
    void hasRole_whenUserNotMemberOfProject_shouldReturnFalse() {
        // Given
        String projectId = "proj-001";
        String userId = "user-001";

        when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(projectId, userId))
            .thenReturn(Optional.empty());

        // When
        boolean result = projectSecurityService.hasRole(projectId, "PM");

        // Then
        assertFalse(result);
    }

    @Test
    void hasRole_whenUserIsSystemAdmin_shouldReturnTrueForAnyProject() {
        // Given: User has ADMIN system role in SecurityContext
        // When
        boolean result = projectSecurityService.hasRole("any-project", "PM");

        // Then
        assertTrue(result);
    }
}
```

### 10.2 Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    @WithMockUser(username = "pm@example.com")
    void updateProject_whenUserIsPmOnProject_shouldSucceed() throws Exception {
        // Given: User is PM on project-001
        String projectId = "project-001";

        mockMvc.perform(put("/api/projects/{id}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Updated Name\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dev@example.com")
    void updateProject_whenUserIsDeveloperOnProject_shouldReturn403() throws Exception {
        // Given: User is DEVELOPER on project-001
        String projectId = "project-001";

        mockMvc.perform(put("/api/projects/{id}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Updated Name\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "pm@example.com")
    void updateProject_whenUserIsPmOnDifferentProject_shouldReturn403() throws Exception {
        // Given: User is PM on project-001, but accessing project-002
        String projectId = "project-002";

        mockMvc.perform(put("/api/projects/{id}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Updated Name\"}"))
            .andExpect(status().isForbidden());
    }
}
```

### 10.3 E2E Test Scenarios

| Scenario | Expected Result |
|----------|-----------------|
| PM on Project A accesses Project A | 200 OK |
| PM on Project A accesses Project B | 403 Forbidden |
| DEVELOPER on Project A tries to delete issue on Project A | 403 Forbidden |
| ADMIN accesses any project | 200 OK |
| AUDITOR views any project | 200 OK |
| User not assigned to any project | 403 Forbidden |

---

## 11. Implementation Phases

### Phase 1: Foundation (Week 1)

| Task | Priority | Effort |
|------|----------|--------|
| Database schema migration | High | 2h |
| ProjectSecurityService implementation | High | 4h |
| Unit tests for ProjectSecurityService | High | 4h |
| ProjectMemberRepository updates | High | 2h |

### Phase 2: Backend Migration (Week 2)

| Task | Priority | Effort |
|------|----------|--------|
| Update AuthService for new LoginResponse | High | 2h |
| Migrate ProjectController | High | 2h |
| Migrate IssueController | High | 2h |
| Migrate DeliverableController | Medium | 2h |
| Migrate TaskController | Medium | 2h |
| Integration tests | High | 4h |

### Phase 3: Frontend Updates (Week 3)

| Task | Priority | Effort |
|------|----------|--------|
| Update auth types and context | High | 2h |
| Implement permission helpers | High | 2h |
| Update UI components | Medium | 4h |
| E2E tests | Medium | 4h |

### Phase 4: Cleanup & Documentation (Week 4)

| Task | Priority | Effort |
|------|----------|--------|
| Make User.role nullable | Low | 1h |
| Remove legacy role checks | Low | 2h |
| Update API documentation | Medium | 2h |
| Performance testing | Medium | 2h |
| Security review | High | 2h |

---

## Appendix

### A. API Changes Summary

| Endpoint | Current Auth | New Auth |
|----------|--------------|----------|
| `PUT /projects/{id}` | `hasRole('PM')` | `@projectSecurity.hasAnyRole(#id, 'PM', 'PMO_HEAD')` |
| `DELETE /projects/{id}` | `hasRole('PMO_HEAD')` | `@projectSecurity.hasRole(#id, 'PMO_HEAD')` |
| `POST /projects/{id}/issues` | `hasAnyRole('PM','DEV','QA')` | `@projectSecurity.hasAnyRole(#id, 'PM','DEVELOPER','QA')` |
| `PUT /projects/{id}/issues/{issueId}` | `hasAnyRole('PM','DEV')` | `@projectSecurity.hasAnyRole(#id, 'PM','DEVELOPER')` |
| `POST /phases/{id}/deliverables` | `hasRole('PM')` | `@projectSecurity.hasRole(#projectId, 'PM')` |

### B. Role Permission Matrix (Default)

| Permission | SPONSOR | PMO_HEAD | PM | DEVELOPER | QA | BA | MEMBER |
|------------|---------|----------|----|-----------|----|----|----|
| project.view | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| project.edit | ✓ | ✓ | ✓ | - | - | - | - |
| project.delete | - | ✓ | - | - | - | - | - |
| issue.create | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| issue.edit | ✓ | ✓ | ✓ | ✓ | ✓ | - | - |
| issue.delete | - | ✓ | ✓ | - | - | - | - |
| task.create | - | ✓ | ✓ | ✓ | - | ✓ | - |
| task.assign | - | ✓ | ✓ | - | - | - | - |
| deliverable.upload | - | ✓ | ✓ | ✓ | ✓ | ✓ | - |
| deliverable.approve | ✓ | ✓ | ✓ | - | - | - | - |
| member.add | - | ✓ | ✓ | - | - | - | - |
| member.remove | - | ✓ | ✓ | - | - | - | - |

### C. References

- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)

---

**Document Status**: Draft - Pending Review

**Reviewers**:
- [ ] Backend Lead
- [ ] Security Team
- [ ] Frontend Lead
- [ ] Product Owner

**Approval Date**: TBD
