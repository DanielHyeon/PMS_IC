# 챗봇 대화 저장 및 세션 관리 기능 개선 요약

## ✅ 구현 완료 사항

### 1. 대화 저장 기능 확인 및 개선
- **현재 상태**: ✅ 이미 구현되어 있음
  - `ChatMessage` 엔티티로 사용자 메시지와 AI 응답 모두 저장
  - `ChatSession`으로 세션 관리
  - PostgreSQL 데이터베이스에 영구 저장

### 2. 기존 대화 불러오기 기능 개선

#### 문제점
- `getRecentMessages()` 메서드가 중복 쿼리 실행 (비효율적)
- 최근 10개 메시지만 가져와서 초기 맥락 손실 가능

#### 개선 사항
- ✅ **스마트 컨텍스트 선택**: `getContextMessages()` 메서드 추가
  - 초기 대화 맥락 (처음 3개) + 최근 대화 (최근 10개) 조합
  - 중복 제거 및 시간순 정렬
- ✅ **쿼리 최적화**: `ChatMessageRepository`에 최적화된 쿼리 추가
- ✅ **컨텍스트 전달**: LLM에 더 풍부한 맥락 제공

### 3. 세션 목록 조회 API 추가
- ✅ **GET `/api/chat/sessions`**: 현재 사용자의 활성 세션 목록 조회
- ✅ **ChatSessionDto**: 세션 정보를 담는 DTO 생성

### 4. 새로운 대화 시작 API 추가
- ✅ **POST `/api/chat/sessions`**: 새로운 채팅 세션 생성
- ✅ **CreateSessionRequest**: 세션 생성 요청 DTO
- ✅ 사용자가 명시적으로 새 대화를 시작할 수 있음

### 5. 세션 제목 자동 생성/업데이트
- ✅ **자동 생성**: 첫 메시지의 앞부분(30자)을 제목으로 자동 설정
- ✅ **수동 수정**: PUT `/api/chat/session/{sessionId}/title` API로 제목 수정 가능
- ✅ **UpdateSessionTitleRequest**: 제목 수정 요청 DTO

### 6. 대화 맥락 유지 개선
- ✅ **스마트 컨텍스트**: 초기 대화 + 최근 대화 조합으로 맥락 유지
- ✅ **자동 전달**: 기존 대화가 자동으로 LLM 컨텍스트에 포함
- ✅ **세션 기반**: 각 세션별로 독립적인 대화 맥락 유지

---

## 📋 주요 변경 파일

### 새로운 파일
- `ChatSessionDto.java`: 세션 정보 DTO
- `CreateSessionRequest.java`: 세션 생성 요청 DTO
- `UpdateSessionTitleRequest.java`: 제목 수정 요청 DTO

### 수정된 파일
- `ChatMessageRepository.java`: 최적화된 쿼리 메서드 추가
- `ChatService.java`: 
  - `getContextMessages()`: 스마트 컨텍스트 선택
  - `getUserSessions()`: 사용자 세션 목록 조회
  - `createNewSession()`: 새 세션 생성
  - `updateSessionTitle()`: 세션 제목 업데이트
  - `updateSessionTitle()` (private): 자동 제목 생성
- `ChatController.java`: 새로운 API 엔드포인트 추가

---

## 🎯 API 엔드포인트

### 기존 API
1. **POST `/api/chat/message`**: 메시지 전송
   - `sessionId`가 있으면 기존 세션에 추가
   - `sessionId`가 없으면 새 세션 생성

2. **GET `/api/chat/history/{sessionId}`**: 대화 히스토리 조회

3. **DELETE `/api/chat/session/{sessionId}`**: 세션 삭제

### 새로운 API
4. **GET `/api/chat/sessions`**: 세션 목록 조회
   ```json
   Response: [
     {
       "id": "session-id",
       "userId": "user-id",
       "title": "프로젝트 현황 알려줘",
       "active": true,
       "createdAt": "2024-01-01T10:00:00",
       "updatedAt": "2024-01-01T10:05:00"
     }
   ]
   ```

5. **POST `/api/chat/sessions`**: 새로운 대화 시작
   ```json
   Request: {
     "title": "새로운 대화" // 선택적
   }
   Response: {
     "id": "new-session-id",
     "title": "새로운 대화",
     ...
   }
   ```

6. **PUT `/api/chat/session/{sessionId}/title`**: 세션 제목 수정
   ```json
   Request: {
     "title": "수정된 제목"
   }
   ```

---

## 🔄 동작 흐름

### 기존 대화 계속하기
```
1. 사용자가 sessionId와 함께 메시지 전송
   POST /api/chat/message
   {
     "sessionId": "existing-session-id",
     "message": "그 프로젝트의 진행률은?"
   }
   ↓
2. ChatService가 세션 조회
   ↓
3. getContextMessages()로 기존 대화 맥락 조회
   - 초기 대화 (처음 3개)
   - 최근 대화 (최근 10개)
   ↓
4. LLM에 기존 맥락 + 새 메시지 전달
   ↓
5. LLM이 맥락을 이해하고 답변 생성
   ↓
6. 새 메시지와 응답 저장
```

### 새로운 대화 시작하기
```
방법 1: 세션 생성 API 사용
1. POST /api/chat/sessions
   {
     "title": "프로젝트 질문"
   }
   ↓
2. 새 sessionId 받기
   ↓
3. 받은 sessionId로 메시지 전송

방법 2: sessionId 없이 메시지 전송
1. POST /api/chat/message
   {
     "message": "안녕하세요"
   }
   ↓
2. 자동으로 새 세션 생성
   ↓
3. 첫 메시지로 제목 자동 생성
```

---

## 💡 주요 개선점

### 1. 스마트 컨텍스트 선택
- **이전**: 최근 10개 메시지만 전달
- **개선**: 초기 대화(3개) + 최근 대화(10개) 조합
- **효과**: 대화의 전체 맥락을 유지하면서 최근 대화도 포함

### 2. 세션 관리 개선
- **이전**: 세션 목록 조회 불가
- **개선**: 사용자의 모든 활성 세션 조회 가능
- **효과**: 여러 주제의 대화를 관리 가능

### 3. 제목 자동 생성
- **이전**: 모든 세션이 "New Chat"
- **개선**: 첫 메시지 기반 자동 제목 생성
- **효과**: 세션을 쉽게 구분 가능

### 4. 명시적 새 대화 시작
- **이전**: sessionId를 null로 보내야 함 (불명확)
- **개선**: 전용 API로 명시적 세션 생성
- **효과**: 사용자 경험 개선

---

## 🚀 사용 예시

### 시나리오 1: 프로젝트 관련 대화
```javascript
// 1. 새 대화 시작
POST /api/chat/sessions
{ "title": "프로젝트 질문" }

// 2. 첫 메시지
POST /api/chat/message
{
  "sessionId": "session-1",
  "message": "프로젝트 현황 알려줘"
}

// 3. 기존 대화 맥락 유지하며 추가 질문
POST /api/chat/message
{
  "sessionId": "session-1",
  "message": "그 중에서 진행률이 가장 높은 프로젝트는?"
}
// → LLM이 이전 대화를 기억하고 답변
```

### 시나리오 2: 여러 주제의 대화 관리
```javascript
// 1. 세션 목록 조회
GET /api/chat/sessions
// → [
//     { "id": "session-1", "title": "프로젝트 질문" },
//     { "id": "session-2", "title": "일정 문의" },
//     { "id": "session-3", "title": "기술 질문" }
//   ]

// 2. 원하는 세션 선택하여 계속 대화
POST /api/chat/message
{
  "sessionId": "session-1",
  "message": "프로젝트 예산은?"
}
```

---

## ⚠️ 주의사항

1. **컨텍스트 크기**: 초기 대화 + 최근 대화 조합으로 컨텍스트가 커질 수 있음
   - 현재: 최대 13개 메시지 (초기 3개 + 최근 10개)
   - 향후: 토큰 수 기반 제한 고려

2. **세션 제목 길이**: 자동 생성 시 30자로 제한
   - 더 긴 제목이 필요하면 수동 수정 API 사용

3. **세션 삭제**: `active = false`로 설정 (실제 삭제 아님)
   - 향후: 완전 삭제 옵션 추가 고려

---

## 📝 향후 개선 사항

1. **컨텍스트 최적화**: 토큰 수 기반 스마트 선택
2. **세션 검색**: 제목 기반 세션 검색 기능
3. **세션 공유**: 세션을 다른 사용자와 공유하는 기능
4. **대화 내보내기**: 대화 내용을 파일로 내보내기
5. **대화 요약**: 긴 대화의 요약 생성

---

**구현 완료일**: 2024년
**구현자**: AI Assistant


