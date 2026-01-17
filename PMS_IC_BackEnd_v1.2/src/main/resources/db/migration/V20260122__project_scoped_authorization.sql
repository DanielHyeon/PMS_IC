-- Migration Script: Project-Scoped Authorization
-- Version: V20260122
-- Description: Add tables and columns for project-scoped authorization

-- 1. Add new columns to project_members for soft-delete and tracking
ALTER TABLE project.project_members
ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. Backfill active status for existing members
UPDATE project.project_members SET active = TRUE WHERE active IS NULL;

-- 3. Create project_role_permissions table (per-project role-permission mapping)
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

-- 4. Create indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_prp_project_role
    ON project.project_role_permissions(project_id, role);
CREATE INDEX IF NOT EXISTS idx_pm_user_active
    ON project.project_members(user_id, active);
CREATE INDEX IF NOT EXISTS idx_pm_project_active
    ON project.project_members(project_id, active);

-- 5. Create default_role_permissions table (template for new projects)
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

-- 6. Add comment on auth.users.role column to clarify system-wide role usage
COMMENT ON COLUMN auth.users.role IS
    'System-wide role only: ADMIN, AUDITOR, or project-based roles (will be deprecated for non-system roles)';
