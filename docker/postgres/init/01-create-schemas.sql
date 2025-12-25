-- PostgreSQL 초기화 스크립트
-- 각 모듈별 독립 스키마 생성 (MSA 전환 대비)

-- Auth 스키마
CREATE SCHEMA IF NOT EXISTS auth;

-- Project 스키마
CREATE SCHEMA IF NOT EXISTS project;

-- Task 스키마
CREATE SCHEMA IF NOT EXISTS task;

-- Chat 스키마
CREATE SCHEMA IF NOT EXISTS chat;

-- Risk 스키마
CREATE SCHEMA IF NOT EXISTS risk;

-- Report 스키마
CREATE SCHEMA IF NOT EXISTS report;

-- 기본 권한 설정
GRANT ALL PRIVILEGES ON SCHEMA auth TO pms_user;
GRANT ALL PRIVILEGES ON SCHEMA project TO pms_user;
GRANT ALL PRIVILEGES ON SCHEMA task TO pms_user;
GRANT ALL PRIVILEGES ON SCHEMA chat TO pms_user;
GRANT ALL PRIVILEGES ON SCHEMA risk TO pms_user;
GRANT ALL PRIVILEGES ON SCHEMA report TO pms_user;

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- 텍스트 검색 최적화

COMMIT;
