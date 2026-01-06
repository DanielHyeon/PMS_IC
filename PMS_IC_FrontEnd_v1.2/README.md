# PMS Insurance Claims Frontend v1.2

보험 심사 자동화를 위한 AI 기반 프로젝트 관리 시스템 프론트엔드

Original Figma Design: https://www.figma.com/design/nMR8K9TDG9Q9pEJBfLU0Xa/AI-Powered-Insurance-Claims-Management

## Features

- 역할 기반 접근 제어 (RBAC)
- 대시보드 with 실시간 차트
- 단계별 프로젝트 관리 (Waterfall)
- 칸반 보드 (Agile/Scrum)
- 백로그 관리
- AI 어시스턴트 통합
- **백엔드가 없어도 목업 데이터로 자동 동작**

## Installation

```bash
npm install
```

## Configuration (Optional)

백엔드 API를 사용하려면 `.env` 파일 생성:

```bash
cp .env.example .env
```

`.env` 파일 수정:
```
VITE_API_URL=http://localhost:3001/api
```

**참고:** 백엔드가 없으면 자동으로 목업 데이터를 사용합니다.

## Running the code

```bash
npm run dev
```

브라우저에서 http://localhost:5173 접속

## Build

```bash
npm run build
```

## 사용자 계정

### 백엔드 연동 시
모든 계정 비밀번호: `password123`

| 이메일 | 역할 |
|--------|------|
| kim@example.com | PM |
| lee@example.com | Developer |
| park@example.com | Developer |
| choi@example.com | QA |
| jung@example.com | PMO Head |
| kang@example.com | Sponsor |

### 목업 데이터 사용 시 (백엔드 없이)

| 이메일 | 비밀번호 | 역할 |
|--------|---------|------|
| sponsor@insure.com | sponsor123 | 스폰서 |
| pmo@insure.com | pmo123 | PMO 총괄 |
| pm@insure.com | pm123 | PM |
| dev@insure.com | dev123 | 개발자 |
| qa@insure.com | qa123 | QA |
| ba@insure.com | ba123 | 현업분석가 |
| auditor@insure.com | auditor123 | 감리 |
| admin@insure.com | admin123 | 관리자 |

## 역할별 권한

### Sponsor (스폰서)
- 대시보드, 단계별 관리, 역할 관리 조회
- 예산 정보 조회
- 읽기 전용

### PMO Head (PMO 총괄)
- 모든 메뉴 접근
- 승인 권한
- 전체 프로젝트 관리

### PM (프로젝트 매니저)
- 단계별 관리, 칸반, 백로그 관리
- 승인 및 우선순위 조정 권한

### Developer / QA
- 칸반 보드, 백로그 관리
- AI 어시스턴트 사용

### Business Analyst / Auditor
- 읽기 전용 모드

## 백엔드 연동

백엔드 서버 설정:
1. `../PMS_IC_BackEnd_v1.2` 폴더로 이동
2. `npm install` 실행
3. `.env` 파일 설정
4. `npm run dev` 실행

백엔드가 실행되면 자동으로 API를 호출하고, 없으면 목업 데이터를 사용합니다.

## Tech Stack

- React 18.3 + TypeScript
- Vite
- TailwindCSS 4.x
- Radix UI Components
- React DnD (Drag & Drop)
- Recharts (Charts)
- Lucide Icons

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── Dashboard.tsx          # 통합 대시보드
│   │   ├── PhaseManagement.tsx    # 단계별 관리
│   │   ├── KanbanBoard.tsx        # 칸반 보드
│   │   ├── BacklogManagement.tsx  # 백로그 관리
│   │   ├── RoleManagement.tsx     # 권한 관리
│   │   ├── AIAssistant.tsx        # AI 어시스턴트
│   │   ├── LoginScreen.tsx        # 로그인
│   │   ├── Header.tsx & Sidebar.tsx
│   │   └── ui/                    # UI 컴포넌트
│   └── App.tsx
├── services/
│   └── api.ts                     # API 서비스 (자동 fallback)
└── main.tsx
```

## 자동 Fallback 시스템

`src/services/api.ts`가 자동으로:
1. 백엔드 API 연결 시도
2. 실패 시 목업 데이터 사용
3. 사용자에게 투명하게 작동

이를 통해 백엔드 없이도 전체 시스템을 테스트할 수 있습니다.