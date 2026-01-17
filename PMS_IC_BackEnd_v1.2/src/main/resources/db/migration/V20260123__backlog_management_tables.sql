-- Backlog Management Tables Migration
-- Version: 20260123
-- Description: Create Backlog, BacklogItem tables and extend Requirement for story points and progress tracking

-- ============================================
-- 1. Epic Table (Epic management for enterprise scaling)
-- ============================================

CREATE TABLE IF NOT EXISTS project.epics (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    goal TEXT,
    owner_id VARCHAR(36),
    target_completion_date DATE,
    business_value INTEGER,
    total_story_points INTEGER DEFAULT 0,
    item_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    CONSTRAINT fk_epic_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT uk_project_epic_name UNIQUE (project_id, name)
);

CREATE INDEX IF NOT EXISTS idx_epics_project_id ON project.epics(project_id);
CREATE INDEX IF NOT EXISTS idx_epics_status ON project.epics(status);
CREATE INDEX IF NOT EXISTS idx_epics_owner_id ON project.epics(owner_id);
CREATE INDEX IF NOT EXISTS idx_epics_business_value ON project.epics(business_value);

COMMENT ON TABLE project.epics IS 'Epic entities for grouping related backlog items (enterprise scaling feature)';
COMMENT ON COLUMN project.epics.status IS 'DRAFT, ACTIVE, COMPLETED, CANCELLED';
COMMENT ON COLUMN project.epics.business_value IS 'Business priority or value score for the epic';
COMMENT ON COLUMN project.epics.total_story_points IS 'Cumulative story points of all backlog items in this epic';


-- ============================================
-- 2. Backlog Table (Product Backlog)
-- ============================================

CREATE TABLE IF NOT EXISTS project.backlogs (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) DEFAULT 'Product Backlog',
    description TEXT,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    CONSTRAINT fk_backlog_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_backlogs_project_id ON project.backlogs(project_id);
CREATE INDEX IF NOT EXISTS idx_backlogs_status ON project.backlogs(status);

COMMENT ON TABLE project.backlogs IS 'Product Backlog entities for managing requirements lifecycle';
COMMENT ON COLUMN project.backlogs.status IS 'ACTIVE, ARCHIVED';


-- ============================================
-- 3. Backlog Items Table
-- ============================================

CREATE TABLE IF NOT EXISTS project.backlog_items (
    id VARCHAR(36) PRIMARY KEY,
    backlog_id VARCHAR(36) NOT NULL,
    requirement_id VARCHAR(36),
    origin_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    epic_id_ref VARCHAR(36),
    epic_id VARCHAR(100),
    priority_order INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'BACKLOG',
    story_points INTEGER,
    estimated_effort_hours INTEGER,
    acceptance_criteria TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    CONSTRAINT fk_backlog_item_backlog FOREIGN KEY (backlog_id)
        REFERENCES project.backlogs(id) ON DELETE CASCADE,
    CONSTRAINT fk_backlog_item_requirement FOREIGN KEY (requirement_id)
        REFERENCES project.requirements(id) ON DELETE SET NULL,
    CONSTRAINT fk_backlog_item_epic FOREIGN KEY (epic_id_ref)
        REFERENCES project.epics(id) ON DELETE SET NULL,
    CONSTRAINT uk_backlog_requirement UNIQUE (backlog_id, requirement_id)
);

CREATE INDEX IF NOT EXISTS idx_backlog_items_backlog_id ON project.backlog_items(backlog_id);
CREATE INDEX IF NOT EXISTS idx_backlog_items_requirement_id ON project.backlog_items(requirement_id);
CREATE INDEX IF NOT EXISTS idx_backlog_items_status ON project.backlog_items(status);
CREATE INDEX IF NOT EXISTS idx_backlog_items_priority_order ON project.backlog_items(priority_order);
CREATE INDEX IF NOT EXISTS idx_backlog_items_origin_type ON project.backlog_items(origin_type);
CREATE INDEX IF NOT EXISTS idx_backlog_items_epic_id_ref ON project.backlog_items(epic_id_ref);
CREATE INDEX IF NOT EXISTS idx_backlog_items_epic_id ON project.backlog_items(epic_id);

COMMENT ON TABLE project.backlog_items IS 'Items in Product Backlog - represents requirements or features';
COMMENT ON COLUMN project.backlog_items.origin_type IS 'REQUIREMENT (extracted from Requirement), MANUAL (manually created)';
COMMENT ON COLUMN project.backlog_items.epic_id_ref IS 'Reference to Epic entity (normalized approach)';
COMMENT ON COLUMN project.backlog_items.epic_id IS 'Epic identifier string (lightweight tagging approach)';
COMMENT ON COLUMN project.backlog_items.status IS 'BACKLOG, SELECTED, SPRINT, COMPLETED';
COMMENT ON COLUMN project.backlog_items.priority_order IS 'Order for drag-and-drop prioritization (lower = higher priority)';


-- ============================================
-- 4. Extend Requirement table with Story Points and Progress fields
-- ============================================

-- Add story_points field if not exists
ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS story_points INTEGER;

-- Add effort tracking fields if not exist
ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS estimated_effort_hours INTEGER;

ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS actual_effort_hours INTEGER;

ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS remaining_effort_hours INTEGER;

-- Rename progress column to progress_percentage for clarity
-- Note: If column already exists with this name, this will be skipped
-- If it exists as 'progress', we need to handle the rename
-- For PostgreSQL, we need to check existing state
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'project'
               AND table_name = 'requirements'
               AND column_name = 'progress') THEN
        ALTER TABLE project.requirements
        RENAME COLUMN progress TO progress_percentage;
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema = 'project'
                      AND table_name = 'requirements'
                      AND column_name = 'progress_percentage') THEN
        ALTER TABLE project.requirements
        ADD COLUMN progress_percentage INTEGER DEFAULT 0;
    END IF;
END $$;

-- Add last_progress_update field if not exists
ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS last_progress_update TIMESTAMP;

-- Add progress calculation method field if not exists
ALTER TABLE project.requirements
ADD COLUMN IF NOT EXISTS progress_calc_method VARCHAR(50) DEFAULT 'STORY_POINT';

-- Ensure progress_percentage has default value of 0 if it doesn't
ALTER TABLE project.requirements
ALTER COLUMN progress_percentage SET DEFAULT 0;

-- Create indexes for progress tracking
CREATE INDEX IF NOT EXISTS idx_requirements_progress_percentage ON project.requirements(progress_percentage);
CREATE INDEX IF NOT EXISTS idx_requirements_effort ON project.requirements(estimated_effort_hours);

COMMENT ON COLUMN project.requirements.story_points IS 'Story points estimation (Fibonacci sequence: 1, 2, 3, 5, 8, 13, 21). Optional at creation, mandatory at Ready/Committed state';
COMMENT ON COLUMN project.requirements.estimated_effort_hours IS 'Estimated effort in hours';
COMMENT ON COLUMN project.requirements.actual_effort_hours IS 'Actual effort spent in hours';
COMMENT ON COLUMN project.requirements.remaining_effort_hours IS 'Remaining effort to complete (for burndown tracking)';
COMMENT ON COLUMN project.requirements.progress_percentage IS 'Requirement completion percentage (0-100). Calculated using progressCalcMethod';
COMMENT ON COLUMN project.requirements.progress_calc_method IS 'STORY_POINT (default when SP available), TASK_COUNT (fallback), TIME_BASED (optional)';
COMMENT ON COLUMN project.requirements.last_progress_update IS 'Last time progress was updated';


-- ============================================
-- 5. WIP Limits Configuration Tables (Phase 4 preparation)
-- ============================================

-- Extend KanbanColumn with WIP settings
ALTER TABLE task.kanban_columns
ADD COLUMN IF NOT EXISTS wip_limit_soft INTEGER;

ALTER TABLE task.kanban_columns
ADD COLUMN IF NOT EXISTS wip_limit_hard INTEGER;

ALTER TABLE task.kanban_columns
ADD COLUMN IF NOT EXISTS is_bottleneck_column BOOLEAN DEFAULT FALSE;

-- Extend Sprint with CONWIP settings
ALTER TABLE task.sprints
ADD COLUMN IF NOT EXISTS conwip_limit INTEGER;

ALTER TABLE task.sprints
ADD COLUMN IF NOT EXISTS enable_wip_validation BOOLEAN DEFAULT TRUE;

COMMENT ON COLUMN task.kanban_columns.wip_limit_soft IS 'Soft WIP limit - warning threshold';
COMMENT ON COLUMN task.kanban_columns.wip_limit_hard IS 'Hard WIP limit - enforcement threshold';
COMMENT ON COLUMN task.sprints.conwip_limit IS 'Total WIP limit for the entire sprint (Constant Work In Progress)';


-- ============================================
-- 6. Create initial Backlog for existing projects (one-time data migration)
-- ============================================

-- Insert default Product Backlog for each existing project
INSERT INTO project.backlogs (id, project_id, name, description, status, created_at, updated_at)
SELECT
    gen_random_uuid()::text,
    id,
    'Product Backlog',
    'Default product backlog for ' || name,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM project.projects
WHERE NOT EXISTS (
    SELECT 1 FROM project.backlogs WHERE backlogs.project_id = projects.id
)
ON CONFLICT DO NOTHING;
