-- Parts (Sub-Project) Management Tables Migration
-- Version: 20260118
-- Description: Create Parts and Part Members tables for sub-project management

-- Create project schema if not exists
CREATE SCHEMA IF NOT EXISTS project;

-- Parts Table
CREATE TABLE IF NOT EXISTS project.parts (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_id VARCHAR(50) NOT NULL,
    leader_id VARCHAR(50),
    leader_name VARCHAR(100),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    start_date DATE,
    end_date DATE,
    progress INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT fk_parts_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE
);

-- Part Members Table (for ElementCollection)
CREATE TABLE IF NOT EXISTS project.part_members (
    part_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (part_id, user_id),
    CONSTRAINT fk_part_members_part FOREIGN KEY (part_id)
        REFERENCES project.parts(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_parts_project_id ON project.parts(project_id);
CREATE INDEX IF NOT EXISTS idx_parts_leader_id ON project.parts(leader_id);
CREATE INDEX IF NOT EXISTS idx_parts_status ON project.parts(status);
CREATE INDEX IF NOT EXISTS idx_part_members_user_id ON project.part_members(user_id);

-- Comments
COMMENT ON TABLE project.parts IS 'Sub-projects (Parts) within a project';
COMMENT ON TABLE project.part_members IS 'Members assigned to each part';
COMMENT ON COLUMN project.parts.status IS 'ACTIVE, INACTIVE, COMPLETED';
COMMENT ON COLUMN project.parts.progress IS 'Progress percentage (0-100)';
