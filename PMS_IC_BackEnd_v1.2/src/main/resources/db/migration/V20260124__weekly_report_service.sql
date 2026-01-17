-- Weekly Report Service Migration
-- Version: 20260124
-- Description: Create tables for weekly report generation with LLM integration

-- ============================================
-- 1. Weekly Reports Table
-- ============================================

CREATE TABLE IF NOT EXISTS task.weekly_reports (
    id VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL,
    sprint_id VARCHAR(50),
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    generated_by VARCHAR(50),
    generated_at DATE NOT NULL,

    -- Metrics
    total_tasks INTEGER,
    completed_tasks INTEGER,
    in_progress_tasks INTEGER,
    todo_tasks INTEGER,
    blocked_tasks INTEGER,
    completion_rate DECIMAL(5, 2),
    velocity DECIMAL(10, 2),

    -- Progress
    story_points_completed INTEGER,
    story_points_in_progress INTEGER,
    story_points_planned INTEGER,

    -- WIP Analysis
    average_wip_count INTEGER,
    peak_wip_count INTEGER,
    flow_efficiency DECIMAL(5, 2),
    bottlenecks TEXT,

    -- Trend Analysis
    velocity_trend DECIMAL(10, 2),
    completion_trend DECIMAL(5, 2),
    recommendations TEXT,

    -- Summary
    summary TEXT,
    generated_content TEXT,

    -- LLM Integration
    llm_model VARCHAR(100),
    llm_confidence_score DECIMAL(3, 2),

    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    CONSTRAINT fk_weekly_report_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_weekly_report_sprint FOREIGN KEY (sprint_id)
        REFERENCES task.sprints(id) ON DELETE SET NULL
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_weekly_reports_project_id ON task.weekly_reports(project_id);
CREATE INDEX IF NOT EXISTS idx_weekly_reports_sprint_id ON task.weekly_reports(sprint_id);
CREATE INDEX IF NOT EXISTS idx_weekly_reports_week_start_date ON task.weekly_reports(week_start_date);
CREATE INDEX IF NOT EXISTS idx_weekly_reports_project_week ON task.weekly_reports(project_id, week_start_date);
CREATE INDEX IF NOT EXISTS idx_weekly_reports_sprint_week ON task.weekly_reports(sprint_id, week_start_date);

COMMENT ON TABLE task.weekly_reports IS 'Weekly reports for projects and sprints with LLM-generated insights';
COMMENT ON COLUMN task.weekly_reports.completion_rate IS 'Weekly completion rate as percentage (0-100)';
COMMENT ON COLUMN task.weekly_reports.velocity IS 'Number of tasks completed in the week';
COMMENT ON COLUMN task.weekly_reports.flow_efficiency IS 'Flow efficiency percentage based on WIP and completion';
COMMENT ON COLUMN task.weekly_reports.llm_model IS 'Name of LLM model used for generating insights';
COMMENT ON COLUMN task.weekly_reports.llm_confidence_score IS 'Confidence score of LLM-generated content (0-1)';


-- ============================================
-- 2. Weekly Report Trends Table (Optional - for historical analysis)
-- ============================================

CREATE TABLE IF NOT EXISTS task.weekly_report_trends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(10, 2),
    week_end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trend_project FOREIGN KEY (project_id)
        REFERENCES project.projects(id) ON DELETE CASCADE,
    CONSTRAINT uk_trend_metric_date UNIQUE (project_id, metric_name, week_end_date)
);

CREATE INDEX IF NOT EXISTS idx_weekly_report_trends_project_id
    ON task.weekly_report_trends(project_id);
CREATE INDEX IF NOT EXISTS idx_weekly_report_trends_metric
    ON task.weekly_report_trends(metric_name, week_end_date);

COMMENT ON TABLE task.weekly_report_trends IS 'Historical trend data extracted from weekly reports for trend analysis';
COMMENT ON COLUMN task.weekly_report_trends.metric_name IS 'Name of the metric (e.g., velocity, completion_rate, flow_efficiency)';
