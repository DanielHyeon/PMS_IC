# 로컬 LLM 서비스 (GGUF 모델)

GGUF 형식의 LLM 모델을 사용하는 Python 기반 챗봇 서비스입니다.

## 요구사항

- Python 3.11+
- llama-cpp-python
- GGUF 모델 파일

## 설치 및 실행

### 1. 모델 파일 준비

`models/` 폴더에 GGUF 모델 파일을 배치하세요:

```bash
mkdir -p models
# LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf 파일을 models/ 폴더에 복사
```

### 2. 로컬 실행

```bash
# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
export MODEL_PATH=./models/LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf
export PORT=8000

# 서비스 실행
python app.py
```

### 3. Docker로 실행

```bash
# 모델 파일을 models/ 폴더에 배치
# docker-compose.yml에서 llm-service를 시작
docker-compose up llm-service
```

## API 엔드포인트

### POST /api/chat

채팅 요청을 처리합니다.

**Request:**
```json
{
  "message": "사용자 메시지",
  "context": [
    {
      "role": "user",
      "content": "이전 사용자 메시지"
    },
    {
      "role": "assistant",
      "content": "이전 AI 응답"
    }
  ]
}
```

**Response:**
```json
{
  "reply": "AI 응답",
  "confidence": 0.85,
  "suggestions": []
}
```

### GET /health

서비스 상태를 확인합니다.

**Response:**
```json
{
  "status": "healthy",
  "model_loaded": true
}
```

## 환경 변수

- `MODEL_PATH`: GGUF 모델 파일 경로 (기본값: `./models/LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf`)
- `MAX_TOKENS`: 최대 생성 토큰 수 (기본값: 512)
- `TEMPERATURE`: 생성 온도 (기본값: 0.7)
- `TOP_P`: Top-p 샘플링 (기본값: 0.9)
- `PORT`: 서비스 포트 (기본값: 8000)

## 성능 최적화

- CPU 스레드 수 조정: `n_threads` 파라미터 수정
- 컨텍스트 길이 조정: `n_ctx` 파라미터 수정
- GPU 가속: llama-cpp-python의 GPU 버전 사용

## 문제 해결

1. **모델 로드 실패**: 모델 파일 경로와 파일 존재 여부 확인
2. **메모리 부족**: 더 작은 양자화 모델 사용 (Q4_K, Q5_K 등)
3. **응답 속도 느림**: `n_threads` 증가 또는 더 작은 모델 사용

