# RAG 시스템 구현 가이드

## 📋 개요

이 문서는 PMS 시스템에 구현된 RAG (Retrieval-Augmented Generation) 시스템에 대한 설명입니다.

사용자가 업로드한 프로젝트 산출물(PDF, Word, Excel 등)을 자동으로 벡터 데이터베이스에 인덱싱하고, 채팅 시 관련 문서를 검색하여 더 정확한 답변을 제공합니다.

## 🏗️ 아키텍처

```
프론트엔드 (React)
    ↓
    1. 문서 업로드
    ↓
백엔드 (Spring Boot) :8080
    ├─ DeliverableService (문서 저장)
    │   └─ RAGIndexingService (문서 인덱싱)
    │       └─ PDF/Word/Excel 텍스트 추출
    │           ↓
    └─ ChatService (채팅 처리)
        └─ RAGSearchService (관련 문서 검색)
            ↓
LLM 서비스 (Flask) :8000
    ├─ RAGService (문서 임베딩 및 검색)
    │   ├─ SentenceTransformer (다국어 임베딩)
    │   └─ ChromaDB (벡터 저장소)
    └─ Gemma 3 12B (텍스트 생성)
```

## ✅ 구현된 기능

### 1. 자동 문서 인덱싱
- 사용자가 단계별 산출물을 업로드하면 자동으로 RAG 시스템에 인덱싱
- 지원 파일 형식: PDF, Word (doc/docx), Excel (xls/xlsx), TXT, MD
- 텍스트 추출 → 청크 분할 (512 토큰) → 임베딩 생성 → ChromaDB 저장

### 2. 지능형 문서 검색
- 채팅 질문에 대해 자동으로 관련 문서 검색 (Top 3)
- 다국어 지원 임베딩 모델 (`paraphrase-multilingual-MiniLM-L12-v2`)
- 문서 메타데이터 포함 (산출물 ID, 이름, 단계, 업로드자 등)

### 3. RAG 기반 응답 생성
- 검색된 관련 문서를 프롬프트에 포함
- Gemma 3 모델이 문서 정보를 활용하여 정확한 답변 생성
- 한국어 전용 프롬프트 최적화

## 📁 주요 파일

### LLM 서비스 (Python)
- `llm-service/rag_service.py` - RAG 핵심 로직
- `llm-service/app.py` - Flask API 엔드포인트
- `llm-service/requirements.txt` - Python 의존성

### 백엔드 (Java/Spring Boot)
- `com.insuretech.pms.rag.service.RAGIndexingService` - 문서 인덱싱
- `com.insuretech.pms.rag.service.RAGSearchService` - 문서 검색
- `com.insuretech.pms.project.service.DeliverableService` - 산출물 업로드 통합
- `com.insuretech.pms.chat.service.AIChatClient` - 채팅 RAG 통합

### 인프라
- `docker-compose.yml` - ChromaDB 서비스 추가
- `pom.xml` - PDF/Word 처리 라이브러리 추가

## 🚀 시작하기

### 1. 전체 시스템 실행

```bash
cd /wp/PMS_IC

# 모든 서비스 시작 (ChromaDB 포함)
docker-compose up -d

# 로그 확인
docker-compose logs -f chromadb
docker-compose logs -f llm-service
docker-compose logs -f backend
```

### 2. 서비스 상태 확인

```bash
# ChromaDB 헬스체크
curl http://localhost:8001/api/v1/heartbeat

# LLM 서비스 헬스체크 (RAG 포함)
curl http://localhost:8000/health
# 응답: {"status":"healthy","model_loaded":true,"rag_service_loaded":true}

# 백엔드 헬스체크
curl http://localhost:8080/actuator/health
```

### 3. RAG 통계 확인

```bash
# RAG에 인덱싱된 문서 수 확인
curl http://localhost:8000/api/documents/stats
# 응답: {"total_chunks":150,"collection_name":"pms_documents"}
```

## 🧪 테스트 방법

### 1. 문서 업로드 및 자동 인덱싱

**Step 1: 로그인**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@insure.com",
    "password": "admin123"
  }' > /tmp/login.json

# 토큰 추출
TOKEN=$(cat /tmp/login.json | grep -o 'eyJ[^"]*' | head -1)
```

**Step 2: 산출물 업로드** (자동으로 RAG에 인덱싱됨)
```bash
curl -X POST "http://localhost:8080/api/phases/{phaseId}/deliverables" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/document.pdf" \
  -F "name=프로젝트 계획서" \
  -F "description=1단계 프로젝트 계획 문서" \
  -F "type=DOCUMENT"
```

**Step 3: 백엔드 로그 확인**
```bash
docker-compose logs backend | grep "RAG"
# 출력 예시:
# Deliverable indexed to RAG successfully: abc-123-def
```

### 2. RAG 기반 채팅 테스트

```bash
# 채팅 메시지 전송 (자동으로 RAG 검색 수행)
curl -X POST http://localhost:8080/api/chat/message \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": null,
    "message": "프로젝트 계획서에 명시된 주요 일정은?"
  }'
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid-xxx",
    "reply": "프로젝트 계획서에 따르면 주요 일정은 다음과 같습니다:\n1. 요구사항 분석: 1-2주차\n2. 설계: 3-4주차\n3. 개발: 5-10주차\n4. 테스트: 11-12주차",
    "confidence": 0.85,
    "suggestions": []
  }
}
```

### 3. RAG 직접 검색 테스트

```bash
# LLM 서비스의 RAG 검색 API 직접 호출
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "프로젝트 일정",
    "top_k": 3
  }'
```

**응답 예시:**
```json
{
  "query": "프로젝트 일정",
  "count": 3,
  "results": [
    {
      "content": "프로젝트 전체 일정은 12주로 계획되어 있으며...",
      "metadata": {
        "deliverable_id": "abc-123",
        "deliverable_name": "프로젝트 계획서",
        "phase_id": "phase-1",
        "type": "DOCUMENT"
      },
      "distance": 0.23
    },
    ...
  ]
}
```

## 📊 RAG 프로세스 상세

### 문서 업로드 → 인덱싱 플로우

```
1. 사용자가 프론트엔드에서 파일 업로드
   ↓
2. DeliverableController가 요청 수신
   ↓
3. DeliverableService가 파일 저장
   ↓
4. RAGIndexingService가 자동 호출됨
   ↓
5. 파일 타입에 따라 텍스트 추출
   - PDF: Apache PDFBox
   - Word: Apache POI
   - Excel: Apache POI
   - TXT/MD: 직접 읽기
   ↓
6. LLM 서비스 /api/documents API 호출
   ↓
7. RAGService가 텍스트 청킹 (512 토큰)
   ↓
8. SentenceTransformer로 임베딩 생성
   ↓
9. ChromaDB에 벡터 저장
   ✓ 인덱싱 완료
```

### 채팅 → RAG 검색 → 응답 플로우

```
1. 사용자가 채팅 메시지 입력
   ↓
2. ChatService가 메시지 수신
   ↓
3. AIChatClient.callFlaskLLM() 호출
   ↓
4. RAGSearchService가 관련 문서 검색
   - 질문을 임베딩으로 변환
   - ChromaDB에서 유사도 검색 (Top 3)
   ↓
5. 검색 결과를 LLM 서비스에 전달
   {
     "message": "질문",
     "context": [...],
     "retrieved_docs": ["문서1", "문서2", "문서3"]
   }
   ↓
6. LLM 서비스가 프롬프트 구성
   - 시스템 프롬프트
   - 관련 문서 (RAG)
   - 현재 질문
   ↓
7. Gemma 3 모델이 응답 생성
   ↓
8. 사용자에게 응답 반환
   ✓ RAG 기반 정확한 답변
```

## ⚙️ 설정 및 최적화

### 환경 변수

**LLM 서비스 (docker-compose.yml)**
```yaml
environment:
  MODEL_PATH: /app/models/google.gemma-3-12b-pt.Q6_K.gguf
  MAX_TOKENS: 256
  TEMPERATURE: 0.7
  TOP_P: 0.9
  CHROMA_HOST: chromadb
  CHROMA_PORT: 8000
```

**백엔드 (application.yml)**
```yaml
ai:
  service:
    url: http://llm-service:8000
    timeout: 30000

pms:
  storage:
    deliverables: uploads/deliverables
```

### RAG 파라미터 조정

**검색 문서 개수 조정** - `AIChatClient.java:65`
```java
List<String> retrievedDocs = ragSearchService.searchRelevantDocuments(message, 3);  // 3개 → 5개로 변경
```

**청킹 크기 조정** - `llm-service/rag_service.py:93`
```python
def chunk_text(self, text: str, chunk_size: int = 512, overlap: int = 50):
    # chunk_size를 조정하여 청크 크기 변경 (예: 256, 512, 1024)
```

**임베딩 모델 변경** - `llm-service/rag_service.py:28`
```python
# 한국어 특화 모델로 변경
self.embedding_model = SentenceTransformer('jhgan/ko-sroberta-multitask')
```

## 🔍 트러블슈팅

### 1. ChromaDB 연결 실패

**증상:**
```
Failed to load RAG service: Connection refused
```

**해결:**
```bash
# ChromaDB 서비스 상태 확인
docker-compose ps chromadb

# ChromaDB 재시작
docker-compose restart chromadb

# 헬스체크
curl http://localhost:8001/api/v1/heartbeat
```

### 2. 문서 인덱싱 실패

**증상:**
```
Failed to index deliverable to RAG: xxx
```

**해결:**
```bash
# LLM 서비스 로그 확인
docker-compose logs llm-service | grep "Error"

# 수동으로 문서 인덱싱 테스트
curl -X POST http://localhost:8000/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [{
      "id": "test-doc",
      "content": "테스트 문서 내용",
      "metadata": {"source": "test"}
    }]
  }'
```

### 3. RAG 검색 결과 없음

**증상:**
- 채팅 응답이 일반적인 답변만 제공
- 업로드한 문서 내용이 반영되지 않음

**해결:**
```bash
# RAG 통계 확인 (인덱싱된 문서 수)
curl http://localhost:8000/api/documents/stats

# 검색 테스트
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "프로젝트", "top_k": 5}'

# 임베딩 모델이 로드되었는지 확인
docker-compose logs llm-service | grep "Embedding model loaded"
```

### 4. PDF 텍스트 추출 실패

**증상:**
```
No text extracted from file: xxx.pdf
```

**원인:**
- 스캔된 이미지 PDF (OCR 필요)
- 암호화된 PDF
- 손상된 파일

**해결:**
- PDF 파일이 텍스트 기반인지 확인
- OCR이 필요한 경우, Tesseract 추가 구현 고려

## 📈 성능 최적화

### 1. 임베딩 캐싱
- 자주 검색되는 쿼리는 Redis에 캐싱
- 캐시 TTL: 1시간

### 2. 배치 인덱싱
- 여러 문서를 한 번에 인덱싱할 때 배치 처리
- 임베딩 생성을 배치로 수행하여 성능 향상

### 3. 비동기 인덱싱
- 문서 업로드 응답 속도 개선을 위해 비동기 처리 권장
- Spring `@Async` 어노테이션 활용

```java
@Async
public CompletableFuture<Boolean> indexFileAsync(String documentId, Path filePath, Map<String, String> metadata) {
    boolean result = indexFile(documentId, filePath, metadata);
    return CompletableFuture.completedFuture(result);
}
```

### 4. GPU 가속 (선택사항)
- 임베딩 모델 GPU 실행으로 속도 향상
- CUDA 지원 Docker 이미지 사용

## 🔐 보안 고려사항

### 1. 문서 접근 제어
- 사용자별 문서 접근 권한 필터링
- 메타데이터에 `user_id` 또는 `team_id` 추가

### 2. 민감 정보 보호
- 개인정보가 포함된 문서는 별도 처리
- 인덱싱 전 민감 정보 마스킹

### 3. API 인증
- 모든 RAG API 엔드포인트에 JWT 인증 적용 (백엔드 통해서만 접근)

## 📝 향후 개선 사항

### 단기
- [ ] 문서 업데이트 시 자동 재인덱싱
- [ ] 문서 삭제 시 RAG에서도 삭제
- [ ] 하이브리드 검색 (키워드 + 벡터)

### 중기
- [ ] 문서 요약 기능
- [ ] 다중 컬렉션 지원 (프로젝트별 분리)
- [ ] 검색 결과 출처 표시 (어떤 문서에서 가져왔는지)

### 장기
- [ ] OCR 지원 (스캔 문서)
- [ ] 이미지 내 텍스트 추출
- [ ] 실시간 문서 협업 기능

## 🎯 사용 예시

### 예시 1: 프로젝트 계획서 기반 질문

**업로드된 문서:** `프로젝트_계획서.pdf`
```
프로젝트명: AI 기반 보험 심사 시스템
기간: 2024년 1월 ~ 6월 (6개월)
주요 마일스톤:
- 1월: 요구사항 분석
- 2-3월: 시스템 설계
- 4-5월: 개발
- 6월: 테스트 및 배포
```

**사용자 질문:** "이 프로젝트는 언제 시작하나요?"

**RAG 검색 결과:**
```
1. "프로젝트명: AI 기반 보험 심사 시스템 기간: 2024년 1월 ~ 6월 (6개월)..."
```

**AI 응답:**
```
프로젝트 계획서에 따르면, 이 프로젝트는 2024년 1월에 시작합니다.
전체 기간은 6개월이며, 2024년 6월에 완료될 예정입니다.
```

### 예시 2: 기술 명세서 기반 질문

**업로드된 문서:** `기술_명세서.docx`
```
사용 기술 스택:
- 백엔드: Spring Boot 3.2.1, Java 17
- 프론트엔드: React 18, TypeScript
- 데이터베이스: PostgreSQL 15
- AI: Gemma 3 12B, ChromaDB
```

**사용자 질문:** "이 시스템에서 사용하는 데이터베이스는?"

**AI 응답:**
```
기술 명세서에 따르면, 이 시스템은 PostgreSQL 15를 메인 데이터베이스로 사용하고 있으며,
벡터 검색을 위해 ChromaDB도 함께 활용하고 있습니다.
```

## 📞 지원

문제가 발생하면:
1. 로그 확인: `docker-compose logs -f`
2. 헬스체크 수행
3. RAG 통계 확인
4. 이 문서의 트러블슈팅 섹션 참조

---

**구축 완료일:** 2026-01-03
**버전:** 1.0.0
**기술 스택:** Spring Boot 3.2.1, Flask, Gemma 3 12B, ChromaDB, SentenceTransformers
