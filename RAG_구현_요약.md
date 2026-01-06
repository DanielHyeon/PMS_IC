# RAG 시스템 구현 완료 요약

## ✅ 구현 완료 항목

### 1. 인프라 설정
- ✅ ChromaDB 컨테이너 추가 (포트 8001)
- ✅ docker-compose.yml 업데이트
- ✅ 서비스 간 의존성 설정 (healthcheck)

### 2. LLM 서비스 (Python/Flask)
- ✅ `rag_service.py` - RAG 핵심 로직 구현
  - 문서 임베딩 (SentenceTransformer - 다국어 지원)
  - 텍스트 청킹 (512 토큰, 50 오버랩)
  - PDF/Word/Excel 텍스트 추출
  - ChromaDB 벡터 검색
- ✅ `app.py` 업데이트
  - 문서 추가 API: `POST /api/documents`
  - 문서 검색 API: `POST /api/documents/search`
  - 문서 삭제 API: `DELETE /api/documents/<id>`
  - 통계 API: `GET /api/documents/stats`
  - 채팅 시 자동 RAG 검색 통합
- ✅ `requirements.txt` 업데이트
  - chromadb==0.4.22
  - sentence-transformers==2.3.1
  - pypdf==3.17.4
  - python-docx==1.1.0
  - openpyxl==3.1.2

### 3. 백엔드 (Java/Spring Boot)
- ✅ `RAGIndexingService.java` - 문서 인덱싱 서비스
  - PDF 텍스트 추출 (Apache PDFBox)
  - Word 텍스트 추출 (Apache POI)
  - Excel 텍스트 추출 (Apache POI)
  - LLM 서비스 API 호출
- ✅ `RAGSearchService.java` - 문서 검색 서비스
  - 질문에 대한 관련 문서 검색
  - LLM 서비스 검색 API 호출
- ✅ `DeliverableService.java` 수정
  - 파일 업로드 시 자동 RAG 인덱싱
  - 메타데이터 포함 (산출물 ID, 이름, 단계 등)
- ✅ `AIChatClient.java` 수정
  - 채팅 시 자동 RAG 검색 수행
  - 검색 결과를 LLM에 전달
- ✅ `pom.xml` 업데이트
  - Apache PDFBox 3.0.1
  - Apache POI 5.2.5

### 4. 프롬프트 최적화
- ✅ 한국어 전용 시스템 프롬프트
- ✅ RAG 문서 활용 지침 포함
- ✅ Gemma 3 포맷 최적화

### 5. 문서화
- ✅ `RAG_시스템_가이드.md` - 상세 사용 가이드
- ✅ `README.md` 업데이트
- ✅ `test-rag-system.sh` - 통합 테스트 스크립트

## 🎯 작동 방식

### 문서 업로드 → 인덱싱 플로우

```
사용자가 프론트엔드에서 PDF/Word/Excel 업로드
    ↓
DeliverableController (Spring Boot)
    ↓
DeliverableService - 파일 저장
    ↓
RAGIndexingService - 자동 호출
    ├─ PDF → Apache PDFBox로 텍스트 추출
    ├─ Word → Apache POI로 텍스트 추출
    └─ Excel → Apache POI로 텍스트 추출
        ↓
LLM 서비스 POST /api/documents
    ↓
RAGService (Python)
    ├─ 텍스트를 512 토큰 청크로 분할
    ├─ SentenceTransformer로 임베딩 생성
    └─ ChromaDB에 벡터 저장 ✓
```

### 채팅 → RAG 검색 → 응답 플로우

```
사용자가 채팅 질문 입력
    ↓
ChatService (Spring Boot)
    ↓
AIChatClient
    ↓
RAGSearchService - 관련 문서 검색
    ↓
LLM 서비스 POST /api/documents/search
    ↓
RAGService - ChromaDB 벡터 검색 (Top 3)
    ↓
검색 결과 반환 (관련 문서 3개)
    ↓
AIChatClient - LLM에 전달
    {
      "message": "질문",
      "context": [...],
      "retrieved_docs": ["문서1", "문서2", "문서3"]
    }
    ↓
LLM 서비스 /api/chat
    ↓
프롬프트 구성 (Gemma 3 포맷)
    - 시스템 프롬프트 (한국어 전용)
    - 관련 문서 (RAG)
    - 현재 질문
    ↓
Gemma 3 12B 모델 - 응답 생성
    ↓
사용자에게 정확한 답변 반환 ✓
```

## 🚀 사용 방법

### 1. 전체 시스템 시작

```bash
cd /wp/PMS_IC
docker-compose up -d
```

### 2. RAG 시스템 테스트

```bash
# 자동 테스트 스크립트 실행
./test-rag-system.sh
```

### 3. 프론트엔드에서 테스트

1. http://localhost:5173 접속
2. `admin@insure.com` / `admin123` 로그인
3. **프로젝트 → 단계별 산출물 업로드**
   - PDF, Word, Excel 파일 업로드
   - 업로드 시 자동으로 RAG에 인덱싱됨
4. **AI 챗봇 열기**
   - 업로드한 문서 내용에 대해 질문
   - RAG가 자동으로 관련 문서를 찾아 답변

### 4. 백엔드 로그 확인

```bash
# RAG 인덱싱 로그
docker-compose logs backend | grep "RAG"

# 출력 예시:
# Deliverable indexed to RAG successfully: abc-123-def
# Retrieved 3 documents from RAG for message: 프로젝트 일정은?
```

## 📊 주요 파일 위치

### Python (LLM 서비스)
```
llm-service/
├── rag_service.py          # RAG 핵심 로직
├── app.py                   # Flask API (RAG 통합)
└── requirements.txt         # Python 의존성
```

### Java (백엔드)
```
PMS_IC_BackEnd_v1.2/src/main/java/com/insuretech/pms/
├── rag/service/
│   ├── RAGIndexingService.java   # 문서 인덱싱
│   └── RAGSearchService.java     # 문서 검색
├── project/service/
│   └── DeliverableService.java   # 산출물 업로드 + RAG
└── chat/service/
    └── AIChatClient.java         # 채팅 + RAG
```

### 설정
```
docker-compose.yml           # ChromaDB 서비스
pom.xml                      # PDF/Word 라이브러리
```

## 🔧 설정 파라미터

### 검색 문서 개수 조정
**파일:** `AIChatClient.java:65`
```java
List<String> retrievedDocs = ragSearchService.searchRelevantDocuments(message, 3);
// 3 → 5로 변경하면 더 많은 문서 검색
```

### 청크 크기 조정
**파일:** `llm-service/rag_service.py:93`
```python
def chunk_text(self, text: str, chunk_size: int = 512, overlap: int = 50):
    # chunk_size: 256, 512, 1024 등으로 조정 가능
```

### 임베딩 모델 변경
**파일:** `llm-service/rag_service.py:28`
```python
# 현재: 다국어 모델
self.embedding_model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')

# 한국어 특화 모델로 변경하려면:
# self.embedding_model = SentenceTransformer('jhgan/ko-sroberta-multitask')
```

## ⚡ 성능 특징

### 장점
- ✅ 업로드한 문서 내용을 정확하게 참조
- ✅ 다국어 지원 (한국어, 영어 등)
- ✅ 자동 인덱싱 (사용자 개입 불필요)
- ✅ 확장 가능 (문서 수 제한 없음)
- ✅ 메타데이터 필터링 지원

### 고려사항
- ⚠️ 첫 임베딩 모델 로딩 시간: 약 10-20초
- ⚠️ 대용량 문서 인덱싱 시간: PDF 100페이지 기준 약 5-10초
- ⚠️ 메모리 사용량: 임베딩 모델 약 500MB

## 🎓 예제 시나리오

### 시나리오 1: 프로젝트 계획서 업로드

**업로드:** `프로젝트_계획서.pdf`
```
프로젝트명: AI 기반 보험 심사 시스템
기간: 2024년 1월 ~ 6월
예산: 5억원
팀: PM 1명, 개발자 5명, QA 2명
```

**질문:** "이 프로젝트 예산은 얼마인가요?"

**RAG 검색:**
→ "프로젝트명: AI 기반 보험 심사 시스템 기간: 2024년 1월 ~ 6월 예산: 5억원..."

**AI 응답:**
→ "프로젝트 계획서에 따르면, 이 프로젝트의 예산은 5억원입니다."

### 시나리오 2: 기술 명세서 업로드

**업로드:** `기술_명세서.docx`
```
백엔드: Spring Boot 3.2.1, PostgreSQL 15
프론트엔드: React 18, TypeScript
AI: Gemma 3 12B, ChromaDB
```

**질문:** "이 시스템에서 사용하는 AI 모델은?"

**AI 응답:**
→ "기술 명세서에 따르면, 이 시스템은 Gemma 3 12B 모델을 사용하고 있으며, 벡터 검색을 위해 ChromaDB를 활용합니다."

## 🔍 트러블슈팅

### ChromaDB 연결 실패
```bash
# ChromaDB 재시작
docker-compose restart chromadb

# 헬스체크
curl http://localhost:8001/api/v1/heartbeat
```

### 문서 인덱싱 실패
```bash
# 로그 확인
docker-compose logs backend | grep "RAG"
docker-compose logs llm-service | grep "Error"

# 수동 테스트
curl -X POST http://localhost:8000/api/documents -H "Content-Type: application/json" -d '{"documents":[{"id":"test","content":"테스트","metadata":{}}]}'
```

### RAG 검색 결과 없음
```bash
# 통계 확인
curl http://localhost:8000/api/documents/stats

# 검색 테스트
curl -X POST http://localhost:8000/api/documents/search -H "Content-Type: application/json" -d '{"query":"프로젝트","top_k":5}'
```

## 📈 향후 개선 계획

### 단기
- [ ] 비동기 인덱싱 (성능 개선)
- [ ] 문서 업데이트 시 자동 재인덱싱
- [ ] 하이브리드 검색 (키워드 + 벡터)

### 중기
- [ ] 검색 결과 출처 표시 (UI)
- [ ] 프로젝트별 컬렉션 분리
- [ ] 문서 요약 기능

### 장기
- [ ] OCR 지원 (스캔 문서)
- [ ] 이미지 내 텍스트 추출
- [ ] 다중 모달 검색 (텍스트 + 이미지)

## 📞 관련 문서

- **상세 가이드:** [RAG_시스템_가이드.md](./RAG_시스템_가이드.md)
- **LLM 연동:** [LLM_연동_가이드.md](./LLM_연동_가이드.md)
- **테스트 스크립트:** `./test-rag-system.sh`

---

**구현 완료일:** 2026-01-03
**기술 스택:** Spring Boot 3.2.1, Flask, Gemma 3 12B, ChromaDB, SentenceTransformers
**상태:** ✅ 완전 통합 완료
