# RAG 시스템 V2 - 다음 단계 가이드

## Docker 빌드가 완료되면 실행할 명령어들

### 1. 서비스 시작 확인
```bash
cd /wp/PMS_IC

# 컨테이너 상태 확인
docker-compose ps

# ChromaDB가 실행 중인지 확인
docker-compose ps chromadb

# LLM 서비스가 빌드 완료되었는지 확인
docker-compose ps llm-service
```

### 2. LLM 서비스 시작 (빌드 완료 후)
```bash
# LLM 서비스 시작
docker-compose up -d llm-service

# 로그 확인 (RAG V2 로딩 확인)
docker-compose logs -f llm-service
```

**예상 로그:**
```
Loading RAG service V2 (MinerU-based)...
Loading embedding model...
Embedding model loaded successfully
Initializing MinerU document parser...
Document parser initialized
RAG service V2 loaded successfully
```

### 3. 벡터 DB 초기화 및 데이터 재색인
```bash
# 초기화 스크립트 실행
docker exec pms-llm-service python3 init_rag_v2.py
```

**예상 출력:**
```
================================================================================
MinerU2.5 기반 RAG 시스템 초기화
================================================================================

1. RAG 서비스 초기화...
2. 기존 벡터 DB 초기화...
✅ 벡터 DB가 초기화되었습니다.

3. Mock 데이터 재처리 (10개 문서)...
   - MinerU 기반 구조 파싱 적용
   - Layout-Aware Chunking 적용

Parsing document project_overview with MinerU...
Document project_overview parsed into 25 blocks and 12 chunks
✅ Added document project_overview with 12 layout-aware chunks

... (9개 문서 더)

✅ 10/10 문서 처리 완료

4. 벡터 DB 통계 조회...

컬렉션 통계:
- 컬렉션명: pms_documents_v2
- 전체 청크 수: 142
- 구조화된 청크: 45
- 표 포함 청크: 18
- 리스트 포함 청크: 27
- 파서: MinerU2.5-based

5. 검색 품질 테스트...

질문: 현재 프로젝트 진행률은?
  결과 1:
    - 관련성 점수: 0.935
    - 거리: 0.152
    - 제목: 프로젝트 KPI
    - 구조화: True
    - 내용: [CONTEXT] 프로젝트 KPI [TITLE] 전체 진행률 전체 진행률: 62% 목표: 100% (2024-12-31) 현재 상태: 순조...

================================================================================
✅ RAG 시스템 초기화 완료!
================================================================================
```

### 4. 검색 품질 테스트 (API)
```bash
# 간단한 검색 테스트
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "현재 프로젝트 진행률은?",
    "top_k": 3
  }'
```

### 5. 통계 확인
```bash
# 컬렉션 통계 조회
curl http://localhost:8000/api/documents/stats
```

**예상 응답:**
```json
{
  "total_chunks": 142,
  "collection_name": "pms_documents_v2",
  "structured_chunks": 45,
  "chunks_with_tables": 18,
  "chunks_with_lists": 27,
  "parser": "MinerU2.5-based"
}
```

## 기존 RAG vs 새 RAG 비교 테스트

### 테스트 쿼리들
```bash
# 1. 진행률 관련
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "현재 프로젝트 진행률은?", "top_k": 2}'

# 2. 팀원 정보
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "프로젝트 팀원은 누가 있나요?", "top_k": 2}'

# 3. OCR 모델 성능
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "OCR 모델의 정확도는?", "top_k": 2}'

# 4. 구조화된 데이터 (표)
curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "스프린트 속도 추이는?", "top_k": 2}'
```

### 비교 포인트
- **컨텍스트 완전성**: 제목과 내용이 함께 반환되는지
- **표 데이터 유지**: 표가 온전히 포함되는지
- **Relevance Score**: 더 높은 점수를 받는지
- **거리(Distance)**: 더 낮은 거리 값을 보이는지

## 프론트엔드 통합 테스트

### AI 챗봇에서 테스트
1. 프론트엔드 접속: http://localhost:3000
2. AI 어시스턴트 탭 클릭
3. 테스트 질문:
   - "현재 프로젝트 진행률 알려줘"
   - "OCR 모델 정확도는 어때?"
   - "프로젝트 팀원 명단 보여줘"
   - "이번 스프린트에서 뭐 하고 있어?"

### 기대 결과
- **Before**: 맥락 없는 단편적 답변, 표 데이터 누락
- **After**: 명확한 컨텍스트, 구조화된 정보 제공

## 성능 모니터링

### 응답 시간 측정
```bash
# 검색 성능 테스트
time curl -X POST http://localhost:8000/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "현재 프로젝트 진행률은?", "top_k": 3}'
```

### 로그 모니터링
```bash
# 실시간 로그 확인
docker-compose logs -f llm-service | grep -E "(RAG|search|embedding)"
```

## 문제 해결

### 빌드가 너무 오래 걸릴 경우
```bash
# 빌드 프로세스 확인
docker-compose logs llm-service

# 필요 시 다시 빌드
docker-compose down llm-service
docker-compose build --no-cache llm-service
docker-compose up -d llm-service
```

### init_rag_v2.py 실행 오류
```bash
# 1. 컨테이너 내부 쉘 접속
docker exec -it pms-llm-service bash

# 2. 직접 파이썬 실행
python3 init_rag_v2.py

# 3. 오류 로그 확인
echo $?  # 종료 코드 확인
```

### ChromaDB 연결 실패
```bash
# ChromaDB 상태 확인
docker-compose logs chromadb

# ChromaDB 재시작
docker-compose restart chromadb

# 포트 확인 (8001로 매핑되어 있어야 함)
docker-compose ps chromadb
```

### 임베딩 모델 다운로드 실패
```bash
# 컨테이너 내부에서 수동 다운로드
docker exec pms-llm-service python3 -c "
from sentence_transformers import SentenceTransformer
model = SentenceTransformer('intfloat/multilingual-e5-large')
print('Model downloaded successfully')
"
```

## 성능 최적화 옵션

### 1. 배치 임베딩 (대량 문서 처리 시)
```python
# rag_service_v2.py 수정
embeddings = self.embedding_model.encode(
    texts,
    batch_size=32,  # 배치 크기 조정
    show_progress_bar=True
)
```

### 2. GPU 사용 (가능한 경우)
```python
# document_parser.py 수정
parser = MinerUDocumentParser(device="cuda", use_mock=False)
```

### 3. 청크 크기 조정
```python
# 더 작은 청크 = 더 정확하지만 더 많은 저장공간
chunker = LayoutAwareChunker(max_chunk_size=500, overlap=50)

# 더 큰 청크 = 더 많은 컨텍스트, 적은 청크 수
chunker = LayoutAwareChunker(max_chunk_size=1500, overlap=150)
```

## 다음 개선 작업 (추후)

### 1. 실제 MinerU 모델 통합
```bash
# HuggingFace에서 모델 다운로드
huggingface-cli download opendatalab/MinerU2.5-2509-1.2B

# document_parser.py 수정
use_mock=False  # 실제 모델 사용
```

### 2. 표 전용 검색기 추가
- 표 데이터를 별도 컬렉션에 저장
- 구조화된 쿼리에 특화된 검색

### 3. 하이브리드 검색
- 키워드 검색 + 시맨틱 검색 결합
- BM25 + Dense Retrieval

### 4. 캐싱 레이어 추가
- 자주 검색되는 쿼리 캐싱
- Redis 통합

## 체크리스트

- [ ] Docker 빌드 완료
- [ ] LLM 서비스 정상 시작
- [ ] ChromaDB 연결 확인
- [ ] init_rag_v2.py 실행 성공
- [ ] 검색 API 테스트 완료
- [ ] 프론트엔드 통합 테스트 완료
- [ ] 성능 비교 확인
- [ ] 문서화 검토

## 지원 및 문의

문제가 발생하면 다음을 확인하세요:
1. [UPGRADE_SUMMARY.md](../UPGRADE_SUMMARY.md) - 전체 변경사항
2. [README_RAG_V2.md](README_RAG_V2.md) - 기술 문서
3. Docker 로그: `docker-compose logs llm-service`
4. ChromaDB 로그: `docker-compose logs chromadb`

---

**준비 완료!** Docker 빌드가 완료되면 위 단계를 따라 실행하세요.
