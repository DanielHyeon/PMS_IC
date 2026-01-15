# PSM 추가요건 구현 계획서

> 기준 문서: `PSM 추가요건_20260114`
> 작성일: 2026-01-15
> 상태: 계획 수립 완료, 구현 대기

---

## 1. 현재 구현 상태 vs 요구사항 비교

| 요구사항 영역 | 현재 상태 | 필요 작업 |
|-------------|----------|----------|
| **통합 대시보드** | ✅ 기본 구현됨 | 🔶 확장 필요 |
| **단계별 관리** | ✅ PhaseManagement 존재 | 🔶 AI/SI 투트랙 확장 |
| **공통 관리** | ⚠️ 부분 구현 | 🔴 산출물/회의/이슈 통합 |
| **교육 관리** | ❌ 미구현 | 🔴 신규 구현 필요 |
| **RFP → 요구사항 자동분류** | ✅ 기본 추출 구현 | 🔶 AI/SI/공통 자동분류 추가 |
| **AI 오케스트레이션** | ❌ 미구현 | ⬜ 향후 개발 (옵션) |

---

## 2. 신규 구현 필요 항목

### 2.1 교육 관리 모듈 (신규)

**Backend 엔티티:**
- `Education` - 교육 과정 정의
- `EducationSession` - 교육 세션 (일정, 장소, 강사)
- `EducationHistory` - 참여자별 수강 이력
- `EducationRoadmap` - 역할별 로드맵 매트릭스

**기능:**
- IT 교육: 역할별 로드맵 (기초→중급→고급)
  - 에이전트 AI, 머신러닝, 딥러닝, 파이썬 등
- 현업 교육: AI 인식, 사례 기반 (기획/운영)
- 교육 히스토리 관리 (참여자, 날짜, 목차)
- 로드맵 매트릭스 UI (역할 × 레벨)
- 사후 교육: 구축 에이전트 역할 설명

**Frontend 컴포넌트:**
- `EducationManagement.tsx`
- `EducationRoadmap.tsx`
- `EducationHistory.tsx`

---

### 2.2 공통 관리 통합 (재구성)

**Backend 엔티티:**
- `Deliverable` - 산출물 (제출일, 검수일, 버전, 히스토리)
- `Meeting` - 회의 (일정, 히스토리, 요구사항 연동)
- `Issue` - 이슈 (담당자, 확인자, 마감일, 진행상황)

**기능:**
- 산출물 관리: 목록, 버전 히스토리, 검수 확인서 업로드
- 회의 관리: 주간/착수/종료 보고 일정, 회의록
- 이슈 관리: 발생→할당→해결 워크플로우

**Frontend 컴포넌트:**
- `CommonManagement.tsx` (탭 구조)
  - `DeliverableTab.tsx`
  - `MeetingTab.tsx`
  - `IssueTab.tsx`

---

## 3. 확장/수정 필요 항목

### 3.1 통합 대시보드 확장

**추가 기능:**
- AI/SI/공통 항목별 진척률 분리 표시
- 파트 리더별 책임 영역 가시화
- 요구사항 상태 분포 (완료/진행중/미착수) 히트맵
- LLM 자동 요약 ("이번 주 지연 이슈 3건")
- 색상 코딩 (녹색: 완료, 황색: 진행, 적색: 미착수)

**수정 파일:**
- `Dashboard.tsx` - UI 확장
- `api.ts` - 대시보드 통계 API 추가
- `app.py` - LLM 요약 엔드포인트 추가

---

### 3.2 요구사항 자동 분류 고도화

**분류 카테고리 확장:**
```
RequirementCategory:
  - AI_FUNCTIONAL (AI 기능 요구) - 30%
  - SI_FUNCTIONAL (SI 기능 요구) - 30%
  - COMMON (공통 기능: 포털, 연계, 인터페이스) - 15%
  - NON_FUNCTIONAL_PERFORMANCE (성능)
  - NON_FUNCTIONAL_SECURITY (보안)
  - NON_FUNCTIONAL_INFRA (인프라)
  - 합계 비기능: 25%
```

**수정 파일:**
- `Requirement.java` - enum 확장
- `app.py` - 분류 프롬프트 고도화
- `RequirementManagement.tsx` - 필터 UI 확장

---

### 3.3 단계별 관리 투트랙 확장

**AI 트랙 단계:**
1. 요구사항 분석
2. 데이터 수집
3. OCR 처리
4. AI 적용
5. 테스트
6. 운영
7. 검증

**SI 트랙 단계:**
1. 분석
2. 설계
3. 개발
4. 테스트

**기능:**
- 투 트랙 병행 관리 및 상호 연계 표시
- 트랙별 번다운 차트
- 스프린트/태스크 관리 강화

**수정 파일:**
- `PhaseManagement.tsx` - 투트랙 UI
- Phase 관련 엔티티에 `trackType` 필드 추가

---

## 4. 향후 개발 (옵션)

### 4.1 AI 에이전트 오케스트레이션

**기능:**
- 에이전트 모니터링 (큐/버틀넥 대시보드)
- 이벤트 트리거 (승인 → 다음 에이전트 자동 시작)
- 온/오프 스위치, 롤백 기능
- 모바일 알림 지원

**구현 시 고려사항:**
- 클라우드 풀 기반 구조
- SI 업체 협업 시 분리 옵션 제공
- 엔드 투 엔드 성능 영향 최소화

---

## 5. 구현 우선순위 및 Phase 계획

| Phase | 내용 | Backend | Frontend | 예상 규모 | 우선순위 |
|-------|------|---------|----------|----------|---------|
| **Phase A** | 공통 관리 통합 | Entity 3개, Service 3개, Controller 3개 | 컴포넌트 4개 | 중 | 🔴 높음 |
| **Phase B** | 교육 관리 모듈 | Entity 4개, Service 2개, Controller 2개 | 컴포넌트 3개 | 대 | 🔴 높음 |
| **Phase C** | 통합 대시보드 확장 | API 추가, LLM 엔드포인트 | Dashboard 확장 | 중 | 🔶 중간 |
| **Phase D** | 요구사항 자동 분류 고도화 | enum 확장, 프롬프트 수정 | 필터 UI 확장 | 소 | 🔶 중간 |
| **Phase E** | 단계별 관리 투트랙 | trackType 필드 추가 | PhaseManagement 확장 | 중 | 🔶 중간 |
| **Phase F** | AI 오케스트레이션 | 신규 모듈 | 신규 대시보드 | 대 | ⬜ 낮음 |

---

## 6. 기술 스택 참조

### Backend (Spring Boot)
- 기존 구조 활용: Entity → Repository → Service → Controller
- MongoDB 컬렉션 추가

### Frontend (React + TypeScript)
- 기존 컴포넌트 패턴 활용
- shadcn/ui 컴포넌트 재사용

### LLM 서비스 (Flask)
- 기존 `app.py` 확장
- 프롬프트 엔지니어링 고도화

---

## 7. 시작 명령어

별도 세션에서 다음 명령어로 시작:

```
Phase A부터 구현을 시작해주세요. (공통 관리 통합: 산출물/회의/이슈)
```

또는 특정 Phase 지정:

```
Phase B 교육 관리 모듈부터 구현해주세요.
```

---

## 8. 참고 문서

- 원본 요건서: `/docs/PSM 추가요건_20260114`
- 기존 구현 코드:
  - Backend: `/PMS_IC_BackEnd_v1.2/src/main/java/com/insuretech/pms/`
  - Frontend: `/PMS_IC_FrontEnd_v1.2/src/app/components/`
  - LLM Service: `/llm-service/app.py`
