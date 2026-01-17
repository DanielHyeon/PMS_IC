# Gemma 3 12B Q5 안정성 개선 가이드

**작성일**: 2026-01-17
**버전**: 1.0
**목적**: Gemma 3 12B Q5 모델의 "Unable to answer" 오류 및 불안정성 해결

---

## 개요

이 문서는 PMS-IC 프로젝트에서 Gemma 3 12B Q5_K_M GGUF 모델의 응답 안정성을 향상시키기 위해 구현된 **4가지 핵심 개선사항**을 설명합니다.

### 문제점
- 간헐적인 "Unable to answer" 응답
- 불완전하거나 절단된 응답
- 타임아웃으로 인한 응답 실패
- 반복적인 응답 생성

### 해결책
1. **응답 검증 시스템** - 실패 패턴 자동 감지
2. **재시도 한계 증가** - MAX_QUERY_RETRIES: 2 → 4
3. **모니터링/로깅** - 실패 유형별 추적 및 분석
4. **타임아웃 + 재시도** - 지수 백오프를 포함한 재시도 로직

---

## 1. 응답 검증 시스템

### 파일 위치
[`llm-service/response_validator.py`](../llm-service/response_validator.py)

### 기능

응답의 **7가지 실패 유형**을 자동으로 감지합니다:

| 유형 | 설명 | 예시 |
|------|------|------|
| `UNABLE_TO_ANSWER` | 답변 불가 패턴 | "Unable to answer", "답변할 수 없습니다" |
| `INCOMPLETE_RESPONSE` | 불완전한 응답 | "프로젝트는..." (끝에 "...") |
| `EMPTY_RESPONSE` | 빈 응답 | "" (길이 0) |
| `TIMEOUT_CUTOFF` | 타임아웃 절단 | "프로젝트는 중요합니다," (마침표 없음) |
| `REPETITIVE_RESPONSE` | 반복적 응답 | "좋습니다 좋습니다 좋습니다" |
| `MALFORMED_RESPONSE` | 형식 오류 | 코드 블록만 있는 응답 |
| `NONE` | 유효한 응답 | (정상) |

### 사용법

```python
from response_validator import ResponseValidator, ResponseFailureType

validator = ResponseValidator()

# 응답 검증
result = validator.validate("Unable to answer this question")

print(result.is_valid)           # False
print(result.failure_type)       # ResponseFailureType.UNABLE_TO_ANSWER
print(result.reason)             # "응답이 답변 불가 패턴 포함"
print(result.suggested_retry)    # True
```

### 통합 위치

[`llm-service/chat_workflow.py`](../llm-service/chat_workflow.py) (Line 521-547)에서 자동으로 실행:

```python
if self.response_validator:
    validation_result = self.response_validator.validate(reply, message)

    if not validation_result.is_valid:
        # 재시도 로직 실행
        if validation_result.suggested_retry and state.get("retry_count", 0) < RAG.MAX_QUERY_RETRIES:
            # 쿼리 개선 후 재시도
            refined_message = self._refine_message_by_failure(message, validation_result.failure_type)
            state = self.rag_search_node(state)
            return state
```

---

## 2. 재시도 한계 증가 및 쿼리 개선

### 설정 변경

[`llm-service/config/constants.py`](../llm-service/config/constants.py)

```python
class RAGConfig:
    MAX_QUERY_RETRIES: int = 4  # 이전: 2
    LLM_RESPONSE_TIMEOUT: int = 30000  # 30초
    QUERY_TIMEOUT: int = 60000  # 1분
```

### 쿼리 개선 전략

실패 유형에 따라 자동으로 쿼리를 개선합니다:

| 실패 유형 | 개선 전략 |
|----------|---------|
| `UNABLE_TO_ANSWER` | 핵심 키워드 추출 → 브로드닝 |
| `INCOMPLETE_RESPONSE` | "상세히 설명해주세요" 추가 |
| `REPETITIVE_RESPONSE` | "다른 관점에서" 추가 |
| `TIMEOUT_CUTOFF` | 쿼리 단축 |
| `MALFORMED_RESPONSE` | "이 질문에 대해 설명해주세요" 재구성 |
| `EMPTY_RESPONSE` | "구체적으로" 추가 |

### 구현 위치

[`llm-service/chat_workflow.py`](../llm-service/chat_workflow.py) (Line 911-985)

```python
def _refine_message_by_failure(self, original_message: str, failure_type) -> str:
    """응답 실패 유형에 따른 쿼리 개선"""
    strategies = {
        ResponseFailureType.UNABLE_TO_ANSWER: {
            "description": "답변 불가 - 핵심 키워드 추출",
            "action": lambda msg: self._extract_keywords_refined(msg)
        },
        # ... 다른 전략들
    }
```

---

## 3. 모니터링 및 로깅 시스템

### 파일 위치
[`llm-service/response_monitoring.py`](../llm-service/response_monitoring.py)

### 핵심 기능

#### ResponseMonitor (실시간 메트릭)

```python
from response_monitoring import get_monitor, ResponseMetrics
from datetime import datetime

monitor = get_monitor()

# 메트릭 기록
metric = ResponseMetrics(
    timestamp=datetime.now(),
    response_id="req-001",
    query="프로젝트 진척",
    response_length=150,
    is_valid=True,
    failure_type=None,
    rag_doc_count=3,
    confidence_score=0.85
)
monitor.record_metric(metric)

# 통계 조회
stats = monitor.get_stats()
print(f"Success Rate: {stats['success_rate']}%")
print(f"Failure Distribution: {stats['failure_distribution']}")
```

#### 위험한 패턴 감지

```python
# 실패율이 threshold 이상인 패턴 감지
patterns = monitor.get_critical_patterns(threshold=0.1)  # 10% 이상

for pattern in patterns:
    print(f"Pattern: {pattern['failure_type']}")
    print(f"Rate: {pattern['rate']}%")
    print(f"Recommendation: {pattern['recommendation']}")
```

### REST API 엔드포인트

[`llm-service/app.py`](../llm-service/app.py) (Line 892-981)

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/monitoring/metrics` | GET | 전체 메트릭 조회 (또는 특정 실패 유형) |
| `/api/monitoring/critical-patterns` | GET | 위험한 패턴 감지 |
| `/api/monitoring/logs` | GET | 모니터링 로그 조회 |
| `/api/monitoring/export` | POST | 메트릭 JSON 내보내기 |
| `/api/monitoring/reset` | POST | 모니터 초기화 |

#### 사용 예시

```bash
# 전체 메트릭 조회
curl http://localhost:8000/api/monitoring/metrics

# 특정 실패 유형 조회
curl "http://localhost:8000/api/monitoring/metrics?failure_type=unable_to_answer"

# 위험한 패턴 감지 (실패율 >= 10%)
curl "http://localhost:8000/api/monitoring/critical-patterns?threshold=0.1"

# 최근 100줄 로그 조회
curl "http://localhost:8000/api/monitoring/logs?lines=100"

# 메트릭 내보내기
curl -X POST http://localhost:8000/api/monitoring/export \
  -H "Content-Type: application/json" \
  -d '{"export_path": "/tmp/metrics.json"}'
```

---

## 4. 타임아웃 + 재시도 로직

### 파일 위치
[`llm-service/timeout_retry_handler.py`](../llm-service/timeout_retry_handler.py)

### 설정

```python
from timeout_retry_handler import TimeoutRetryConfig

config = TimeoutRetryConfig(
    timeout_seconds=30,          # 30초 타임아웃
    max_retries=3,               # 최대 3회 재시도
    backoff_factor=1.5,          # 지수 백오프 (1.5배씩 증가)
    initial_delay_ms=100,        # 첫 재시도: 100ms
    max_delay_ms=5000,           # 최대 대기: 5초
    jitter_enabled=True          # 지터 추가 (부하 분산)
)
```

### 재시도 전략

#### 지수 백오프 + 지터

```
시도 1: 즉시
시도 2: 100ms + 지터
시도 3: 150ms + 지터  (100 * 1.5)
시도 4: 225ms + 지터  (150 * 1.5)
```

지터(Jitter)는 동시 재시도로 인한 부하를 방지합니다.

### 사용법

#### 방법 1: 데코레이터

```python
from timeout_retry_handler import with_timeout_retry, TimeoutRetryConfig

@with_timeout_retry(TimeoutRetryConfig(timeout_seconds=30, max_retries=3))
def my_llm_inference():
    return llm_model.generate("프로젝트 진척")
```

#### 방법 2: 컨텍스트 매니저

```python
from timeout_retry_handler import timeout_context

with timeout_context(30):
    response = llm_model.generate("프로젝트 진척")
```

#### 방법 3: 결합 핸들러

```python
from timeout_retry_handler import CombinedTimeoutRetry, DEFAULT_GEMMA3_TIMEOUT_RETRY_CONFIG

handler = CombinedTimeoutRetry(DEFAULT_GEMMA3_TIMEOUT_RETRY_CONFIG)
result = handler.execute(llm_inference)
```

### 적응형 타임아웃

실행 이력을 바탕으로 타임아웃을 자동으로 조정합니다:

```python
from timeout_retry_handler import AdaptiveTimeoutRetry

adaptive_handler = AdaptiveTimeoutRetry(initial_config)

# 타임아웃을 적응적으로 조정하면서 실행
result = adaptive_handler.execute_with_adaptation(llm_inference)
```

### LLM 통합

[`llm-service/chat_workflow.py`](../llm-service/chat_workflow.py) (Line 511-548)

```python
def llm_inference():
    response = self.llm(
        prompt,
        max_tokens=LLM.MAX_TOKENS,
        # ... 다른 파라미터
    )
    return response["choices"][0]["text"].strip()

# 타임아웃 + 재시도 핸들러 적용
timeout_retry_handler = CombinedTimeoutRetry(DEFAULT_GEMMA3_TIMEOUT_RETRY_CONFIG)

try:
    reply = timeout_retry_handler.execute(
        llm_inference,
        on_retry_callback=on_retry_callback
    )
except TimeoutException as te:
    logger.error(f"LLM inference timeout after all retries: {te}")
    reply = "죄송합니다. 응답 생성 중 타임아웃이 발생했습니다."
```

---

## 5. 테스트 케이스

### 파일 위치
- [Comprehensive Tests](../llm-service/test_gemma3_stability.py) (pytest 기반)
- [Simple Tests](../llm-service/test_gemma3_stability_simple.py) (스탠드얼론)

### 테스트 실행

```bash
# 스탠드얼론 테스트 (의존성 최소)
cd /home/daniel/projects/pms-ic/llm-service
python3 test_gemma3_stability_simple.py
```

### 테스트 커버리지

✅ **응답 검증**
- "Unable to answer" 패턴 감지
- 불완전한 응답 감지
- 빈 응답 감지
- 유효한 응답 검증
- 반복적 응답 감지
- 타임아웃 절단 감지

✅ **재시도 로직**
- 백오프 딜레이 계산
- 최대 재시도 한계
- 재시도 불가능 예외 처리
- 성공적인 재시도 실행

✅ **모니터링**
- 메트릭 기록
- 통계 조회
- 위험한 패턴 감지
- 적응형 타임아웃 조정

---

## 6. 배포 및 구성

### Docker 환경 변수

```yaml
# docker-compose.yml
llm-service:
  environment:
    MODEL_PATH: /app/models/google.gemma-3-12b-pt.Q5_K_M.gguf
    MAX_TOKENS: 256
    TEMPERATURE: 0.7
    TOP_P: 0.9
    LLM_N_GPU_LAYERS: 50  # Gemma 3에 최적화
    LLM_N_CTX: 4096       # 컨텍스트 윈도우
    LLM_N_THREADS: 6      # 병렬 스레드
```

### 성능 최적화

**권장 설정** (Gemma 3 12B Q5)

| 설정 | 값 | 설명 |
|------|-----|------|
| GPU VRAM | 12GB+ | 최소 요구사항 |
| Context Window | 4096 tokens | 긴 컨텍스트 처리 가능 |
| GPU Layers | 50+ | 대부분을 GPU에서 처리 |
| Batch Size | 1 | 안정성 우선 |
| MAX_TOKENS | 256-512 | 생성 길이 제어 |

---

## 7. 문제 해결 (Troubleshooting)

### "Unable to answer" 계속 발생

**확인 사항**
1. RAG 문서가 적절히 로드되었는지 확인
2. 쿼리 개선 전략이 작동하는지 로그 확인
3. 재시도 한계(MAX_QUERY_RETRIES) 증가 여부 확인

**조치**
```python
# 모니터링 로그에서 패턴 확인
curl "http://localhost:8000/api/monitoring/metrics?failure_type=unable_to_answer"

# 위험한 패턴 감지
curl "http://localhost:8000/api/monitoring/critical-patterns?threshold=0.1"
```

### 타임아웃 오류 빈번

**원인**: 복잡한 쿼리 또는 GPU 메모리 부족

**해결책**
```python
# 타임아웃 증가
config = TimeoutRetryConfig(timeout_seconds=60)

# 또는 GPU 메모리 확인
# nvidia-smi

# 또는 배치 크기 감소
export LLM_N_THREADS=4
```

### 메모리 누수

**확인**
```bash
# 주기적인 모니터 초기화
curl -X POST http://localhost:8000/api/monitoring/reset
```

---

## 8. 성능 메트릭

### 기대 효과

| 항목 | 이전 | 이후 | 개선율 |
|------|------|------|--------|
| 성공률 | ~70% | ~95%+ | +25% |
| 평균 응답 시간 | ~5s | ~3-4s | -20% |
| "Unable to answer" 빈도 | 5-10% | <1% | -80% |
| 타임아웃 오류 | 2-3% | <0.5% | -75% |

### 모니터링 예시

```bash
# 통계 조회
curl http://localhost:8000/api/monitoring/metrics | jq

# 출력:
{
  "window_hours": 24,
  "total_requests": 1500,
  "valid_responses": 1425,
  "invalid_responses": 75,
  "success_rate": 95.0,
  "failure_distribution": {
    "unable_to_answer": 25,
    "timeout_cutoff": 15,
    "incomplete_response": 35
  }
}
```

---

## 9. 참고 자료

- [MinerU 최적화 방안](./MinerU_최적화_방안.md)
- [응답 검증 소스](../llm-service/response_validator.py)
- [모니터링 시스템 소스](../llm-service/response_monitoring.py)
- [타임아웃 + 재시도 소스](../llm-service/timeout_retry_handler.py)
- [테스트 케이스](../llm-service/test_gemma3_stability_simple.py)

---

## 10. 체크리스트

### 배포 전
- [ ] 모든 테스트 통과 (`test_gemma3_stability_simple.py`)
- [ ] 응답 검증기 통합 확인
- [ ] 모니터링 엔드포인트 작동 확인
- [ ] Docker 환경 변수 설정 확인

### 배포 후
- [ ] 모니터링 대시보드 접근 확인 (`/api/monitoring/metrics`)
- [ ] 초기 메트릭 기록 확인
- [ ] 응답 품질 모니터링
- [ ] 위험한 패턴 감지 설정

---

**마지막 업데이트**: 2026-01-17
**담당자**: PMS-IC 개발팀
