FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && apt-get install -y \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 모델 파일 복사 (또는 볼륨 마운트 사용)
COPY . .

# 모델 디렉토리 생성
RUN mkdir -p /app/models

EXPOSE 8000

CMD ["python", "app.py"]

