FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 설치 (llama-cpp-python 빌드에 필요)
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY app.py .
COPY rag_service.py .
COPY chat_workflow.py .
COPY init_rag_mock_data.py .
COPY README.md .

# 모델 디렉토리 생성 (볼륨 마운트로 모델 제공)
RUN mkdir -p /app/models

# 헬스체크를 위한 curl 설치됨
EXPOSE 8000

CMD ["python", "app.py"]
