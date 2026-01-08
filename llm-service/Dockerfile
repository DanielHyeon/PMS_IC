FROM nvidia/cuda:12.3.0-devel-ubuntu22.04

WORKDIR /app

# 시스템 의존성 설치 (CUDA + llama-cpp-python 빌드에 필요)
ENV DEBIAN_FRONTEND=noninteractive
ENV CMAKE_ARGS="-DGGML_CUDA=on"
ENV FORCE_CMAKE=1

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    python3-dev \
    build-essential \
    cmake \
    curl \
    git \
    libgl1 \
    libglib2.0-0 \
    poppler-utils \
    && rm -rf /var/lib/apt/lists/*

# CUDA stub libs for build-time linking (libcuda.so.1)
RUN ln -s /usr/local/cuda/lib64/stubs/libcuda.so /usr/local/cuda/lib64/stubs/libcuda.so.1
ENV LIBRARY_PATH=/usr/local/cuda/lib64/stubs:${LIBRARY_PATH}
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64/stubs:/usr/local/cuda/lib64:${LD_LIBRARY_PATH}

# Python 의존성 설치
COPY requirements.txt .
RUN python3 -m pip install --no-cache-dir --upgrade pip setuptools wheel \
    && python3 -m pip install --no-cache-dir -r requirements.txt \
    && python3 -m pip install --no-cache-dir --no-deps "mineru"

# 런타임에서는 실제 CUDA 드라이버 라이브러리를 우선 사용
ENV LD_LIBRARY_PATH=/usr/local/cuda/lib64:/usr/lib/x86_64-linux-gnu

# 애플리케이션 코드 복사 (실제 존재하는 파일만)
COPY app.py .
COPY chat_workflow.py .
COPY document_parser.py .
COPY pdf_ocr_pipeline.py .
COPY rag_service_neo4j.py .
COPY load_ragdata_pdfs_neo4j.py .
COPY test_query_refinement.py .
COPY test_query_refinement_simple.py .

# 모델 디렉토리 생성 (볼륨 마운트로 모델 제공)
RUN mkdir -p /app/models /models /data /output \
    && echo '{"models-dir": "/app/models"}' > /root/mineru.json

# 헬스체크를 위한 curl 설치됨
EXPOSE 8000

CMD ["python3", "app.py"]
