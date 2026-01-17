-- Scrum Workflow Enhancement Migration
-- Creates tables and columns for complete Scrum traceability:
-- Requirement -> UserStory -> Task lineage tracking

-- ============================================
-- 1. Requirement to Story Mapping Table
-- ============================================

CREATE TABLE IF NOT EXISTS project.requirement_story_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_id VARCHAR(36) NOT NULL,
    story_id VARCHAR(50) NOT NULL,
    mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    mapped_by VARCHAR(36),
    notes TEXT,
    UNIQUE(requirement_id, story_id)
);

-- Indexes for efficient lookups
CREATE INDEX IF NOT EXISTS idx_rsm_requirement_id
    ON project.requirement_story_mapping(requirement_id);
CREATE INDEX IF NOT EXISTS idx_rsm_story_id
    ON project.requirement_story_mapping(story_id);

-- Foreign key constraints (optional - depends on cross-schema FK support)
-- ALTER TABLE project.requirement_story_mapping
--     ADD CONSTRAINT fk_rsm_requirement
--     FOREIGN KEY (requirement_id) REFERENCES project.requirements(id);

COMMENT ON TABLE project.requirement_story_mapping IS
'Maps Requirements to UserStories for traceability
@lineage: Requirement -> UserStory (DERIVES relationship)
@openmetadata: Creates lineage edge in OpenMetadata';

COMMENT ON COLUMN project.requirement_story_mapping.requirement_id IS
'Reference to project.requirements.id';

COMMENT ON COLUMN project.requirement_story_mapping.story_id IS
'Reference to task.user_stories.id';


-- ============================================
-- 2. Add user_story_id and sprint_id to Tasks
-- ============================================

ALTER TABLE task.tasks
ADD COLUMN IF NOT EXISTS user_story_id VARCHAR(50);

ALTER TABLE task.tasks
ADD COLUMN IF NOT EXISTS sprint_id VARCHAR(50);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tasks_user_story_id
    ON task.tasks(user_story_id);
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_id
    ON task.tasks(sprint_id);

COMMENT ON COLUMN task.tasks.user_story_id IS
'Reference to task.user_stories.id - links Task to its parent UserStory
@lineage: UserStory -> Task (BREAKS_DOWN_TO relationship)';

COMMENT ON COLUMN task.tasks.sprint_id IS
'Reference to task.sprints.id - direct sprint assignment for reporting';


-- ============================================
-- 3. Add neo4j_node_id to entities for graph sync
-- ============================================

ALTER TABLE task.user_stories
ADD COLUMN IF NOT EXISTS neo4j_node_id VARCHAR(100);

ALTER TABLE task.tasks
ADD COLUMN IF NOT EXISTS neo4j_node_id VARCHAR(100);

ALTER TABLE task.sprints
ADD COLUMN IF NOT EXISTS neo4j_node_id VARCHAR(100);

COMMENT ON COLUMN task.user_stories.neo4j_node_id IS
'Neo4j node ID for graph database synchronization';

COMMENT ON COLUMN task.tasks.neo4j_node_id IS
'Neo4j node ID for graph database synchronization';

COMMENT ON COLUMN task.sprints.neo4j_node_id IS
'Neo4j node ID for graph database synchronization';


-- ============================================
-- 4. Sprint to Requirement Mapping (optional direct mapping)
-- ============================================

CREATE TABLE IF NOT EXISTS project.sprint_requirement_coverage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id VARCHAR(50) NOT NULL,
    requirement_id VARCHAR(36) NOT NULL,
    coverage_type VARCHAR(20) DEFAULT 'PARTIAL',  -- FULL, PARTIAL, NONE
    covered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sprint_id, requirement_id)
);

COMMENT ON TABLE project.sprint_requirement_coverage IS
'Tracks which requirements are covered in each sprint
Derived from requirement_story_mapping but provides direct sprint view';


-- ============================================
-- 5. Update existing table comments for OpenMetadata
-- ============================================

COMMENT ON TABLE project.requirements IS
'RFP-extracted requirements for PMS projects
@story: Auto-linked via requirement_story_mapping
@owner: backend-team
@tier: Tier1
@openmetadata: Primary source for requirement lineage';

COMMENT ON TABLE task.user_stories IS
'Scrum User Stories - units of work in sprints
@tier: Tier1
@openmetadata: Middle layer in Requirement->Story->Task lineage';

COMMENT ON TABLE task.tasks IS
'Kanban board tasks - implementation work items
@tier: Tier1
@openmetadata: Final layer in Requirement->Story->Task lineage';

COMMENT ON TABLE task.sprints IS
'Scrum sprints - time-boxed development iterations
@tier: Tier1';

COMMENT ON TABLE task.kanban_columns IS
'Kanban board workflow columns (TODO, IN_PROGRESS, DONE, etc.)
@tier: Tier2';


-- ============================================
-- 6. Materialized View for Requirement Lineage (optional, for performance)
-- ============================================

-- DROP MATERIALIZED VIEW IF EXISTS project.mv_requirement_lineage;

-- CREATE MATERIALIZED VIEW project.mv_requirement_lineage AS
-- SELECT
--     r.id as requirement_id,
--     r.code as requirement_code,
--     r.title as requirement_title,
--     r.status as requirement_status,
--     us.id as story_id,
--     us.title as story_title,
--     us.status as story_status,
--     us.story_points,
--     s.id as sprint_id,
--     s.name as sprint_name,
--     t.id as task_id,
--     t.title as task_title,
--     t.status as task_status
-- FROM project.requirements r
-- LEFT JOIN project.requirement_story_mapping rsm ON r.id = rsm.requirement_id
-- LEFT JOIN task.user_stories us ON rsm.story_id = us.id
-- LEFT JOIN task.sprints s ON us.sprint_id = s.id
-- LEFT JOIN task.tasks t ON t.user_story_id = us.id;

-- CREATE UNIQUE INDEX ON project.mv_requirement_lineage (requirement_id, story_id, task_id);

-- COMMENT ON MATERIALIZED VIEW project.mv_requirement_lineage IS
-- 'Pre-computed requirement lineage for fast queries
-- Refresh with: REFRESH MATERIALIZED VIEW CONCURRENTLY project.mv_requirement_lineage';
