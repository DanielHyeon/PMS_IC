# MinerU2.5 실제 모델 통합 가이드

## 개요

MinerU2.5-2509-1.2B GGUF 모델을 llama-cpp-python으로 로드하여 실제 문서 구조 파싱을 수행합니다.

## 모델 정보

- **모델명**: MinerU2.5-2509-1.2B
- **파일명**: `MinerU2.5-2509-1.2B.i1-Q6_K.gguf`
- **위치**: `/wp/PMS_IC/models/`
- **크기**: 483MB
- **양자화**: Q6_K (6-bit quantization)

## 주요 기능

### 1. 실제 모델 기반 문서 파싱

기존 휴리스틱 방식 대신 MinerU 모델이 직접 문서 구조를 분석합니다:

```python
from document_parser import MinerUDocumentParser

# 실제 모델 사용
parser = MinerUDocumentParser(use_mock=False, device="cpu")

# 문서 파싱
blocks = parser.parse_document(text)
```

### 2. 구조 요소 인식

MinerU 모델이 다음을 자동으로 식별합니다:

- **TITLE**: 주요 섹션 제목
- **HEADING**: 부제목
- **PARAGRAPH**: 일반 문단
- **TABLE**: 표 데이터
- **LIST**: 리스트 항목
- **FORMULA**: 수식

### 3. JSON 형식 출력

모델은 구조화된 JSON 형식으로 결과를 반환합니다:

```json
[
  {
    "type": "TITLE",
    "content": "프로젝트 KPI",
    "line": 1,
    "confidence": 0.95
  },
  {
    "type": "TABLE",
    "content": "단계    진행률\n1단계   100%\n2단계   85%",
    "line": 10,
    "confidence": 0.92
  }
]
```

## 사용 방법

### 1. 모델 테스트

```bash
# Docker 환경에서 실행
docker exec pms-llm-service python3 test_mineru_model.py
```

**예상 출력:**
```
================================================================================
MinerU2.5 모델 테스트 시작
================================================================================

1️⃣  MinerU 모델 로드 중...
Loading MinerU2.5 model from /app/models/MinerU2.5-2509-1.2B.i1-Q6_K.gguf...
✅ MinerU2.5 model loaded successfully!
   Model: MinerU2.5-2509-1.2B.i1-Q6_K.gguf
   Device: cpu

2️⃣  샘플 문서 파싱 테스트...
   문서 길이: 425 문자

✅ 파싱 완료: 12개 블록 추출

3️⃣  파싱 결과:
--------------------------------------------------------------------------------

블록 1:
  타입: title
  내용: 프로젝트명: 보험 Claim Automation AI 시스템 구축...
  메타데이터: {'line': 1, 'source': 'mineru_model', 'confidence': 0.95}

...

4️⃣  블록 타입 통계:
--------------------------------------------------------------------------------
  title          :   3개
  paragraph      :   5개
  table          :   1개
  list           :   3개

================================================================================
✅ 모든 테스트 성공!
================================================================================
```

### 2. RAG 시스템 초기화 (MinerU 모델 사용)

```bash
# MinerU 모델로 RAG 재초기화
docker exec pms-llm-service python3 init_rag_with_mineru.py
```

### 3. 환경변수 제어

**모델 사용:**
```bash
export USE_MINERU_MODEL=true  # 실제 MinerU 모델 사용 (기본값)
```

**휴리스틱 사용:**
```bash
export USE_MINERU_MODEL=false  # 빠른 휴리스틱 파싱
```

## 성능 비교

### 휴리스틱 vs MinerU 모델

| 항목 | 휴리스틱 | MinerU 모델 |
|------|----------|-------------|
| **정확도** | 중간 | 높음 |
| **속도** | 빠름 (~10ms) | 느림 (~500ms) |
| **표 인식** | 제한적 | 정확 |
| **구조 이해** | 규칙 기반 | AI 기반 |
| **한계** | 복잡한 문서 취약 | 모든 문서 대응 |

### 처리 시간

- **휴리스틱**: 문서당 ~10ms
- **MinerU 모델**: 문서당 ~500ms (CPU 기준)
- **권장**: 배치 초기화 시 사용, 실시간은 휴리스틱 고려

## 프롬프트 엔지니어링

MinerU 모델은 다음 프롬프트로 문서 구조를 분석합니다:

```python
prompt = f"""You are a document structure analyzer. Analyze the following document and identify structural elements.

For each element, identify its type and content:
- TITLE: Main section headings
- HEADING: Sub-section headings
- PARAGRAPH: Regular text paragraphs
- TABLE: Tabular data or structured information
- LIST: Bulleted or numbered lists
- FORMULA: Mathematical formulas or equations

Return your analysis in JSON format as an array of blocks:
[
  {{"type": "TITLE", "content": "...", "line": 1}},
  {{"type": "PARAGRAPH", "content": "...", "line": 5}},
  {{"type": "TABLE", "content": "...", "line": 10}}
]

Document to analyze:
---
{text[:3000]}
---

Structural analysis (JSON):"""
```

### 프롬프트 개선 팁

1. **온도 낮추기**: `temperature=0.1` - 일관성 증가
2. **토큰 제한**: `max_tokens=2048` - 긴 문서 대응
3. **Stop 시퀀스**: JSON 종료 시점 명확히

## 모델 로딩 옵션

### CPU 사용 (기본)
```python
parser = MinerUDocumentParser(
    use_mock=False,
    device="cpu"
)
```

### GPU 사용 (가능한 경우)
```python
parser = MinerUDocumentParser(
    use_mock=False,
    device="cuda",
    model_path="/path/to/model.gguf"
)
```

### 커스텀 모델 경로
```python
parser = MinerUDocumentParser(
    use_mock=False,
    model_path="/custom/path/MinerU2.5-2509-1.2B.i1-Q6_K.gguf"
)
```

## 문제 해결

### 1. 모델 로드 실패

**증상:**
```
ERROR - Model file not found: /app/models/MinerU2.5-2509-1.2B.i1-Q6_K.gguf
WARNING - Falling back to heuristic-based parsing.
```

**해결:**
```bash
# 모델 파일 확인
ls -lh /wp/PMS_IC/models/MinerU2.5-2509-1.2B.i1-Q6_K.gguf

# Docker 볼륨 마운트 확인
docker inspect pms-llm-service | grep -A 5 Mounts
```

### 2. 메모리 부족

**증상:**
```
ERROR - Failed to load MinerU model: Cannot allocate memory
```

**해결:**
```python
# 더 작은 컨텍스트 사용
parser = MinerUDocumentParser(
    use_mock=False,
    device="cpu"
)
# document_parser.py 수정: n_ctx=2048 (기본값 4096에서 감소)
```

### 3. JSON 파싱 오류

**증상:**
```
WARNING - Could not parse JSON from MinerU response. Using heuristic parsing.
```

**원인**: 모델 응답이 JSON 형식이 아님

**해결**: 자동으로 휴리스틱 파싱으로 폴백됨 (정상 동작)

### 4. 처리 속도 느림

**해결 방법:**

1. **배치 처리 최적화**:
   ```python
   # 여러 문서를 한번에 처리
   documents = [doc1, doc2, doc3]
   for doc in documents:
       blocks = parser.parse_document(doc)
   ```

2. **캐싱 활용**:
   ```python
   # 이미 파싱한 문서는 캐시 사용
   cached_results = {}
   doc_hash = hash(text)
   if doc_hash in cached_results:
       return cached_results[doc_hash]
   ```

3. **휴리스틱 혼용**:
   ```python
   # 간단한 문서는 휴리스틱, 복잡한 문서만 모델 사용
   if len(text) < 1000:
       parser = MinerUDocumentParser(use_mock=True)
   else:
       parser = MinerUDocumentParser(use_mock=False)
   ```

## 향후 개선

### 1. 이미지 입력 지원

현재는 텍스트만 지원하지만, MinerU는 원래 이미지 분석에 특화:

```python
# 향후 구현
from PIL import Image
image = Image.open("document.png")
blocks = parser.parse_image(image)
```

### 2. PDF 직접 처리

```python
# 향후 구현
blocks = parser.parse_pdf("document.pdf", page=1)
```

### 3. 다중 모델 앙상블

```python
# 휴리스틱 + 모델 결과 병합
heuristic_blocks = parser_heuristic.parse_document(text)
model_blocks = parser_model.parse_document(text)
final_blocks = merge_blocks(heuristic_blocks, model_blocks)
```

## 참고 자료

- **모델 소스**: OpenDataLab MinerU2.5-2509-1.2B
- **llama-cpp-python**: https://github.com/abetlen/llama-cpp-python
- **GGUF 형식**: https://github.com/ggerganov/ggml

---

**문서 업데이트**: 2026-01-05
**버전**: MinerU Model Integration v1.0
