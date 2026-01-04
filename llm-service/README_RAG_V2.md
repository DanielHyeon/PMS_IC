# RAG 시스템 V2 개선사항

## MinerU2.5 기반 Layout-Aware RAG 구현

### 주요 개선사항

#### 1. **구조 인식 문서 파싱 (document_parser.py)**
- 기존: 단순 텍스트 추출 → 512 토큰씩 기계적 분할
- 개선: 문서 구조 이해 → 의미 단위 분할

**문서 블록 타입 인식:**
- 제목 (TITLE): 주요 섹션 헤딩
- 부제목 (HEADING): 하위 섹션
- 문단 (PARAGRAPH): 일반 텍스트
- 표 (TABLE): 구조화된 데이터
- 리스트 (LIST): 항목 나열
- 수식 (FORMULA): 수학/통계 표현

#### 2. **Layout-Aware Chunking**
기존 RAG의 문제점:
```python
# 기존 방식 (rag_service.py)
"프로젝트 진행률: 62%\n목표: 100%"  # 청크 1
"현재 상태: 순조\n\n2. 예산 사용률"   # 청크 2
```
→ 문맥이 끊김, 표와 설명이 분리됨

개선된 방식:
```python
# 새 방식 (rag_service_v2.py)
[CONTEXT] 프로젝트 KPI
[TITLE] 전체 진행률
전체 진행률: 62%
목표: 100% (2024-12-31)
현재 상태: 순조

[TABLE]
스프린트 속도:
Sprint 1: 23 SP
Sprint 2: 28 SP
...
```
→ 의미 단위로 청킹, 컨텍스트 유지

#### 3. **향상된 임베딩 모델**
- 기존: `paraphrase-multilingual-MiniLM-L12-v2`
- 개선: `intfloat/multilingual-e5-large`
  - 더 나은 다국어 성능
  - query/passage 프리픽스 활용
  - 높은 정확도

#### 4. **구조 정보 기반 검색 최적화**

**메타데이터 활용:**
```python
{
  'title': '프로젝트 KPI',
  'has_table': True,
  'has_list': False,
  'is_structured': True,
  'block_types': ['title', 'paragraph', 'table']
}
```

**Relevance Score 계산:**
```python
similarity = 1 - distance
structure_weight = 1.2 if has_table else 1.0
title_weight = 1.1 if title else 1.0
relevance_score = similarity * structure_weight * title_weight
```

→ 표가 포함된 청크에 가중치 부여
→ 제목이 있는 청크 우선순위 증가

### 성능 비교

| 항목 | 기존 RAG | MinerU RAG |
|------|----------|------------|
| 입력 | 텍스트 | 문서 구조 |
| Chunk 기준 | 토큰 수 | 의미 단위 |
| 표/수식 | 손실 가능 | 완전 유지 |
| 환각 | 높음 | 낮음 |
| 구조화 문서 | 취약 | 강력 |

### 사용 방법

#### 1. 컬렉션 초기화 및 데이터 재색인
```bash
# Docker 환경에서 실행
docker exec pms-llm-service python3 init_rag_v2.py
```

#### 2. API 사용 (자동으로 V2 사용)
```python
# app.py가 자동으로 RAGServiceV2를 로드합니다
from rag_service_v2 import RAGServiceV2

rag = RAGServiceV2()

# 문서 추가
rag.add_document({
    'id': 'doc_1',
    'content': '프로젝트 내용...',
    'metadata': {'type': 'project', 'category': 'kpi'}
})

# 검색
results = rag.search("현재 진행률은?", top_k=3)
```

#### 3. 검색 결과 예시
```python
{
  'content': '[CONTEXT] 프로젝트 KPI\n[TITLE] 전체 진행률\n...',
  'metadata': {
    'title': '프로젝트 KPI',
    'has_table': True,
    'is_structured': True
  },
  'distance': 0.15,
  'relevance_score': 0.935  # 높은 점수
}
```

### 파일 구조

```
llm-service/
├── document_parser.py      # MinerU 기반 파서
├── rag_service_v2.py       # 개선된 RAG 서비스
├── init_rag_v2.py          # 초기화 스크립트
├── app.py                  # (수정) V2 사용
└── requirements.txt        # (업데이트) 의존성 추가
```

### 향후 개선 계획

1. **실제 MinerU2.5 모델 통합**
   - 현재: 휴리스틱 기반 파싱 (시뮬레이션)
   - 목표: HuggingFace에서 실제 MinerU 모델 로드

2. **이미지/PDF 직접 처리**
   - 현재: 텍스트 입력
   - 목표: PDF 이미지를 직접 MinerU에 입력

3. **표 전용 Retriever**
   - 표 데이터는 별도 벡터 공간에 저장
   - 구조화된 쿼리에 최적화

4. **Multi-Vector 아키텍처**
   - 텍스트, 표, 수식 각각 다른 임베딩 모델 사용

### 기술 스택

- **Document Parsing**: MinerU2.5-2509-1.2B (시뮬레이션)
- **Embedding**: intfloat/multilingual-e5-large
- **Vector DB**: ChromaDB
- **Framework**: Python 3.11, Flask

### 참고 자료

PDF 문서: "MinerU2.5-2509-1.2B를 이용한 RAG 구현 방법.pdf"
- MinerU2.5 성능 분석
- Layout-Aware Chunking 원리
- RAG vs LAG 비교
