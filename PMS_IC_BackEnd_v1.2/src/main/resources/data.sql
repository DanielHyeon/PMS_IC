-- Mock Data for PMS Insurance Claims
-- Version: 20260117
-- Description: Sample data with 2 projects for testing

-- ============================================
-- 1. USERS (auth.users)
-- ============================================
INSERT INTO auth.users (id, email, password, name, role, department, active, created_at, updated_at)
VALUES
    -- Admin
    ('user-admin-001', 'admin@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'System Admin', 'ADMIN', 'IT', true, NOW(), NOW()),
    -- PMO Head
    ('user-pmo-001', 'pmo.head@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'James Wilson', 'PMO_HEAD', 'PMO', true, NOW(), NOW()),
    -- Project Managers
    ('user-pm-001', 'pm.kim@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Kim Minsu', 'PM', 'Project Management', true, NOW(), NOW()),
    ('user-pm-002', 'pm.lee@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Lee Jihye', 'PM', 'Project Management', true, NOW(), NOW()),
    -- Developers
    ('user-dev-001', 'dev.park@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Park Sungho', 'DEVELOPER', 'Development', true, NOW(), NOW()),
    ('user-dev-002', 'dev.choi@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Choi Eunji', 'DEVELOPER', 'Development', true, NOW(), NOW()),
    ('user-dev-003', 'dev.jung@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Jung Wonjae', 'DEVELOPER', 'Development', true, NOW(), NOW()),
    -- QA
    ('user-qa-001', 'qa.han@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Han Soojin', 'QA', 'Quality Assurance', true, NOW(), NOW()),
    -- Business Analyst
    ('user-ba-001', 'ba.yoon@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Yoon Hyemi', 'BUSINESS_ANALYST', 'Business Analysis', true, NOW(), NOW()),
    -- Sponsor
    ('user-sponsor-001', 'sponsor.kang@insuretech.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsRdQe5V0u0EJmGPTu', 'Kang Daehyun', 'SPONSOR', 'Executive', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 2. PROJECTS (project.projects)
-- ============================================
INSERT INTO project.projects (id, name, description, status, start_date, end_date, budget, progress, created_at, updated_at, created_by)
VALUES
    -- Project 1: AI Claims Processing System
    ('proj-001', 'AI Claims Processing System',
     'Develop an AI-powered insurance claims processing system with automated document analysis, fraud detection, and intelligent routing capabilities. This project aims to reduce claim processing time by 60% and improve accuracy by 40%.',
     'IN_PROGRESS', '2026-01-15', '2026-06-30', 500000000.00, 25, NOW(), NOW(), 'user-pm-001'),

    -- Project 2: Mobile Insurance Platform
    ('proj-002', 'Mobile Insurance Platform',
     'Build a comprehensive mobile platform for insurance services including policy management, claims submission, real-time status tracking, and customer support integration. Target: 100,000 active users within first year.',
     'PLANNING', '2026-02-01', '2026-08-31', 350000000.00, 5, NOW(), NOW(), 'user-pm-002')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 3. PHASES (project.phases)
-- ============================================
-- Project 1 Phases
INSERT INTO project.phases (id, project_id, name, order_num, status, gate_status, start_date, end_date, progress, description, track_type, created_at, updated_at)
VALUES
    ('phase-001-01', 'proj-001', 'Requirements Analysis', 1, 'COMPLETED', 'APPROVED', '2026-01-15', '2026-01-31', 100, 'Gather and analyze all requirements for AI claims processing', 'COMMON', NOW(), NOW()),
    ('phase-001-02', 'proj-001', 'System Design', 2, 'IN_PROGRESS', 'PENDING', '2026-02-01', '2026-02-28', 60, 'Design system architecture and data models', 'COMMON', NOW(), NOW()),
    ('phase-001-03', 'proj-001', 'AI Model Development', 3, 'NOT_STARTED', NULL, '2026-03-01', '2026-04-15', 0, 'Develop and train AI models for document processing and fraud detection', 'AI', NOW(), NOW()),
    ('phase-001-04', 'proj-001', 'Backend Development', 4, 'NOT_STARTED', NULL, '2026-03-15', '2026-05-15', 0, 'Implement backend services and APIs', 'SI', NOW(), NOW()),
    ('phase-001-05', 'proj-001', 'Integration & Testing', 5, 'NOT_STARTED', NULL, '2026-05-01', '2026-06-15', 0, 'System integration and comprehensive testing', 'COMMON', NOW(), NOW()),
    ('phase-001-06', 'proj-001', 'Deployment & Go-Live', 6, 'NOT_STARTED', NULL, '2026-06-15', '2026-06-30', 0, 'Production deployment and user training', 'COMMON', NOW(), NOW()),

    -- Project 2 Phases
    ('phase-002-01', 'proj-002', 'Market Research & Planning', 1, 'IN_PROGRESS', 'PENDING', '2026-02-01', '2026-02-28', 30, 'Research market needs and create detailed project plan', 'COMMON', NOW(), NOW()),
    ('phase-002-02', 'proj-002', 'UX/UI Design', 2, 'NOT_STARTED', NULL, '2026-03-01', '2026-03-31', 0, 'Design mobile app user experience and interface', 'COMMON', NOW(), NOW()),
    ('phase-002-03', 'proj-002', 'Mobile App Development', 3, 'NOT_STARTED', NULL, '2026-04-01', '2026-06-30', 0, 'Develop iOS and Android applications', 'SI', NOW(), NOW()),
    ('phase-002-04', 'proj-002', 'Backend API Development', 4, 'NOT_STARTED', NULL, '2026-04-01', '2026-06-15', 0, 'Build backend APIs for mobile platform', 'SI', NOW(), NOW()),
    ('phase-002-05', 'proj-002', 'Testing & QA', 5, 'NOT_STARTED', NULL, '2026-06-15', '2026-08-15', 0, 'Quality assurance and user acceptance testing', 'COMMON', NOW(), NOW()),
    ('phase-002-06', 'proj-002', 'Launch & Marketing', 6, 'NOT_STARTED', NULL, '2026-08-15', '2026-08-31', 0, 'App store submission and marketing campaign', 'COMMON', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 4. KANBAN COLUMNS (task.kanban_columns)
-- ============================================
-- Project 1 Kanban Columns
INSERT INTO task.kanban_columns (id, project_id, name, order_num, wip_limit, color, created_at, updated_at)
VALUES
    ('col-001-01', 'proj-001', 'Backlog', 1, NULL, '#6B7280', NOW(), NOW()),
    ('col-001-02', 'proj-001', 'To Do', 2, 10, '#3B82F6', NOW(), NOW()),
    ('col-001-03', 'proj-001', 'In Progress', 3, 5, '#F59E0B', NOW(), NOW()),
    ('col-001-04', 'proj-001', 'Review', 4, 3, '#8B5CF6', NOW(), NOW()),
    ('col-001-05', 'proj-001', 'Done', 5, NULL, '#10B981', NOW(), NOW()),

    -- Project 2 Kanban Columns
    ('col-002-01', 'proj-002', 'Backlog', 1, NULL, '#6B7280', NOW(), NOW()),
    ('col-002-02', 'proj-002', 'To Do', 2, 8, '#3B82F6', NOW(), NOW()),
    ('col-002-03', 'proj-002', 'In Progress', 3, 4, '#F59E0B', NOW(), NOW()),
    ('col-002-04', 'proj-002', 'Review', 4, 3, '#8B5CF6', NOW(), NOW()),
    ('col-002-05', 'proj-002', 'Done', 5, NULL, '#10B981', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 5. TASKS (task.tasks)
-- ============================================
-- Project 1 Tasks
INSERT INTO task.tasks (id, column_id, phase_id, title, description, assignee_id, priority, status, due_date, order_num, tags, track_type, created_at, updated_at)
VALUES
    -- Completed tasks (in Done column)
    ('task-001-01', 'col-001-05', 'phase-001-01', 'Document RFP requirements', 'Analyze and document all requirements from the RFP document', 'user-ba-001', 'HIGH', 'DONE', '2026-01-20', 1, 'requirements,documentation', 'COMMON', NOW(), NOW()),
    ('task-001-02', 'col-001-05', 'phase-001-01', 'Stakeholder interviews', 'Conduct interviews with key stakeholders to gather requirements', 'user-ba-001', 'HIGH', 'DONE', '2026-01-25', 2, 'requirements,stakeholder', 'COMMON', NOW(), NOW()),
    ('task-001-03', 'col-001-05', 'phase-001-01', 'Requirements sign-off', 'Get formal approval on documented requirements', 'user-pm-001', 'CRITICAL', 'DONE', '2026-01-31', 3, 'requirements,approval', 'COMMON', NOW(), NOW()),

    -- In Progress tasks
    ('task-001-04', 'col-001-03', 'phase-001-02', 'Design system architecture', 'Create high-level system architecture diagram', 'user-dev-001', 'CRITICAL', 'IN_PROGRESS', '2026-02-15', 1, 'architecture,design', 'SI', NOW(), NOW()),
    ('task-001-05', 'col-001-03', 'phase-001-02', 'Design data models', 'Design database schema and entity relationships', 'user-dev-002', 'HIGH', 'IN_PROGRESS', '2026-02-20', 2, 'database,design', 'SI', NOW(), NOW()),

    -- Review tasks
    ('task-001-06', 'col-001-04', 'phase-001-02', 'API specification draft', 'Create OpenAPI specification for all endpoints', 'user-dev-001', 'HIGH', 'REVIEW', '2026-02-18', 1, 'api,documentation', 'SI', NOW(), NOW()),

    -- To Do tasks
    ('task-001-07', 'col-001-02', 'phase-001-02', 'Security architecture review', 'Review and document security measures', 'user-dev-003', 'HIGH', 'TODO', '2026-02-25', 1, 'security,architecture', 'COMMON', NOW(), NOW()),
    ('task-001-08', 'col-001-02', 'phase-001-03', 'Set up ML development environment', 'Configure development environment for AI model training', 'user-dev-002', 'MEDIUM', 'TODO', '2026-03-05', 2, 'ai,setup', 'AI', NOW(), NOW()),

    -- Backlog tasks
    ('task-001-09', 'col-001-01', 'phase-001-03', 'Train document classification model', 'Develop and train AI model for document type classification', 'user-dev-002', 'HIGH', 'TODO', '2026-03-20', 1, 'ai,ml,training', 'AI', NOW(), NOW()),
    ('task-001-10', 'col-001-01', 'phase-001-03', 'Implement fraud detection algorithm', 'Develop fraud detection model using historical data', 'user-dev-002', 'CRITICAL', 'TODO', '2026-04-01', 2, 'ai,fraud,ml', 'AI', NOW(), NOW()),
    ('task-001-11', 'col-001-01', 'phase-001-04', 'Implement claims API', 'Build REST API for claims management', 'user-dev-001', 'HIGH', 'TODO', '2026-04-15', 3, 'api,backend', 'SI', NOW(), NOW()),
    ('task-001-12', 'col-001-01', 'phase-001-04', 'Implement document upload service', 'Build service for document upload and processing', 'user-dev-003', 'MEDIUM', 'TODO', '2026-04-20', 4, 'backend,files', 'SI', NOW(), NOW()),

    -- Project 2 Tasks
    -- In Progress
    ('task-002-01', 'col-002-03', 'phase-002-01', 'Competitor analysis', 'Analyze competitor mobile insurance apps', 'user-ba-001', 'HIGH', 'IN_PROGRESS', '2026-02-15', 1, 'research,competitor', 'COMMON', NOW(), NOW()),
    ('task-002-02', 'col-002-03', 'phase-002-01', 'User persona development', 'Create target user personas based on research', 'user-ba-001', 'MEDIUM', 'IN_PROGRESS', '2026-02-20', 2, 'research,ux', 'COMMON', NOW(), NOW()),

    -- To Do
    ('task-002-03', 'col-002-02', 'phase-002-01', 'Feature prioritization', 'Prioritize features for MVP release', 'user-pm-002', 'HIGH', 'TODO', '2026-02-25', 1, 'planning,mvp', 'COMMON', NOW(), NOW()),
    ('task-002-04', 'col-002-02', 'phase-002-02', 'Wireframe design', 'Create wireframes for main app screens', NULL, 'HIGH', 'TODO', '2026-03-10', 2, 'ux,design', 'COMMON', NOW(), NOW()),

    -- Backlog
    ('task-002-05', 'col-002-01', 'phase-002-02', 'Visual design system', 'Create comprehensive design system and style guide', NULL, 'MEDIUM', 'TODO', '2026-03-20', 1, 'design,ui', 'COMMON', NOW(), NOW()),
    ('task-002-06', 'col-002-01', 'phase-002-03', 'iOS app setup', 'Set up iOS project with Swift', 'user-dev-001', 'MEDIUM', 'TODO', '2026-04-05', 2, 'ios,mobile', 'SI', NOW(), NOW()),
    ('task-002-07', 'col-002-01', 'phase-002-03', 'Android app setup', 'Set up Android project with Kotlin', 'user-dev-003', 'MEDIUM', 'TODO', '2026-04-05', 3, 'android,mobile', 'SI', NOW(), NOW()),
    ('task-002-08', 'col-002-01', 'phase-002-04', 'Authentication API', 'Implement user authentication endpoints', 'user-dev-001', 'CRITICAL', 'TODO', '2026-04-15', 4, 'api,auth,backend', 'SI', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 6. RFPS (project.rfps) - Sample RFP Documents
-- ============================================
INSERT INTO project.rfps (id, project_id, title, content, status, processing_status, tenant_id, created_at, updated_at, created_by)
VALUES
    ('rfp-001', 'proj-001', 'AI Claims Processing System RFP',
     'Request for Proposal for developing an AI-powered insurance claims processing system. Key requirements include: automated document analysis, fraud detection capabilities, integration with existing systems, and compliance with insurance regulations.',
     'APPROVED', 'COMPLETED', 'tenant-001', NOW(), NOW(), 'user-sponsor-001'),
    ('rfp-002', 'proj-002', 'Mobile Insurance Platform RFP',
     'Request for Proposal for building a comprehensive mobile platform for insurance services. Must include: policy management, claims submission, real-time notifications, secure authentication, and offline capabilities.',
     'SUBMITTED', 'PENDING', 'tenant-001', NOW(), NOW(), 'user-sponsor-001')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 7. REQUIREMENTS (project.requirements)
-- ============================================
-- Project 1 Requirements
INSERT INTO project.requirements (id, rfp_id, project_id, requirement_code, title, description, category, priority, status, progress, tenant_id, created_at, updated_at)
VALUES
    ('req-001-01', 'rfp-001', 'proj-001', 'REQ-AI-001', 'Document OCR Processing', 'System must be able to extract text from scanned insurance documents with 99% accuracy', 'AI', 'CRITICAL', 'APPROVED', 60, 'tenant-001', NOW(), NOW()),
    ('req-001-02', 'rfp-001', 'proj-001', 'REQ-AI-002', 'Fraud Detection Algorithm', 'Implement ML-based fraud detection with configurable sensitivity thresholds', 'AI', 'CRITICAL', 'ANALYZED', 30, 'tenant-001', NOW(), NOW()),
    ('req-001-03', 'rfp-001', 'proj-001', 'REQ-SI-001', 'Claims Management API', 'RESTful API for full claims lifecycle management', 'FUNCTIONAL', 'HIGH', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-001-04', 'rfp-001', 'proj-001', 'REQ-SI-002', 'Legacy System Integration', 'Integration with existing policy management system via ESB', 'INTEGRATION', 'HIGH', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-001-05', 'rfp-001', 'proj-001', 'REQ-SEC-001', 'Data Encryption', 'All PII must be encrypted at rest and in transit using AES-256', 'SECURITY', 'CRITICAL', 'APPROVED', 0, 'tenant-001', NOW(), NOW()),
    ('req-001-06', 'rfp-001', 'proj-001', 'REQ-NF-001', 'Performance Requirements', 'System must handle 1000 concurrent users with <2s response time', 'NON_FUNCTIONAL', 'HIGH', 'ANALYZED', 0, 'tenant-001', NOW(), NOW()),

    -- Project 2 Requirements
    ('req-002-01', 'rfp-002', 'proj-002', 'REQ-MOB-001', 'User Authentication', 'Biometric and password-based authentication for mobile app', 'SECURITY', 'CRITICAL', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-002-02', 'rfp-002', 'proj-002', 'REQ-MOB-002', 'Policy Dashboard', 'Display all user policies with key information on dashboard', 'FUNCTIONAL', 'HIGH', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-002-03', 'rfp-002', 'proj-002', 'REQ-MOB-003', 'Claims Submission', 'Allow users to submit claims with photo uploads from mobile', 'FUNCTIONAL', 'CRITICAL', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-002-04', 'rfp-002', 'proj-002', 'REQ-MOB-004', 'Push Notifications', 'Real-time notifications for claim status updates', 'FUNCTIONAL', 'MEDIUM', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW()),
    ('req-002-05', 'rfp-002', 'proj-002', 'REQ-MOB-005', 'Offline Mode', 'App must work offline with data sync when connected', 'NON_FUNCTIONAL', 'MEDIUM', 'IDENTIFIED', 0, 'tenant-001', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 8. PARTS (project.parts)
-- ============================================
INSERT INTO project.parts (id, project_id, name, description, leader_id, leader_name, status, start_date, end_date, progress, created_at, updated_at)
VALUES
    -- Project 1 Parts
    ('part-001-01', 'proj-001', 'AI Development', 'AI/ML model development team for claims processing', 'user-dev-002', 'Choi Eunji', 'ACTIVE', '2026-01-15', '2026-06-30', 30, NOW(), NOW()),
    ('part-001-02', 'proj-001', 'Backend Development', 'Backend API and service development', 'user-dev-001', 'Park Sungho', 'ACTIVE', '2026-01-15', '2026-06-30', 25, NOW(), NOW()),
    ('part-001-03', 'proj-001', 'QA & Testing', 'Quality assurance and testing team', 'user-qa-001', 'Han Soojin', 'ACTIVE', '2026-01-15', '2026-06-30', 20, NOW(), NOW()),

    -- Project 2 Parts
    ('part-002-01', 'proj-002', 'Mobile Development', 'iOS and Android app development', 'user-dev-001', 'Park Sungho', 'ACTIVE', '2026-02-01', '2026-08-31', 10, NOW(), NOW()),
    ('part-002-02', 'proj-002', 'UX/UI Design', 'User experience and interface design', NULL, NULL, 'ACTIVE', '2026-02-01', '2026-08-31', 5, NOW(), NOW()),
    ('part-002-03', 'proj-002', 'Backend API', 'Mobile backend API development', 'user-dev-003', 'Jung Wonjae', 'ACTIVE', '2026-02-01', '2026-08-31', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- 9. PART MEMBERS (project.part_members)
-- ============================================
INSERT INTO project.part_members (part_id, user_id)
VALUES
    -- AI Development Part members
    ('part-001-01', 'user-dev-002'),
    ('part-001-01', 'user-ba-001'),

    -- Backend Development Part members
    ('part-001-02', 'user-dev-001'),
    ('part-001-02', 'user-dev-003'),

    -- QA Part members
    ('part-001-03', 'user-qa-001'),

    -- Mobile Development Part members
    ('part-002-01', 'user-dev-001'),
    ('part-002-01', 'user-dev-003'),

    -- UX/UI Design Part members (no leader yet)
    ('part-002-02', 'user-ba-001'),

    -- Backend API Part members
    ('part-002-03', 'user-dev-003')
ON CONFLICT DO NOTHING;

-- ============================================
-- 10. PROJECT MEMBERS (project.project_members)
-- ============================================
INSERT INTO project.project_members (id, project_id, user_id, user_name, user_email, role, department, created_at, updated_at)
VALUES
    -- Project 1 Members
    ('pm-001-01', 'proj-001', 'user-sponsor-001', 'Kang Daehyun', 'sponsor.kang@insuretech.com', 'SPONSOR', 'Executive', NOW(), NOW()),
    ('pm-001-02', 'proj-001', 'user-pm-001', 'Kim Minsu', 'pm.kim@insuretech.com', 'PM', 'Project Management', NOW(), NOW()),
    ('pm-001-03', 'proj-001', 'user-pmo-001', 'James Wilson', 'pmo.head@insuretech.com', 'PMO_HEAD', 'PMO', NOW(), NOW()),
    ('pm-001-04', 'proj-001', 'user-dev-001', 'Park Sungho', 'dev.park@insuretech.com', 'DEVELOPER', 'Development', NOW(), NOW()),
    ('pm-001-05', 'proj-001', 'user-dev-002', 'Choi Eunji', 'dev.choi@insuretech.com', 'DEVELOPER', 'Development', NOW(), NOW()),
    ('pm-001-06', 'proj-001', 'user-dev-003', 'Jung Wonjae', 'dev.jung@insuretech.com', 'DEVELOPER', 'Development', NOW(), NOW()),
    ('pm-001-07', 'proj-001', 'user-qa-001', 'Han Soojin', 'qa.han@insuretech.com', 'QA', 'Quality Assurance', NOW(), NOW()),
    ('pm-001-08', 'proj-001', 'user-ba-001', 'Yoon Hyemi', 'ba.yoon@insuretech.com', 'BUSINESS_ANALYST', 'Business Analysis', NOW(), NOW()),

    -- Project 2 Members
    ('pm-002-01', 'proj-002', 'user-sponsor-001', 'Kang Daehyun', 'sponsor.kang@insuretech.com', 'SPONSOR', 'Executive', NOW(), NOW()),
    ('pm-002-02', 'proj-002', 'user-pm-002', 'Lee Jihye', 'pm.lee@insuretech.com', 'PM', 'Project Management', NOW(), NOW()),
    ('pm-002-03', 'proj-002', 'user-dev-001', 'Park Sungho', 'dev.park@insuretech.com', 'DEVELOPER', 'Development', NOW(), NOW()),
    ('pm-002-04', 'proj-002', 'user-dev-003', 'Jung Wonjae', 'dev.jung@insuretech.com', 'DEVELOPER', 'Development', NOW(), NOW()),
    ('pm-002-05', 'proj-002', 'user-ba-001', 'Yoon Hyemi', 'ba.yoon@insuretech.com', 'BUSINESS_ANALYST', 'Business Analysis', NOW(), NOW()),
    ('pm-002-06', 'proj-002', 'user-admin-001', 'System Admin', 'admin@insuretech.com', 'AUDITOR', 'IT', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- Summary:
-- - 10 Users across different roles
-- - 2 Projects (AI Claims Processing, Mobile Platform)
-- - 12 Phases (6 per project)
-- - 10 Kanban Columns (5 per project)
-- - 20 Tasks distributed across columns and phases
-- - 2 RFPs
-- - 11 Requirements
-- - 6 Parts (3 per project)
-- - 14 Project Members with various roles
-- ============================================
