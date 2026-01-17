-- Project Members Table Migration
-- Version: 20260119
-- Description: Create Project Members table for managing project team membership

-- Create project schema if not exists
CREATE SCHEMA IF NOT EXISTS project;

-- Project Members Table
CREATE TABLE IF NOT EXISTS project.project_members (
    id VARCHAR(50) PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    user_name VARCHAR(100),
    user_email VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_members_project_user UNIQUE (project_id, user_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_project_members_project_id ON project.project_members(project_id);
CREATE INDEX IF NOT EXISTS idx_project_members_user_id ON project.project_members(user_id);
CREATE INDEX IF NOT EXISTS idx_project_members_role ON project.project_members(role);

-- Comments
COMMENT ON TABLE project.project_members IS 'Team members assigned to each project';
COMMENT ON COLUMN project.project_members.role IS 'SPONSOR, PM, PMO_HEAD, DEVELOPER, QA, BUSINESS_ANALYST, AUDITOR, MEMBER';
