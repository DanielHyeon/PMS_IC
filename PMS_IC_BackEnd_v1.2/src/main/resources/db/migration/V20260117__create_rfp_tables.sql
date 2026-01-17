-- RFP Management Tables Migration
-- Version: 20260117
-- Description: Create RFP and Requirement tables for RFP management feature

-- Create project schema if not exists
CREATE SCHEMA IF NOT EXISTS project;

-- RFP Table
CREATE TABLE IF NOT EXISTS project.rfps (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    file_path VARCHAR(500),
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    status VARCHAR(50) DEFAULT 'DRAFT',
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    processing_message TEXT,
    submitted_at TIMESTAMP,
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36)
);

-- Create indexes for rfps
CREATE INDEX IF NOT EXISTS idx_rfps_project_id ON project.rfps(project_id);
CREATE INDEX IF NOT EXISTS idx_rfps_status ON project.rfps(status);
CREATE INDEX IF NOT EXISTS idx_rfps_tenant_id ON project.rfps(tenant_id);

-- Requirements Table
CREATE TABLE IF NOT EXISTS project.requirements (
    id VARCHAR(36) PRIMARY KEY,
    rfp_id VARCHAR(36) REFERENCES project.rfps(id) ON DELETE SET NULL,
    project_id VARCHAR(36) NOT NULL,
    requirement_code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    category VARCHAR(50) DEFAULT 'FUNCTIONAL',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(50) DEFAULT 'IDENTIFIED',
    progress INTEGER DEFAULT 0,
    source_text TEXT,
    page_number INTEGER,
    assignee_id VARCHAR(36),
    due_date DATE,
    acceptance_criteria TEXT,
    estimated_effort INTEGER,
    actual_effort INTEGER,
    tenant_id VARCHAR(36) NOT NULL,
    neo4j_node_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36)
);

-- Create indexes for requirements
CREATE INDEX IF NOT EXISTS idx_requirements_project_id ON project.requirements(project_id);
CREATE INDEX IF NOT EXISTS idx_requirements_rfp_id ON project.requirements(rfp_id);
CREATE INDEX IF NOT EXISTS idx_requirements_status ON project.requirements(status);
CREATE INDEX IF NOT EXISTS idx_requirements_category ON project.requirements(category);
CREATE INDEX IF NOT EXISTS idx_requirements_assignee_id ON project.requirements(assignee_id);
CREATE INDEX IF NOT EXISTS idx_requirements_tenant_id ON project.requirements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_requirements_code ON project.requirements(requirement_code);

-- Requirement-Task Links Table (for ElementCollection)
CREATE TABLE IF NOT EXISTS project.requirement_task_links (
    requirement_id VARCHAR(36) NOT NULL REFERENCES project.requirements(id) ON DELETE CASCADE,
    task_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (requirement_id, task_id)
);

-- Create index for requirement_task_links
CREATE INDEX IF NOT EXISTS idx_req_task_links_task_id ON project.requirement_task_links(task_id);

-- Requirement-Sprint Mapping Table (optional, for future use)
CREATE TABLE IF NOT EXISTS project.requirement_sprint_mapping (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    requirement_id VARCHAR(36) NOT NULL REFERENCES project.requirements(id) ON DELETE CASCADE,
    sprint_id VARCHAR(36) NOT NULL,
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),
    CONSTRAINT uk_req_sprint UNIQUE (requirement_id, sprint_id)
);

-- Create index for requirement_sprint_mapping
CREATE INDEX IF NOT EXISTS idx_req_sprint_mapping_sprint ON project.requirement_sprint_mapping(sprint_id);

-- Comments
COMMENT ON TABLE project.rfps IS 'RFP (Request for Proposal) documents';
COMMENT ON TABLE project.requirements IS 'Requirements extracted from RFPs or manually created';
COMMENT ON TABLE project.requirement_task_links IS 'Links between requirements and tasks';
COMMENT ON TABLE project.requirement_sprint_mapping IS 'Mapping between requirements and sprints';

COMMENT ON COLUMN project.rfps.status IS 'DRAFT, SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED';
COMMENT ON COLUMN project.rfps.processing_status IS 'PENDING, EXTRACTING, INDEXING, COMPLETED, FAILED';
COMMENT ON COLUMN project.requirements.category IS 'FUNCTIONAL, NON_FUNCTIONAL, UI, INTEGRATION, SECURITY, AI, SI, COMMON, TECHNICAL, BUSINESS, CONSTRAINT';
COMMENT ON COLUMN project.requirements.priority IS 'CRITICAL, HIGH, MEDIUM, LOW';
COMMENT ON COLUMN project.requirements.status IS 'IDENTIFIED, ANALYZED, APPROVED, IMPLEMENTED, VERIFIED, DEFERRED, REJECTED';
