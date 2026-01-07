# Neo4j GraphRAG 구현 가이드

## 📋 개요

Qdrant에서 Neo4j로 마이그레이션하여 **벡터 검색 + 그래프 관계**를 단일 데이터베이스에서 처리하는 GraphRAG 시스템입니다.

### 주요 개선 사항

1. **단일 데이터베이스 아키텍처**
   - Neo4j에서 벡터 인덱스 + 그래프 관계 통합 관리
   - Helix-DB 대비 안정적이고 성숙한 솔루션
   - 데이터 동기화 불필요

2. **GraphRAG 검색 전략**
   - 벡터 유사도 검색 (HNSW 인덱스)
   - 순차 컨텍스트 확장 (NEXT_CHUNK 관계)
   - 카테고리별 관련 문서 추천
   - 문서 구조 정보 활용 (표, 리스트 등)

3. **Neo4j 5.20+ 네이티브 벡터 지원**
   - Apache Lucene HNSW 알고리즘
   - 코사인 유사도 검색
   - 1024차원 임베딩 (multilingual-e5-large)

---

## 🏗️ 아키텍처

```
┌─────────────────┐
│   사용자 질의    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  RAGServiceNeo4j            │
│  (rag_service_neo4j.py)     │
└──────┬──────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│         Neo4j 5.20           │
│  - Vector Index (HNSW)       │
│  - Graph Relationships       │
│  - Cypher Query Engine       │
└──────────────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│    통합 검색 결과             │
│ (벡터 유사도 + 그래프 컨텍스트)│
└──────────────────────────────┘
```

---

## 📦 그래프 스키마

### 노드 타입

#### Document
```cypher
(:Document {
  doc_id: String (UNIQUE),
  title: String,
  content: String,
  file_type: String,
  file_path: String,
  created_at: String
})
```

#### Chunk
```cypher
(:Chunk {
  chunk_id: String (UNIQUE),
  content: String,
  chunk_index: Integer,
  title: String,
  doc_id: String,
  embedding: List<Float>[1024],  // 벡터 인덱스
  structure_type: String,  // "heading", "paragraph", "table", "list"
  has_table: Boolean,
  has_list: Boolean,
  section_title: String,
  page_number: Integer
})
```

#### Category
```cypher
(:Category {
  name: String (UNIQUE)
})
```

### 관계 타입

```cypher
(Document)-[:HAS_CHUNK]->(Chunk)
(Document)-[:BELONGS_TO]->(Category)
(Chunk)-[:NEXT_CHUNK]->(Chunk)  // 순차 관계
```

---

## 🚀 시작하기

### 1. Neo4j 컨테이너 시작

```bash
# 전체 서비스 시작
docker compose up -d

# Neo4j만 시작
docker compose up -d neo4j

# 상태 확인
docker compose ps
curl http://localhost:7474  # Browser UI
```

**✅ Neo4j Browser 접속**:
- URL: http://localhost:7474
- Username: `neo4j`
- Password: `pmspassword123`

### 2. 환경 변수 설정

`.env` 파일 또는 `docker-compose.yml`에서 이미 설정됨:

```bash
VECTOR_DB=neo4j
NEO4J_URI=bolt://neo4j:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=pmspassword123
USE_GRAPH_RAG=true
```

### 3. Qdrant → Neo4j 마이그레이션

기존 Qdrant 데이터를 Neo4j로 이전:

```bash
# LLM 서비스 컨테이너에 접속
docker exec -it pms-llm-service bash

# Qdrant 프로필로 Qdrant 시작 (마이그레이션용)
docker compose --profile qdrant up -d qdrant

# 마이그레이션 실행
python migrate_qdrant_to_neo4j.py --clear-neo4j

# 옵션:
# --collection <name>      : Qdrant 컬렉션 이름 (기본: pms_documents_v2)
# --batch-size <n>         : 배치 크기 (기본: 10)
# --clear-neo4j            : Neo4j 기존 데이터 삭제
```

**마이그레이션 과정**:
1. Qdrant에서 모든 청크 가져오기
2. 문서별로 그룹화
3. Neo4j에 Document → Chunk → NEXT_CHUNK 구조로 추가
4. 임베딩 재생성 및 벡터 인덱스 등록

### 4. 새 문서 추가

**Python API 사용**:

```python
from rag_service_neo4j import RAGServiceNeo4j

# 서비스 초기화
rag_service = RAGServiceNeo4j()

# 문서 추가
document = {
    "id": "doc_001",
    "content": "보험 약관 내용...",
    "metadata": {
        "title": "보험 약관",
        "category": "보험",
        "file_type": "pdf",
        "file_path": "/data/insurance.pdf",
        "created_at": "2026-01-07"
    }
}

success = rag_service.add_document(document)
print(f"Added: {success}")
```

**REST API 사용**:

```bash
curl -X POST http://localhost:8000/api/documents \
  -H "Content-Type: application/json" \
  -d '{
    "documents": [{
      "id": "doc_001",
      "content": "보험 약관 내용...",
      "metadata": {
        "title": "보험 약관",
        "category": "보험"
      }
    }]
  }'
```

---

## 🔍 검색 전략

### 1. GraphRAG 검색 (권장)

벡터 검색 + 순차 컨텍스트 + 관련 문서:

```python
results = rag_service.search(
    query="보험 청구 절차",
    top_k=5,
    use_graph_expansion=True  # GraphRAG 활성화
)

for result in results:
    print(f"Content: {result['content']}")
    print(f"Score: {result['relevance_score']}")

    # 순차 컨텍스트
    if 'context' in result:
        if 'prev' in result['context']:
            print(f"Previous: {result['context']['prev'][:100]}...")
        if 'next' in result['context']:
            print(f"Next: {result['context']['next'][:100]}...")

        # 같은 카테고리의 관련 문서
        if 'related_docs' in result['context']:
            for doc in result['context']['related_docs']:
                print(f"Related: {doc['title']}")
```

### 2. 단순 벡터 검색

그래프 확장 없이 빠른 검색:

```python
results = rag_service.search(
    query="보험 청구 절차",
    top_k=5,
    use_graph_expansion=False  # 벡터 검색만
)
```

### 3. 카테고리 필터 검색

```python
results = rag_service.search(
    query="청구 절차",
    top_k=5,
    filter_metadata={"category": "보험"}
)
```

### 4. Cypher 직접 쿼리

Neo4j Browser에서 직접 쿼리:

```cypher
// 카테고리별 문서 수
MATCH (d:Document)-[:BELONGS_TO]->(c:Category)
RETURN c.name AS category, count(d) AS doc_count
ORDER BY doc_count DESC

// 특정 문서의 청크들과 순차 관계
MATCH (d:Document {doc_id: "doc_001"})-[:HAS_CHUNK]->(chunk:Chunk)
OPTIONAL MATCH (chunk)-[:NEXT_CHUNK]->(next:Chunk)
RETURN chunk.chunk_index, chunk.content, next.content AS next_content
ORDER BY chunk.chunk_index

// 벡터 검색 + 그래프 확장
CALL db.index.vector.queryNodes('chunk_embeddings', 5, $embedding)
YIELD node AS c, score
MATCH (d:Document)-[:HAS_CHUNK]->(c)
OPTIONAL MATCH (c)-[:NEXT_CHUNK]->(next:Chunk)
RETURN c.content, score, next.content AS next_context
ORDER BY score DESC
```

---

## 📊 성능 벤치마크

### 벤치마크 실행

```bash
docker exec -it pms-llm-service bash
python benchmark_rag_services.py --top-k 5
```

**비교 항목**:
- ✅ 레이턴시 (평균, 중앙값, 최소, 최대)
- ✅ 결과 품질 (평균 관련성 점수)
- ✅ 결과 개수
- ✅ 오류 발생률

**예상 결과**:

| 항목 | Qdrant | Neo4j (GraphRAG) |
|------|--------|------------------|
| 평균 레이턴시 | 50-100ms | 100-200ms |
| 관련성 점수 | 0.75 | 0.80-0.85 |
| 컨텍스트 품질 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 안정성 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 🛠️ 유지보수

### 통계 확인

```python
stats = rag_service.get_collection_stats()
print(stats)
```

**출력 예시**:
```json
{
  "vector_db": "neo4j",
  "graph_db": "neo4j",
  "status": "available",
  "total_documents": 150,
  "total_chunks": 2345,
  "vector_size": 1024,
  "categories": [
    {"category": "보험", "doc_count": 80},
    {"category": "프로젝트", "doc_count": 70}
  ],
  "graph_rag_enabled": true
}
```

### Neo4j 헬스체크

```bash
# REST API
curl http://localhost:7474

# Cypher
docker exec -it pms-neo4j cypher-shell -u neo4j -p pmspassword123
> CALL dbms.components() YIELD name, versions, edition;
```

### 문제 해결

**1. Neo4j 연결 실패**
```bash
docker compose restart neo4j
docker compose logs -f neo4j
```

**2. 벡터 인덱스 재생성**
```cypher
DROP INDEX chunk_embeddings IF EXISTS;
CREATE VECTOR INDEX chunk_embeddings
FOR (c:Chunk)
ON c.embedding
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1024,
    `vector.similarity_function`: 'cosine'
  }
}
```

**3. 데이터 초기화**
```cypher
MATCH (n) DETACH DELETE n;
```

---

## 📈 성능 최적화

### 1. 메모리 설정

`docker-compose.yml`에서 Neo4j 메모리 조정:

```yaml
environment:
  - NEO4J_dbms_memory_heap_initial__size=512m
  - NEO4J_dbms_memory_heap_max__size=4G      # 대용량 데이터
  - NEO4J_dbms_memory_pagecache_size=2G
```

### 2. 인덱스 최적화

```cypher
// 복합 인덱스 생성
CREATE INDEX chunk_doc_idx IF NOT EXISTS FOR (c:Chunk) ON (c.doc_id);
CREATE INDEX chunk_structure_idx IF NOT EXISTS FOR (c:Chunk) ON (c.structure_type);
```

### 3. 쿼리 프로파일링

```cypher
PROFILE
CALL db.index.vector.queryNodes('chunk_embeddings', 5, $embedding)
YIELD node AS c, score
MATCH (d:Document)-[:HAS_CHUNK]->(c)
RETURN c.content, score
```

---

## 🎯 Qdrant vs Neo4j 비교

### 언제 Neo4j를 사용할까?

✅ **Neo4j가 적합한 경우**:
- 문서 간 관계가 중요한 경우
- 순차 컨텍스트 확장이 필요한 경우
- 단일 데이터베이스로 통합 관리하고 싶은 경우
- 중소 규모 데이터 (<10M 벡터)
- GraphRAG의 고품질 답변이 필요한 경우

✅ **Qdrant가 적합한 경우**:
- 순수 벡터 검색만 필요한 경우
- 극한 성능이 필요한 경우 (100ms 이하)
- 대규모 벡터 데이터 (10M+ 벡터)
- 그래프 관계가 필요 없는 경우

### 마이그레이션 체크리스트

- [ ] Neo4j 컨테이너 정상 실행 확인
- [ ] 환경 변수 설정 (`VECTOR_DB=neo4j`)
- [ ] Qdrant 데이터 백업
- [ ] 마이그레이션 스크립트 실행
- [ ] 검색 결과 품질 테스트
- [ ] 성능 벤치마크 실행
- [ ] Helix-DB 컨테이너 제거
- [ ] 프로덕션 배포

---

## 📚 참고 자료

- [Neo4j Vector Search Guide](https://neo4j.com/docs/cypher-manual/current/indexes/semantic-indexes/vector-indexes/)
- [Reference Repository](https://github.com/gongwon-nayeon/graphrag-tools-retriever)
- [Neo4j Python Driver](https://neo4j.com/docs/python-manual/current/)

---

## 💡 다음 단계

1. **고급 GraphRAG 패턴**
   - Text2Cypher: 자연어 → Cypher 쿼리 변환
   - VectorCypher: 벡터 + Cypher 복합 검색
   - ToolsRetriever: 여러 검색 전략 통합

2. **엔티티 추출 및 관계 생성**
   - NER로 개념 자동 추출
   - 문서 간 참조 관계 학습
   - 프로젝트/업무 엔티티 연동

3. **그래프 시각화**
   - Neo4j Browser로 관계 탐색
   - D3.js로 인터랙티브 그래프

4. **프로덕션 최적화**
   - Neo4j Enterprise 기능 활용
   - 읽기 복제본 설정
   - 모니터링 및 알림

---

## ⚠️ 주의사항

1. **Helix-DB는 비활성화됨**
   - `profiles: [deprecated]`로 설정됨
   - 기존 Helix 코드는 유지되지만 사용되지 않음

2. **임베딩 재생성**
   - Qdrant에서 마이그레이션 시 임베딩 재생성됨
   - GPU 사용 시 빠르게 처리됨

3. **Neo4j Community vs Enterprise**
   - Community: 무료, 단일 인스턴스
   - Enterprise: 클러스터링, 고급 기능, 유료

---

## 📞 지원

문제 발생 시:
1. GitHub Issues: [프로젝트 이슈 트래커]
2. 로그 확인: `docker compose logs neo4j`
3. Neo4j Browser에서 직접 쿼리 테스트

---

**구현 완료일**: 2026-01-07
**작성자**: Claude Code with Neo4j GraphRAG
