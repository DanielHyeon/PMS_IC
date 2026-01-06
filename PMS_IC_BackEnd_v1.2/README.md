# PMS Insurance Claims Backend API v1.2

백엔드 API 서버 for PMS (Project Management System) for Insurance Claims

## Features

- RESTful API endpoints
- JWT-based authentication
- Mock data for development
- TypeScript support
- CORS enabled

## Installation

```bash
npm install
```

## Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Edit `.env`:
```
PORT=3001
NODE_ENV=development
JWT_SECRET=your-secret-key-here
CORS_ORIGIN=http://localhost:5173
```

## Development

```bash
npm run dev
```

## Build

```bash
npm run build
```

## Production

```bash
npm start
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/logout` - Logout

### Dashboard
- `GET /api/dashboard/stats` - Dashboard statistics
- `GET /api/dashboard/activities` - Recent activities
- `GET /api/dashboard/charts/phase` - Phase progress chart data
- `GET /api/dashboard/charts/velocity` - Sprint velocity chart data
- `GET /api/dashboard/charts/burndown` - Burndown chart data

### Phases
- `GET /api/phases` - Get all phases
- `GET /api/phases/:id` - Get phase by ID
- `PUT /api/phases/:id` - Update phase
- `PUT /api/phases/:phaseId/deliverables/:deliverableId` - Update deliverable

### Tasks (Kanban)
- `GET /api/tasks/columns` - Get all columns with tasks
- `GET /api/tasks` - Get all tasks
- `POST /api/tasks` - Create new task
- `PUT /api/tasks/:id` - Update task
- `PUT /api/tasks/:id/move` - Move task to another column
- `DELETE /api/tasks/:id` - Delete task

### Stories (Backlog)
- `GET /api/stories` - Get all user stories (supports ?status=backlog&epic=OCR)
- `GET /api/stories/epics` - Get all epic names
- `GET /api/stories/:id` - Get story by ID
- `POST /api/stories` - Create new story
- `PUT /api/stories/:id` - Update story
- `PUT /api/stories/:id/priority` - Change story priority (up/down)
- `DELETE /api/stories/:id` - Delete story

### Users
- `GET /api/users` - Get all users
- `GET /api/users/:id` - Get user by ID

## Default Test Users

All users have password: `password123`

| Email | Role | Name |
|-------|------|------|
| kim@example.com | pm | 김철수 |
| lee@example.com | developer | 이영희 |
| park@example.com | developer | 박민수 |
| choi@example.com | qa | 최지훈 |
| jung@example.com | pmo_head | 정수연 |
| kang@example.com | sponsor | 강민재 |

## Authentication

All endpoints except `/api/auth/login` require JWT token:

```
Authorization: Bearer <token>
```

Example login:
```bash
curl -X POST http://localhost:3001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"kim@example.com","password":"password123"}'
```

## Tech Stack

- Node.js
- Express
- TypeScript
- JWT
- bcrypt
- Helmet (security)
- Morgan (logging)
- CORS
---------------------------------------------------------
# PMS Backend v1.2

Spring Boot 기반 프로젝트 관리 시스템 백엔드

## 기술 스택

- **Framework:** Spring Boot 3.2.1
- **Language:** Java 17
- **Database:** PostgreSQL 15 (prod), H2 (dev)
- **Cache:** Redis 7
- **Security:** Spring Security + JWT
- **API Docs:** Swagger/OpenAPI 3

## 실행 방법

### 개발 환경 (Docker Compose)

```bash
# 전체 환경 실행
docker-compose up -d

# 백엔드만 재시작
docker-compose restart backend

# 로그 확인
docker-compose logs -f backend

----------------------
