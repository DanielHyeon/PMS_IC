# MinerU 최적화 방안 및 비교 분석

> 작성일: 2026-01-17  
> 버전: MinerU 2.7.x 기준

---

## 목차

1. [MinerU 개요](#1-mineru-개요)
2. [현재 프로세스 흐름](#2-현재-프로세스-흐름)
3. [기본 설정값 및 구성 옵션](#3-기본-설정값-및-구성-옵션)
4. [최적화 방안 (복잡성 해결, CS)](#4-최적화-방안-복잡성-해결-cs)
5. [혁신적 솔루션 (IS)](#5-혁신적-솔루션-is)
6. [비교 분석표](#6-비교-분석표)
7. [실전 구현 로드맵](#7-실전-구현-로드맵)

---

## 1. MinerU 개요

### 1.1 MinerU란?

MinerU는 OpenDataLab에서 개발한 오픈소스 멀티모달 문서 파싱 시스템입니다. PDF 및 기타 문서/이미지 형식을 Markdown, JSON과 같은 정형 데이터로 변환하며, 레이아웃 보존, 수식/표 처리, 다국어 지원, OCR 통합 기능을 제공합니다.

### 1.2 주요 특징

| 특징 | 설명 |
|------|------|
| **멀티모달 지원** | 텍스트, 이미지, 표, 수식 등 다양한 요소 처리 |
| **다국어 OCR** | 109개 언어 지원 (스캔된 PDF 대응) |
| **유연한 백엔드** | Pipeline, VLM, Hybrid 백엔드 선택 가능 |
| **GPU 가속** | CUDA, MPS, NPU 지원으로 고속 처리 |
| **API 서버** | REST API 및 Gradio WebUI 제공 |

### 1.3 버전 히스토리 (2025-2026)

| 버전 | 출시일 | 주요 변경사항 |
|------|--------|--------------|
| **2.5** | 2025 중반 | VLM 기반 2-Stage Parsing 도입, vLLM 엔진 지원 |
| **2.6.x** | 2025 후반 | 성능 최적화, 환경 변수 확장 |
| **2.7.0** | 2025-12-30 | Hybrid 백엔드 추가, 기본값 변경 (pipeline → hybrid-auto-engine) |
| **2.7.1** | 2026-01-06 | 버그 수정, EXIF 자동 보정, CVE-2025-64512 패치 |

---

## 2. 현재 프로세스 흐름

### 2.1 고수준 파이프라인 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MinerU Processing Pipeline                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │  Input   │───▶│ Preprocessing│───▶│Layout & Parse│───▶│Post-process│ │
│  │   PDF    │    │   & Validate │    │   (Models)   │    │ & Output   │ │
│  └──────────┘    └──────────────┘    └──────────────┘    └───────────┘ │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │                    Backend Selection                                 ││
│  │  ┌─────────┐  ┌─────────┐  ┌────────────────┐                      ││
│  │  │Pipeline │  │  VLM    │  │Hybrid (Default)│                      ││
│  │  │ Backend │  │ Backend │  │    Backend     │                      ││
│  │  └─────────┘  └─────────┘  └────────────────┘                      ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 단계별 상세 프로세스

#### Stage 1: 입력 및 전처리

```
Input (PDF/Image)
       │
       ▼
┌──────────────────────┐
│ 1. 형식 검증          │
│    - 암호화 확인      │
│    - PDF 타입 분류    │
│      (Native/Scanned)│
└──────────────────────┘
       │
       ▼
┌──────────────────────┐
│ 2. 메타데이터 추출    │
│    - PyMuPDF 활용     │
│    - 언어 분류        │
│    - EXIF 보정 (2.7.1)│
└──────────────────────┘
```

#### Stage 2: 레이아웃 및 문서 파싱

**Pipeline 백엔드 (병렬 모델 처리):**
```
┌─────────────────────────────────────────────────────────────┐
│                     Parallel Model Execution                 │
├─────────────┬─────────────┬─────────────┬──────────────────┤
│   Layout    │    Table    │   Formula   │      OCR         │
│  Detection  │ Recognition │  Detection  │  (if needed)     │
│ (doclayout  │ (rapid_     │ (yolo_v8_   │                  │
│   _yolo)    │   table)    │    mfd)     │                  │
├─────────────┴─────────────┴─────────────┴──────────────────┤
│                    Formula Recognition                       │
│                    (unimernet_small)                        │
└─────────────────────────────────────────────────────────────┘
```

**VLM 백엔드 (2-Stage Coarse-to-Fine):**
```
┌────────────────────────────────────────────────────────────┐
│ Stage I: 저해상도 전역 레이아웃 분석                         │
│          (NaViT + Patch Merger + LLM)                       │
│          - 다운샘플링된 이미지로 구조 세그먼테이션            │
└────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│ Stage II: 고해상도 영역별 콘텐츠 인식                        │
│           - Stage I에서 식별된 크롭 영역에 대해              │
│           - 네이티브 해상도로 텍스트/표/수식 인식            │
└────────────────────────────────────────────────────────────┘
```

**Hybrid 백엔드 (2.7.0+ 기본값):**
```
┌────────────────────────────────────────────────────────────┐
│              VLM 기반 + Pipeline 확장 기능                   │
├────────────────────────────────────────────────────────────┤
│ • 텍스트 기반 PDF: 직접 텍스트 추출 (환각 감소)              │
│ • 스캔 PDF: 109개 언어 OCR 지원                             │
│ • 인라인 수식 인식 독립 스위치                               │
│ • auto-engine: 환경에 따라 최적 추론 엔진 자동 선택          │
└────────────────────────────────────────────────────────────┘
```

#### Stage 3: 후처리 및 정렬

```
┌──────────────────────┐
│ 1. 바운딩 박스 해결   │
│    - 중복 제거        │
│    - IoU 기반 병합    │
└──────────────────────┘
       │
       ▼
┌──────────────────────┐
│ 2. 읽기 순서 결정     │
│    - Top→Bottom      │
│    - Left→Right      │
│    - LayoutReader    │
└──────────────────────┘
       │
       ▼
┌──────────────────────┐
│ 3. 헤더/푸터 필터링   │
│    (선택적)          │
└──────────────────────┘
```

#### Stage 4: 출력 형식 변환

```
┌──────────────────────┐
│    middle_json       │
│   (내부 구조화 형식)  │
└──────────────────────┘
       │
       ├──────────────────┬──────────────────┐
       ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Markdown   │  │     JSON     │  │     HTML     │
│   (LLM/RAG)  │  │  (구조화)    │  │   (표 전용)  │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 2.3 핵심 모델 컴포넌트

| 컴포넌트 | 역할 | 모델 옵션 |
|---------|------|----------|
| **Layout Detection** | 구조적 요소 식별 (섹션, 단락, 컬럼) | `doclayout_yolo`, `layoutlmv3` |
| **OCR / Text Extraction** | 스캔 문서 텍스트 추출 | 109개 언어 지원 |
| **Table Recognition** | 표 경계, 행/열 분할, 재구성 | `rapid_table`, `TableMaster` |
| **Formula Detection** | 수식 영역 감지 (인라인/디스플레이) | `yolo_v8_mfd` |
| **Formula Recognition** | 수식 내용 인식, LaTeX 변환 | `unimernet_small` |
| **Reading Order** | 논리적 읽기 순서 결정 | `layoutreader` |

---

## 3. 기본 설정값 및 구성 옵션

### 3.1 설정 파일 위치

| OS | 경로 |
|----|------|
| **Linux** | `~/.config/magic_pdf/magic-pdf.json` 또는 `~/.magic/pdf/magic-pdf.json` |
| **Windows** | `C:\Users\{username}\magic-pdf.json` |

### 3.2 전체 설정 구조 (magic-pdf.json)

```json
{
  "config_version": "1.2.1",
  
  "bucket_info": {
    "bucket-name-1": ["ACCESS_KEY", "SECRET_KEY", "ENDPOINT"],
    "bucket-name-2": ["...", "...", "..."]
  },
  
  "models-dir": "/root/.cache/models",
  "layoutreader-model-dir": "/root/.cache/layoutreader",
  "device-mode": "cuda",
  
  "layout-config": {
    "model": "doclayout_yolo"
  },
  
  "formula-config": {
    "mfd_model": "yolo_v8_mfd",
    "mfr_model": "unimernet_small",
    "enable": true
  },
  
  "table-config": {
    "model": "rapid_table",
    "sub_model": "slanet_plus",
    "enable": true,
    "max_time": 400
  },
  
  "latex-delimiter-config": {
    "display": { "left": "\\[", "right": "\\]" },
    "inline": { "left": "\\(", "right": "\\)" }
  },
  
  "llm-aided-config": {
    "formula_aided": {
      "api_key": "your_api_key",
      "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
      "model": "qwen2.5-7b-instruct",
      "enable": false
    },
    "text_aided": {
      "api_key": "...",
      "base_url": "...",
      "model": "...",
      "enable": false
    },
    "title_aided": {
      "api_key": "...",
      "base_url": "...",
      "model": "...",
      "enable": false
    }
  }
}
```

### 3.3 주요 설정 항목 상세

#### 기본 설정

| 설정 항목 | 기본값 | 설명 |
|----------|--------|------|
| `config_version` | `"1.2.1"` | 설정 스키마 버전 |
| `models-dir` | 시스템별 상이 | 모델 파일 저장 경로 |
| `layoutreader-model-dir` | 시스템별 상이 | LayoutReader 모델 경로 |
| `device-mode` | `"cuda"` | 실행 디바이스 (`cpu`, `cuda`, `npu`) |

#### 레이아웃 설정 (layout-config)

| 설정 항목 | 기본값 | 설명 |
|----------|--------|------|
| `model` | `"doclayout_yolo"` | 레이아웃 감지 모델 |

> ⚠️ 레이아웃 감지는 현재 비활성화 불가

#### 수식 설정 (formula-config)

| 설정 항목 | 기본값 | 설명 |
|----------|--------|------|
| `mfd_model` | `"yolo_v8_mfd"` | 수식 감지 모델 |
| `mfr_model` | `"unimernet_small"` | 수식 인식 모델 |
| `enable` | `true` | 수식 처리 활성화 여부 |

#### 표 설정 (table-config)

| 설정 항목 | 기본값 | 설명 |
|----------|--------|------|
| `model` | `"rapid_table"` | 표 인식 모델 |
| `sub_model` | `"slanet_plus"` | 하위 모델 |
| `enable` | `true` | 표 인식 활성화 여부 |
| `max_time` | `400` | 최대 처리 시간 (ms) |

### 3.4 환경 변수

| 환경 변수 | 설명 | 예시 값 |
|----------|------|---------|
| `MINERU_DEVICE_MODE` | GPU 가속 모드 강제 설정 | `cuda`, `cpu`, `npu` |
| `MINERU_VIRTUAL_VRAM_SIZE` | 프로세스당 VRAM 크기 조절 | `8` (GB) |
| `MINERU_MODEL_SOURCE` | 모델 소스 위치 | `local`, `huggingface` |
| `MINERU_BACKEND` | 백엔드 선택 | `pipeline`, `vlm`, `hybrid` |
| `MINERU_TABLE_MERGE_ENABLE` | 표 병합 기능 | `1`, `0` |
| `MINERU_INTRA_OP_NUM_THREADS` | ONNX 내부 스레드 수 | `4` |
| `MINERU_INTER_OP_NUM_THREADS` | ONNX 외부 스레드 수 | `2` |
| `MINERU_MIN_BATCH_INFERENCE_SIZE` | 최소 배치 추론 크기 | `4` |

---

## 4. 최적화 방안 (복잡성 해결, CS)

### 4.1 하드웨어 및 실행 가속 계층

#### GPU VRAM 최적화

```bash
# 16GB VRAM 환경에서 병렬 처리를 위한 분할
export MINERU_VIRTUAL_VRAM_SIZE=8  # 프로세스당 8GB 할당
```

| VRAM 용량 | 권장 설정 | 기대 효과 |
|-----------|----------|----------|
| 6GB | 단일 프로세스, Pipeline 백엔드 | 기본 동작 |
| 8-12GB | VLM 백엔드 단일 프로세스 | 2-Stage Parsing 활용 |
| 16GB+ | VRAM 분할, 다중 프로세스 | 병렬 처리로 처리량 증가 |
| 24GB+ (A100) | vLLM Data Parallel | 최대 처리량 |

#### 가속 엔진 선택

```bash
# NVIDIA GPU
export MINERU_DEVICE_MODE="cuda"

# Apple Silicon
export MINERU_DEVICE_MODE="mps"

# vLLM 엔진 사용 (VLM 백엔드)
mineru --engine vllm --data-parallel-size 2
```

### 4.2 모델 계층 최적화

#### 레이아웃 모델 전환

| 모델 | 속도 | 정확도 | 권장 상황 |
|------|------|--------|----------|
| `layoutlmv3` | 느림 | 높음 | 복잡한 학술 문서, 정확도 우선 |
| `doclayout_yolo` | **~10배 빠름** | 유사 | **대부분의 경우 권장** |

```json
{
  "layout-config": {
    "model": "doclayout_yolo"
  }
}
```

#### 수식 및 표 인식 제어

**수식이 없는 문서:**
```json
{
  "formula-config": {
    "enable": false
  }
}
```

**표 처리 최적화:**
```json
{
  "table-config": {
    "model": "rapid_table",
    "max_time": 200
  }
}
```

### 4.3 소프트웨어 파이프라인 최적화

#### 텍스트 추출 방식 분기

```python
import fitz  # PyMuPDF

def classify_pdf(pdf_path):
    """PDF 타입 분류: Native vs Scanned"""
    doc = fitz.open(pdf_path)
    text_pages = 0
    
    for page in doc:
        text = page.get_text()
        if len(text.strip()) > 100:  # 텍스트가 충분히 있으면
            text_pages += 1
    
    text_ratio = text_pages / len(doc)
    
    if text_ratio > 0.8:
        return "native"  # OCR 불필요
    elif text_ratio < 0.2:
        return "scanned"  # OCR 필수
    else:
        return "mixed"  # 혼합

# 사용 예시
pdf_type = classify_pdf("document.pdf")
if pdf_type == "native":
    # txt 모드 (PyMuPDF 직접 추출)
    mode = "txt"
else:
    # ocr 모드
    mode = "ocr"
```

#### 백엔드 선택 가이드

| 상황 | 권장 백엔드 | 이유 |
|------|------------|------|
| 일반 문서, 균형 중시 | `hybrid-auto-engine` | 2.7.0 기본값, 자동 최적화 |
| CPU 전용 환경 | `pipeline` | GPU 불필요 |
| 최대 정확도 필요 | `vlm` | 2-Stage 심층 분석 |
| 대량 처리, 처리량 중시 | `vlm-vllm` | vLLM 가속 |

---

## 5. 혁신적 솔루션 (IS)

### 5.1 분산 병렬 처리 아키텍처

#### Docker Compose 기반 스케일링

```yaml
version: '3.8'

services:
  # vLLM 추론 서버 (전용 GPU)
  mineru-vllm-server:
    image: opendatalab/mineru:latest
    command: >
      mineru-vllm-server
      --host=0.0.0.0
      --port=30000
      --data-parallel-size=2
      --gpu-memory-utilization=0.8
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 2
              capabilities: [gpu]
    shm_size: '16g'
    ipc: host

  # API 서버 (수평 확장 가능)
  mineru-api:
    image: opendatalab/mineru:latest
    command: >
      mineru-api
      --host=0.0.0.0
      --port=8000
      --max_concurrency=200
    deploy:
      replicas: 3  # 수평 확장
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    depends_on:
      - mineru-vllm-server

  # 로드 밸런서
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - mineru-api
```

#### Kubernetes 배포 예시

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mineru-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mineru-api
  template:
    spec:
      containers:
      - name: mineru-api
        image: opendatalab/mineru:latest
        args:
          - mineru-api
          - --host=0.0.0.0
          - --port=8000
          - --max_concurrency=200
        resources:
          limits:
            nvidia.com/gpu: 1
          requests:
            cpu: "2"
            memory: "8Gi"
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mineru-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mineru-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 5.2 지능형 전처리 및 후처리

#### 레이아웃 인식 청킹 (Layout-Aware Chunking)

```python
import re
from typing import List, Dict

def layout_aware_chunking(markdown_content: str, 
                          chunk_size: int = 500,
                          overlap: int = 50) -> List[Dict]:
    """
    MinerU 출력 Markdown을 레이아웃 기반으로 청킹
    
    Args:
        markdown_content: MinerU가 생성한 Markdown
        chunk_size: 청크 최대 크기 (토큰)
        overlap: 청크 간 오버랩
    
    Returns:
        청크 목록 (메타데이터 포함)
    """
    chunks = []
    current_section = ""
    current_content = []
    
    # 헤더 패턴 (# ~ ######)
    header_pattern = re.compile(r'^(#{1,6})\s+(.+)$', re.MULTILINE)
    
    lines = markdown_content.split('\n')
    
    for line in lines:
        header_match = header_pattern.match(line)
        
        if header_match:
            # 이전 섹션 저장
            if current_content:
                content_text = '\n'.join(current_content)
                sub_chunks = _split_by_size(content_text, chunk_size, overlap)
                for idx, sub_chunk in enumerate(sub_chunks):
                    chunks.append({
                        'section': current_section,
                        'content': sub_chunk,
                        'chunk_index': idx,
                        'total_chunks': len(sub_chunks)
                    })
            
            # 새 섹션 시작
            level = len(header_match.group(1))
            title = header_match.group(2)
            current_section = f"{'#' * level} {title}"
            current_content = []
        else:
            current_content.append(line)
    
    # 마지막 섹션 처리
    if current_content:
        content_text = '\n'.join(current_content)
        sub_chunks = _split_by_size(content_text, chunk_size, overlap)
        for idx, sub_chunk in enumerate(sub_chunks):
            chunks.append({
                'section': current_section,
                'content': sub_chunk,
                'chunk_index': idx,
                'total_chunks': len(sub_chunks)
            })
    
    return chunks

def _split_by_size(text: str, max_size: int, overlap: int) -> List[str]:
    """텍스트를 지정 크기로 분할 (오버랩 포함)"""
    if len(text) <= max_size:
        return [text]
    
    chunks = []
    start = 0
    
    while start < len(text):
        end = min(start + max_size, len(text))
        
        # 문장 경계에서 자르기 시도
        if end < len(text):
            last_period = text.rfind('.', start, end)
            if last_period > start + max_size // 2:
                end = last_period + 1
        
        chunks.append(text[start:end])
        start = end - overlap
    
    return chunks
```

#### 이미지/수식 메타데이터 관리

```python
from typing import Dict, List
import json
import hashlib

class MultimodalMetadataManager:
    """
    MinerU 추출 결과의 멀티모달 메타데이터 관리
    """
    
    def __init__(self, output_dir: str):
        self.output_dir = output_dir
        self.metadata_store = {}
    
    def process_mineru_output(self, 
                               markdown_path: str, 
                               images_dir: str,
                               llm_summarizer=None) -> Dict:
        """
        MinerU 출력을 처리하여 메타데이터 생성
        
        Args:
            markdown_path: Markdown 파일 경로
            images_dir: 추출된 이미지 디렉토리
            llm_summarizer: LLM 요약 함수 (선택)
        
        Returns:
            처리된 메타데이터
        """
        result = {
            'images': [],
            'formulas': [],
            'tables': [],
            'enhanced_markdown': ''
        }
        
        with open(markdown_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 이미지 참조 추출 및 요약 생성
        import re
        image_pattern = re.compile(r'!\[([^\]]*)\]\(([^)]+)\)')
        
        for match in image_pattern.finditer(content):
            alt_text = match.group(1)
            image_path = match.group(2)
            
            image_meta = {
                'path': image_path,
                'alt_text': alt_text,
                'hash': self._compute_hash(f"{images_dir}/{image_path}"),
            }
            
            # LLM으로 이미지 요약 생성 (선택)
            if llm_summarizer:
                summary = llm_summarizer(f"{images_dir}/{image_path}")
                image_meta['summary'] = summary
                
                # Markdown에 요약 삽입
                placeholder = f"[IMAGE_SUMMARY: {summary}]"
                content = content.replace(match.group(0), 
                                         f"{match.group(0)}\n{placeholder}")
            
            result['images'].append(image_meta)
        
        result['enhanced_markdown'] = content
        return result
    
    def _compute_hash(self, filepath: str) -> str:
        """파일 해시 계산"""
        try:
            with open(filepath, 'rb') as f:
                return hashlib.md5(f.read()).hexdigest()
        except:
            return ""
```

### 5.3 지능형 모델 스위칭

```python
import fitz
from dataclasses import dataclass
from enum import Enum

class DocumentComplexity(Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"

@dataclass
class DocumentAnalysis:
    complexity: DocumentComplexity
    has_tables: bool
    has_formulas: bool
    has_images: bool
    page_count: int
    recommended_backend: str
    recommended_config: dict

class SmartModelRouter:
    """
    문서 복잡도 분석 기반 모델 자동 라우팅
    """
    
    def __init__(self, sample_pages: int = 2):
        self.sample_pages = sample_pages
    
    def analyze_document(self, pdf_path: str) -> DocumentAnalysis:
        """
        문서 분석 및 최적 설정 추천
        """
        doc = fitz.open(pdf_path)
        page_count = len(doc)
        
        # 샘플 페이지 분석 (중간 1-2 페이지)
        sample_indices = self._get_sample_indices(page_count)
        
        metrics = {
            'text_density': 0,
            'image_count': 0,
            'table_indicators': 0,
            'formula_indicators': 0
        }
        
        for idx in sample_indices:
            page = doc[idx]
            
            # 텍스트 밀도
            text = page.get_text()
            metrics['text_density'] += len(text)
            
            # 이미지 수
            images = page.get_images()
            metrics['image_count'] += len(images)
            
            # 표 지표 (수평선, 격자 패턴)
            drawings = page.get_drawings()
            horizontal_lines = sum(1 for d in drawings 
                                   if self._is_horizontal_line(d))
            metrics['table_indicators'] += horizontal_lines
            
            # 수식 지표 (특수 폰트, 기호)
            blocks = page.get_text("dict")["blocks"]
            for block in blocks:
                if "lines" in block:
                    for line in block["lines"]:
                        for span in line["spans"]:
                            if self._has_math_font(span):
                                metrics['formula_indicators'] += 1
        
        # 복잡도 계산
        complexity = self._calculate_complexity(metrics, len(sample_indices))
        
        # 권장 설정 결정
        recommended = self._get_recommendation(complexity, metrics)
        
        return DocumentAnalysis(
            complexity=complexity,
            has_tables=metrics['table_indicators'] > 5,
            has_formulas=metrics['formula_indicators'] > 3,
            has_images=metrics['image_count'] > 2,
            page_count=page_count,
            recommended_backend=recommended['backend'],
            recommended_config=recommended['config']
        )
    
    def _get_sample_indices(self, page_count: int) -> list:
        """샘플 페이지 인덱스 결정"""
        if page_count <= 2:
            return list(range(page_count))
        
        mid = page_count // 2
        return [mid - 1, mid] if page_count > 2 else [mid]
    
    def _is_horizontal_line(self, drawing) -> bool:
        """수평선 여부 판단"""
        # 간소화된 로직
        return drawing.get('type') == 'l'
    
    def _has_math_font(self, span) -> bool:
        """수식 폰트 여부 판단"""
        math_fonts = ['Symbol', 'MT Extra', 'Cambria Math']
        font = span.get('font', '')
        return any(mf in font for mf in math_fonts)
    
    def _calculate_complexity(self, metrics: dict, sample_count: int) -> DocumentComplexity:
        """복잡도 계산"""
        avg_tables = metrics['table_indicators'] / sample_count
        avg_formulas = metrics['formula_indicators'] / sample_count
        avg_images = metrics['image_count'] / sample_count
        
        score = avg_tables * 2 + avg_formulas * 3 + avg_images * 1.5
        
        if score > 15:
            return DocumentComplexity.HIGH
        elif score > 5:
            return DocumentComplexity.MEDIUM
        else:
            return DocumentComplexity.LOW
    
    def _get_recommendation(self, complexity: DocumentComplexity, 
                           metrics: dict) -> dict:
        """복잡도 기반 권장 설정"""
        
        if complexity == DocumentComplexity.LOW:
            return {
                'backend': 'pipeline',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {'enable': metrics['formula_indicators'] > 0},
                    'table-config': {
                        'model': 'rapid_table',
                        'enable': metrics['table_indicators'] > 0,
                        'max_time': 100
                    }
                }
            }
        
        elif complexity == DocumentComplexity.MEDIUM:
            return {
                'backend': 'hybrid-auto-engine',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {'enable': True},
                    'table-config': {
                        'model': 'rapid_table',
                        'enable': True,
                        'max_time': 200
                    }
                }
            }
        
        else:  # HIGH
            return {
                'backend': 'vlm-vllm',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {
                        'enable': True,
                        'mfr_model': 'unimernet_small'
                    },
                    'table-config': {
                        'model': 'rapid_table',
                        'sub_model': 'slanet_plus',
                        'enable': True,
                        'max_time': 400
                    }
                }
            }
```

---

## 6. 비교 분석표

### 6.1 백엔드 성능 비교

| 항목 | Pipeline | VLM | Hybrid |
|------|----------|-----|--------|
| **속도** | ★★★☆☆ | ★★☆☆☆ | ★★★☆☆ |
| **정확도** | ★★★☆☆ | ★★★★★ | ★★★★☆ |
| **GPU 요구** | 낮음 (6GB+) | 높음 (8GB+) | 중간 (8GB+) |
| **CPU 지원** | ✅ 완전 | ❌ 제한적 | ⚠️ 부분 |
| **확장성** | 중간 | 높음 | 높음 |
| **설정 복잡도** | 낮음 | 높음 | 중간 |
| **권장 용도** | 간단한 문서, CPU 환경 | 복잡한 학술 문서 | 일반 목적 (**기본값**) |

### 6.2 레이아웃 모델 비교

| 항목 | layoutlmv3 | doclayout_yolo |
|------|------------|----------------|
| **속도** | 1x (기준) | **~10x** |
| **정확도** | 높음 | 유사 |
| **GPU 메모리** | 높음 | 낮음 |
| **복잡 레이아웃** | 우수 | 양호 |
| **권장 상황** | 정밀 분석 필요 시 | **대부분의 경우** |

### 6.3 표 인식 모델 비교

| 항목 | rapid_table | TableMaster |
|------|-------------|-------------|
| **속도** | 빠름 | 느림 |
| **정확도** | 양호 | 우수 |
| **복잡한 표** | 보통 | 우수 |
| **권장 상황** | 일반 표, 처리량 우선 | 복잡한 표, 정확도 우선 |

### 6.4 기본 vs 최적화 설정 비교

| 설정 항목 | 기본값 | 최적화 권장값 | 기대 효과 |
|----------|--------|--------------|----------|
| `layout-config.model` | `doclayout_yolo` | `doclayout_yolo` | (이미 최적) |
| `table-config.model` | `rapid_table` | `rapid_table` | 속도/정확도 균형 |
| `formula-config.enable` | `true` | **상황별 조절** | 불필요 시 비활성화로 부하 감소 |
| `table-config.max_time` | `400` | **`200`** | 타임아웃 방지 (복잡한 표) |
| `device-mode` | (자동) | **명시적 지정** | GPU 가속 보장 |
| Backend | `hybrid-auto-engine` | **문서별 라우팅** | 최적 성능 |

### 6.5 처리 성능 벤치마크 (참고)

| 환경 | 백엔드 | 처리량 | 비고 |
|------|--------|--------|------|
| A100 80GB | VLM (vLLM) | ~2.12 pages/sec | 2,337 tokens/sec |
| RTX 3090 24GB | Hybrid | ~1.5 pages/sec | 추정치 |
| RTX 4090 24GB | Hybrid | ~2.0 pages/sec | 추정치 |
| M1 Max 32GB | Pipeline (MPS) | ~0.8 pages/sec | 추정치 |
| CPU Only | Pipeline | ~0.2 pages/sec | 추정치 |

---

## 7. 실전 구현 로드맵

### 7.1 단계별 최적화 로드맵

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MinerU 최적화 로드맵                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Phase 1: 즉시 적용 (Quick Wins)                                  │   │
│  │ ───────────────────────────────────────────────────────────────  │   │
│  │ □ GPU 가속 환경 구축 (CUDA/MPS 설정)                             │   │
│  │ □ doclayout_yolo 모델 전환 확인                                  │   │
│  │ □ 불필요한 기능 비활성화 (수식 등)                                │   │
│  │ □ 기본 환경 변수 설정                                            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Phase 2: 자동화 (Automation)                                     │   │
│  │ ───────────────────────────────────────────────────────────────  │   │
│  │ □ PDF 타입 분류 자동화 (Native vs Scanned)                       │   │
│  │ □ 추출 모드 자동 분기 로직 구현                                   │   │
│  │ □ 스마트 모델 라우팅 시스템 도입                                  │   │
│  │ □ 에러 처리 및 재시도 로직                                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Phase 3: 고도화 (Advanced)                                       │   │
│  │ ───────────────────────────────────────────────────────────────  │   │
│  │ □ 분산 처리 아키텍처 구축 (Docker/K8s)                           │   │
│  │ □ 레이아웃 인식 청킹 파이프라인 통합                              │   │
│  │ □ LangGraph 워크플로우 엔진 연계                                  │   │
│  │ □ 멀티모달 메타데이터 관리 시스템                                 │   │
│  │ □ RAG 시스템 최적화 (검색 정확도 향상)                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Phase 1 구현 체크리스트

#### 환경 설정 스크립트

```bash
#!/bin/bash
# setup_mineru_optimized.sh

echo "=== MinerU 최적화 환경 설정 ==="

# 1. 환경 변수 설정
export MINERU_DEVICE_MODE="cuda"
export MINERU_MODEL_SOURCE="local"
export MINERU_BACKEND="hybrid-auto-engine"

# 2. 설정 파일 위치 확인
CONFIG_DIR="$HOME/.config/magic_pdf"
CONFIG_FILE="$CONFIG_DIR/magic-pdf.json"

# 3. 설정 디렉토리 생성
mkdir -p "$CONFIG_DIR"

# 4. 최적화된 설정 파일 생성
cat > "$CONFIG_FILE" << 'EOF'
{
  "config_version": "1.2.1",
  "device-mode": "cuda",
  "layout-config": {
    "model": "doclayout_yolo"
  },
  "formula-config": {
    "mfd_model": "yolo_v8_mfd",
    "mfr_model": "unimernet_small",
    "enable": false
  },
  "table-config": {
    "model": "rapid_table",
    "sub_model": "slanet_plus",
    "enable": true,
    "max_time": 200
  }
}
EOF

echo "설정 완료: $CONFIG_FILE"

# 5. GPU 확인
if command -v nvidia-smi &> /dev/null; then
    echo "=== GPU 상태 ==="
    nvidia-smi --query-gpu=name,memory.total,memory.free --format=csv
fi

echo "=== MinerU 최적화 설정 완료 ==="
```

### 7.3 Phase 2 구현: 자동 분류 서비스

```python
# auto_classify_service.py

from fastapi import FastAPI, UploadFile, File
from typing import Dict
import tempfile
import os

from smart_model_router import SmartModelRouter

app = FastAPI(title="MinerU Auto-Classify Service")
router = SmartModelRouter()

@app.post("/analyze")
async def analyze_pdf(file: UploadFile = File(...)) -> Dict:
    """
    PDF 분석 및 최적 설정 추천
    """
    # 임시 파일로 저장
    with tempfile.NamedTemporaryFile(delete=False, suffix=".pdf") as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name
    
    try:
        # 문서 분석
        analysis = router.analyze_document(tmp_path)
        
        return {
            "filename": file.filename,
            "complexity": analysis.complexity.value,
            "characteristics": {
                "has_tables": analysis.has_tables,
                "has_formulas": analysis.has_formulas,
                "has_images": analysis.has_images,
                "page_count": analysis.page_count
            },
            "recommendation": {
                "backend": analysis.recommended_backend,
                "config": analysis.recommended_config
            }
        }
    finally:
        os.unlink(tmp_path)

@app.post("/process")
async def process_pdf(file: UploadFile = File(...), 
                      auto_optimize: bool = True) -> Dict:
    """
    PDF 처리 (자동 최적화 옵션)
    """
    # 구현...
    pass
```

### 7.4 모니터링 및 메트릭

```python
# metrics.py

from prometheus_client import Counter, Histogram, Gauge
import time
from functools import wraps

# 메트릭 정의
DOCS_PROCESSED = Counter(
    'mineru_documents_processed_total',
    'Total documents processed',
    ['backend', 'complexity']
)

PROCESSING_TIME = Histogram(
    'mineru_processing_seconds',
    'Document processing time',
    ['backend', 'document_type'],
    buckets=[0.5, 1, 2, 5, 10, 30, 60, 120]
)

PAGES_PER_SECOND = Gauge(
    'mineru_pages_per_second',
    'Processing throughput',
    ['backend']
)

def track_processing(backend: str, document_type: str):
    """처리 메트릭 데코레이터"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            start = time.time()
            
            try:
                result = func(*args, **kwargs)
                DOCS_PROCESSED.labels(
                    backend=backend,
                    complexity=document_type
                ).inc()
                return result
            finally:
                elapsed = time.time() - start
                PROCESSING_TIME.labels(
                    backend=backend,
                    document_type=document_type
                ).observe(elapsed)
        
        return wrapper
    return decorator
```

---

## 부록: 참고 자료

### A. 공식 문서

- [MinerU GitHub](https://github.com/opendatalab/MinerU)
- [MinerU ReadTheDocs](https://mineru.readthedocs.io/)
- [Docker 배포 가이드](https://opendatalab.github.io/MinerU/quick_start/docker_deployment/)

### B. 환경 변수 전체 목록

| 변수명 | 설명 |
|--------|------|
| `MINERU_DEVICE_MODE` | 실행 디바이스 |
| `MINERU_VIRTUAL_VRAM_SIZE` | 가상 VRAM 크기 |
| `MINERU_MODEL_SOURCE` | 모델 소스 |
| `MINERU_BACKEND` | 백엔드 선택 |
| `MINERU_TABLE_MERGE_ENABLE` | 표 병합 활성화 |
| `MINERU_INTRA_OP_NUM_THREADS` | ONNX 내부 스레드 |
| `MINERU_INTER_OP_NUM_THREADS` | ONNX 외부 스레드 |
| `MINERU_MIN_BATCH_INFERENCE_SIZE` | 최소 배치 크기 |
| `MINERU_FORMULA_CH_SUPPORT` | 중국어 수식 지원 |

### C. 트러블슈팅

| 문제 | 원인 | 해결책 |
|------|------|--------|
| CUDA OOM | VRAM 부족 | `MINERU_VIRTUAL_VRAM_SIZE` 조절 또는 Pipeline 백엔드 사용 |
| 느린 처리 | CPU 모드 실행 | GPU 환경 확인, `MINERU_DEVICE_MODE=cuda` 설정 |
| 표 인식 실패 | 타임아웃 | `max_time` 증가 또는 `MINERU_TABLE_MERGE_ENABLE=0` |
| 수식 누락 | 비활성화됨 | `formula-config.enable=true` 확인 |

---

> **문서 버전 이력**
> - v1.0 (2026-01-17): 초기 작성
