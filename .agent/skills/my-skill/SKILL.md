# PMS Insurance Claims - Key Summary (v2.1-lite)

## 1. Mission

Lifecycle management of insurance underwriting projects + GraphRAG AI decision support platform

## 2. Technology Stack Summary (as of January 2026)

- Frontend: React 18 + TypeScript + Vite
- Backend: Spring Boot 3.2 + JPA + PostgreSQL 15 (Schema: auth, project, task, chat, report, etc.)
- AI/LLM: Flask + LangGraph + Gemma-3-12B-Q5_K_M (multi-e5-large embedding)
- Graph DB: Neo4j 5.20 (vector + sequential graph RAG)
- Cache: Redis 7
- Infrastructure: Docker Compose (postgres:5433, redis:6379, neo4j:7687, be:8083, fe:5173, llm:8000)

## 3. Core Domain Entity Relationships (Brief)

User → Project → Phase → Deliverable / Issue
Project → KanbanColumn → Task / UserStory / Sprint
Project ↔ ChatSession → ChatMessage
→ Most entities inherit from BaseEntity (created_at, updated_at)

## 4. AI Chatbot Operation Principles (Core Flow)

1. Intent Classification
2. Casual → Immediate Response
3. Complex → RAG Search → Quality Verification → If low, improve query and re-search → Generate final response

## 5. Essential Development Rules

- TDD (pytest preferred)
- Small unit commits + PRs are essential
- Actively utilize Git Worktree
- Korean comments are prohibited → Translate to English
- API calls are limited to the /api folder

## 6. Security & Authorization Core

- JWT (24 hours) + RBAC
- Key Roles: SPONSOR, PMO_HEAD PM, DEVELOPER, QA, BUSINESS_ANALYST, ADMIN

## 7. Most Frequently Referenced Settings

- AI_SERVICE_URL=http://llm-service:8000
- MODEL_PATH=./models/google.gemma-3-12b-pt.Q5_K_M.gguf
- JWT_SECRET=Required environment variable
- NEO4J_URI=bolt://neo4j:7687

## 8. Quick Start Command

docker-compose up -d