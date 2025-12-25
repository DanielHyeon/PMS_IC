# PMS 개선 계획서 (Project Management System Improvement Plan)

## 📋 범위 정의

### 우리 팀의 범위 (PMS)
- ✅ 프로젝트 관리 (단계별, 스프린트, 태스크)
- ✅ 팀 협업 및 커뮤니케이션
- ✅ 일정 관리 및 진척 추적
- ✅ 리스크/이슈 관리
- ✅ 산출물/문서 관리
- ✅ 승인 워크플로우
- ✅ 리포팅 및 대시보드
- ✅ 감사 로그 및 이력 관리

### 다른 팀의 범위 (제외)
- ❌ 포탈 화면 구현 → 별도 팀
- ❌ MLOps 파이프라인 → 별도 팀
- ❌ AI 모델 개발/배포 → 별도 팀
- ❌ 데이터 사이언스 → 별도 팀

---

## 🎯 Phase 1: 데이터베이스 구현 (Core Infrastructure)

### 1.1 PostgreSQL 데이터베이스 설계

#### 핵심 테이블 구조

```sql
-- 사용자 및 권한
users (id, email, name, role, department, created_at)
user_sessions (id, user_id, token, expires_at)

-- 프로젝트 구조
projects (id, name, description, status, start_date, end_date)
phases (id, project_id, name, order, status, gate_status, start_date, end_date)
phase_gates (id, phase_id, required_approvals, approved_by, approved_at)

-- 태스크 관리
kanban_columns (id, project_id, name, order, wip_limit)
tasks (id, column_id, phase_id, title, description, assignee_id, priority, status, due_date)
subtasks (id, task_id, title, completed, order)

-- 백로그 관리
user_stories (id, project_id, sprint_id, title, description, priority, story_points, status)
sprints (id, project_id, name, goal, start_date, end_date, status)

-- 산출물 관리
deliverables (id, phase_id, name, type, status, file_path, uploaded_by, uploaded_at)

-- 리스크/이슈 관리
risks (id, project_id, title, description, probability, impact, mitigation, owner_id, status)
issues (id, project_id, title, description, severity, reported_by, assigned_to, status, resolved_at)

-- 활동 로그
activity_logs (id, user_id, action, entity_type, entity_id, details, created_at)

-- 댓글/커뮤니케이션
comments (id, entity_type, entity_id, user_id, content, created_at)
```

#### 구현 파일
- `src/db/schema.sql` - 데이터베이스 스키마
- `src/db/connection.ts` - PostgreSQL 연결 풀 관리
- `src/db/migrations/` - 마이그레이션 스크립트

---

## 🎯 Phase 2: 백엔드 API 확장

### 2.1 프로젝트 관리 API

```typescript
// 프로젝트 라이프사이클
POST   /api/projects              // 프로젝트 생성
GET    /api/projects              // 프로젝트 목록
GET    /api/projects/:id          // 프로젝트 상세
PUT    /api/projects/:id          // 프로젝트 수정
DELETE /api/projects/:id          // 프로젝트 삭제
```

### 2.2 단계별 관리 API (Phase Gate Management)

```typescript
// 단계 관리
GET    /api/phases/:id/deliverables    // 단계별 산출물 조회
POST   /api/phases/:id/deliverables    // 산출물 업로드
PUT    /api/phases/:id/status          // 단계 상태 변경

// Gate 승인
POST   /api/phases/:id/gate/submit     // Gate 승인 요청
POST   /api/phases/:id/gate/approve    // Gate 승인
POST   /api/phases/:id/gate/reject     // Gate 반려
GET    /api/phases/:id/gate/history    // 승인 이력
```

### 2.3 스프린트 관리 API

```typescript
POST   /api/sprints                    // 스프린트 생성
GET    /api/sprints/:id                // 스프린트 상세
PUT    /api/sprints/:id/start          // 스프린트 시작
PUT    /api/sprints/:id/complete       // 스프린트 완료
GET    /api/sprints/:id/burndown       // 번다운 차트 데이터
GET    /api/sprints/:id/velocity       // 속도 차트
POST   /api/sprints/:id/retrospective  // 회고 저장
```

### 2.4 리스크/이슈 관리 API

```typescript
// 리스크
POST   /api/risks                      // 리스크 등록
GET    /api/risks                      // 리스크 목록
PUT    /api/risks/:id                  // 리스크 수정
GET    /api/risks/matrix               // 리스크 매트릭스 (확률×영향도)

// 이슈
POST   /api/issues                     // 이슈 등록
GET    /api/issues                     // 이슈 목록
PUT    /api/issues/:id/assign          // 이슈 할당
PUT    /api/issues/:id/resolve         // 이슈 해결
GET    /api/issues/stats               // 이슈 통계
```

### 2.5 산출물 관리 API

```typescript
POST   /api/deliverables               // 산출물 업로드
GET    /api/deliverables               // 산출물 목록
GET    /api/deliverables/:id/download  // 다운로드
PUT    /api/deliverables/:id/version   // 버전 관리
GET    /api/deliverables/:id/history   // 변경 이력
```

### 2.6 리포팅 API

```typescript
GET    /api/reports/project-status     // 프로젝트 현황 보고서
GET    /api/reports/phase-progress     // 단계별 진행률
GET    /api/reports/resource-allocation // 자원 배분 현황
GET    /api/reports/timeline           // 전체 타임라인
GET    /api/reports/export/pdf         // PDF 보고서 생성
GET    /api/reports/export/excel       // Excel 보고서
```

---

## 🎯 Phase 3: 프론트엔드 개선

### 3.1 리스크/이슈 관리 화면 추가

```typescript
// 새 컴포넌트
src/app/components/RiskManagement.tsx
src/app/components/IssueTracking.tsx
src/app/components/RiskMatrix.tsx  // 시각화
```

**기능:**
- 리스크 등록/수정/삭제
- 리스크 매트릭스 시각화 (확률 × 영향도)
- 이슈 트래킹 및 할당
- 이슈 해결 워크플로우

### 3.2 산출물 관리 화면

```typescript
src/app/components/DeliverableManagement.tsx
src/app/components/DeliverableViewer.tsx
```

**기능:**
- 단계별 산출물 업로드
- 파일 버전 관리
- 산출물 다운로드
- 승인 상태 표시

### 3.3 승인 워크플로우 화면

```typescript
src/app/components/ApprovalWorkflow.tsx
src/app/components/PhaseGateApproval.tsx
```

**기능:**
- Gate 승인 요청
- 승인/반려 처리
- 승인 이력 조회
- 이메일 알림 (선택사항)

### 3.4 고급 리포팅

```typescript
src/app/components/ProjectReports.tsx
src/app/components/charts/BurndownChart.tsx
src/app/components/charts/VelocityChart.tsx
src/app/components/charts/GanttChart.tsx
```

**기능:**
- 번다운 차트 (스프린트 진행도)
- 속도 차트 (팀 생산성)
- 간트 차트 (전체 일정)
- PDF/Excel 내보내기

### 3.5 스프린트 회고 화면

```typescript
src/app/components/SprintRetrospective.tsx
```

**기능:**
- Keep / Problem / Try 형식
- 팀원 투표 기능
- 액션 아이템 추출
- 과거 회고 이력

---

## 🎯 Phase 4: 협업 기능 강화

### 4.1 실시간 댓글 시스템

```typescript
// 백엔드
POST   /api/comments                   // 댓글 작성
GET    /api/comments?entity=task&id=1  // 댓글 조회
PUT    /api/comments/:id               // 댓글 수정
DELETE /api/comments/:id               // 댓글 삭제

// 프론트엔드
src/app/components/CommentSection.tsx
```

### 4.2 활동 피드 (Activity Feed)

```typescript
src/app/components/ActivityFeed.tsx
```

**기능:**
- 실시간 활동 스트림
- 필터링 (사용자별, 액션별)
- "누가 무엇을 언제" 추적

### 4.3 알림 시스템

```typescript
src/app/components/NotificationCenter.tsx
```

**알림 유형:**
- 태스크 할당
- Gate 승인 요청
- 스프린트 시작/종료
- 이슈 할당
- 댓글 멘션

---

## 🎯 Phase 5: 감사 및 보안

### 5.1 감사 로그 시스템

```typescript
// 백엔드
GET    /api/audit-logs                 // 전체 로그
GET    /api/audit-logs?user=U001       // 사용자별 로그
GET    /api/audit-logs?entity=task     // 엔티티별 로그

// 프론트엔드
src/app/components/AuditLogViewer.tsx
```

**추적 대상:**
- 로그인/로그아웃
- 데이터 생성/수정/삭제
- 승인/반려 액션
- 파일 업로드/다운로드

### 5.2 역할 기반 접근 제어 강화

```typescript
// 백엔드 미들웨어
src/middleware/rbac.ts

// 권한 체크 함수
const requirePermission = (permission: Permission) => {
  // sponsor: 읽기 전용
  // pmo_head: 전체 관리
  // pm: 프로젝트 관리
  // developer/qa: 태스크 관리
  // ba/auditor: 읽기 전용
}
```

---

## 🎯 Phase 6: 통합 및 확장성

### 6.1 외부 시스템 연동 API (Integration Points)

```typescript
// 다른 팀 시스템과의 연동을 위한 Webhook API
POST   /api/webhooks/register          // Webhook 등록
POST   /api/integrations/ai-status     // AI 팀에서 호출 (모델 상태 업데이트)
POST   /api/integrations/mlops-status  // MLOps 팀에서 호출
GET    /api/integrations/project-data  // 포탈 팀에서 조회

// 예시: AI 팀이 모델 배포 상태를 PMS에 알림
{
  "event": "model_deployed",
  "model_name": "claim_classifier_v2",
  "status": "success",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 6.2 데이터 내보내기/가져오기

```typescript
POST   /api/export/project/:id         // 프로젝트 데이터 내보내기 (JSON)
POST   /api/import/project             // 프로젝트 데이터 가져오기
GET    /api/export/template            // 프로젝트 템플릿 다운로드
```

---

## 📊 구현 우선순위

### Priority 1 (필수, 즉시 구현)
1. ✅ PostgreSQL 데이터베이스 설정
2. ✅ 프로젝트/단계/태스크 CRUD API
3. ✅ Gate 승인 워크플로우
4. ✅ 산출물 관리
5. ✅ 감사 로그

### Priority 2 (중요, 2주 내)
1. ⏳ 리스크/이슈 관리
2. ⏳ 스프린트 관리 강화
3. ⏳ 댓글 시스템
4. ⏳ 리포팅 기능

### Priority 3 (추가 기능, 1개월 내)
1. 🔜 고급 차트 (번다운, 간트)
2. 🔜 알림 시스템
3. 🔜 통합 API (다른 팀과 연동)
4. 🔜 데이터 내보내기/가져오기

---

## 🔧 기술 스택

### 백엔드
- **Runtime:** Node.js 18+ with TypeScript
- **Framework:** Express.js
- **Database:** PostgreSQL 15+
- **ORM:** Prisma (권장) 또는 TypeORM
- **Authentication:** JWT
- **File Storage:** Local file system (초기) → S3-compatible (추후)
- **Validation:** Zod
- **Testing:** Jest + Supertest

### 프론트엔드 (기존 유지)
- React 18.3 + TypeScript
- Vite
- TailwindCSS 4.x
- Radix UI
- React DnD
- Recharts

### 인프라
- **환경:** On-Premise (폐쇄망)
- **역방향 프록시:** Nginx (권장)
- **로그:** Winston + 파일 로테이션
- **백업:** PostgreSQL 자동 백업 스크립트

---

## 📝 예상 산출물

### 백엔드
- `PMS_IC_BackEnd_v1.2/src/db/schema.sql`
- `PMS_IC_BackEnd_v1.2/src/routes/projects.ts`
- `PMS_IC_BackEnd_v1.2/src/routes/risks.ts`
- `PMS_IC_BackEnd_v1.2/src/routes/issues.ts`
- `PMS_IC_BackEnd_v1.2/src/routes/deliverables.ts`
- `PMS_IC_BackEnd_v1.2/src/routes/reports.ts`
- `PMS_IC_BackEnd_v1.2/src/routes/webhooks.ts`
- `PMS_IC_BackEnd_v1.2/src/middleware/rbac.ts`
- `PMS_IC_BackEnd_v1.2/src/services/fileStorage.ts`

### 프론트엔드
- `src/app/components/RiskManagement.tsx`
- `src/app/components/IssueTracking.tsx`
- `src/app/components/DeliverableManagement.tsx`
- `src/app/components/ApprovalWorkflow.tsx`
- `src/app/components/ProjectReports.tsx`
- `src/app/components/SprintRetrospective.tsx`
- `src/app/components/CommentSection.tsx`
- `src/app/components/AuditLogViewer.tsx`

---

## ✅ 다음 단계

이 계획서를 검토하시고 승인해주시면:

1. **Phase 1부터 순차적으로 구현** 시작
2. 각 Phase 완료 후 **리뷰 및 피드백**
3. **우선순위 조정** 가능

**질문:**
- 이 범위가 적절한가요?
- 우선순위를 조정할 부분이 있나요?
- 추가로 필요한 PMS 기능이 있나요?

---

## 📌 중요 참고사항

이 PMS는 **프로젝트 관리**에 집중하며:
- ✅ 프로젝트 일정, 태스크, 팀 협업 관리
- ✅ 단계별 Gate 승인 워크플로우
- ✅ 리스크/이슈/산출물 추적
- ✅ 리포팅 및 진척 모니터링

다음은 **PMS 범위 밖**입니다:
- ❌ AI 모델 학습/배포
- ❌ MLOps 파이프라인
- ❌ 데이터 과학 분석
- ❌ 포탈 UI 구현

**다른 팀과의 협업:** Integration API를 통해 데이터만 주고받습니다.
