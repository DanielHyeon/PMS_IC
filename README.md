# PMS Insurance Claims - AI 기반 프로젝트 관리 시스템

> **보험 심사 프로젝트 관리를 위한 AI 통합 PMS 플랫폼**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![Python](https://img.shields.io/badge/Python-3.10-blue.svg)](https://www.python.org/)

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [시스템 아키텍처](#-시스템-아키텍처)
- [기술 스택](#-기술-스택)
- [시작하기](#-시작하기)
- [사용 가이드](#-사용-가이드)
- [프로덕션 배포](#-프로덕션-배포)
- [문서](#-문서)

## 🎯 프로젝트 개요

PMS Insurance Claims는 보험 심사 프로젝트를 효율적으로 관리하기 위한 AI 기반 프로젝트 관리 시스템입니다. Neo4j GraphRAG와 로컬 LLM을 활용하여 지능형 챗봇 기능을 제공하며, 프로젝트 일정, 리소스, 리스크를 통합 관리합니다.

### 핵심 가치

- **AI 기반 의사결정**: GraphRAG를 활용한 컨텍스트 기반 프로젝트 인사이트
- **통합 관리**: 프로젝트 전주기 관리 (일정, 리소스, 리스크, 산출물)
- **로컬 AI**: 프라이버시 보장을 위한 온프레미스 LLM 배포
- **실시간 협업**: WebSocket 기반 실시간 채팅 및 알림

## ✨ 주요 기능

### 1. AI 챗봇 (GraphRAG + LLM)
- **Neo4j 기반 GraphRAG**: 벡터 + 그래프 하이브리드 검색
- **로컬 LLM**: Google Gemma 3 12B 모델 (GGUF)
- **LangGraph 워크플로우**: 의도 분류 → RAG 검색 → 응답 생성
- **MinerU2.5 파서**: 고급 PDF OCR 처리 (테이블, 이미지 지원)

### 2. 프로젝트 관리
- 프로젝트 생성, 수정, 삭제
- 간트 차트 기반 일정 관리
- 리소스 할당 및 워크로드 분석
- 마일스톤 및 산출물 관리

### 3. 리스크 관리
- 리스크 식별, 평가, 모니터링
- 리스크 매트릭스 시각화
- 대응 계획 수립 및 추적

### 4. 리포트 생성
- 프로젝트 진척 보고서
- 리스크 분석 리포트
- 리소스 활용 리포트
- PDF/Excel 내보내기

## 🏗 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────────┐
│                        사용자 (브라우저)                         │
└────────────────────┬─────────────────────────────────────────┘
                     │
         ┌───────────▼───────────┐
         │  Nginx (선택사항)       │
         │  Reverse Proxy         │
         └───────────┬───────────┘
                     │
      ┌──────────────┼──────────────┐
      │              │              │
┌─────▼─────┐  ┌────▼────┐  ┌──────▼──────┐
│ Frontend  │  │ Backend │  │ LLM Service │
│ (React)   │  │ (Spring)│  │ (Flask)     │
│ Port:5173 │  │ Port:8083│ │ Port:8000   │
└───────────┘  └────┬────┘  └──────┬──────┘
                    │              │
        ┌───────────┼──────────────┼───────────┐
        │           │              │           │
   ┌────▼────┐ ┌───▼───┐    ┌─────▼─────┐ ┌──▼──┐
   │Postgres │ │ Redis │    │  Neo4j    │ │ GPU │
   │ :5433   │ │ :6379 │    │GraphRAG   │ │     │
   └─────────┘ └───────┘    │  :7687    │ └─────┘
                             └───────────┘
```

### 데이터 흐름

1. **사용자 → Frontend**: React SPA에서 UI 인터랙션
2. **Frontend → Backend**: REST API 호출
3. **Backend → Postgres**: 프로젝트 데이터 CRUD
4. **Backend → Redis**: 세션 및 캐시 관리
5. **Backend → LLM Service**: AI 챗봇 요청
6. **LLM Service → Neo4j**: RAG 벡터 검색
7. **LLM Service → GPU**: LLM 추론 (Gemma 3 12B)

## 🛠 기술 스택

### Backend
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.2.1
- **주요 라이브러리**:
  - Spring Data JPA (ORM)
  - Spring Security + JWT
  - Spring WebSocket (실시간 통신)
  - Spring Data Redis
  - Apache PDFBox, Apache POI

### Frontend
- **언어**: JavaScript (ES6+)
- **프레임워크**: React 18.2.0
- **상태 관리**: React Context API
- **UI 라이브러리**: Custom CSS
- **빌드 도구**: Vite

### AI/ML Service
- **언어**: Python 3.10
- **LLM**: Google Gemma 3 12B (GGUF, llama-cpp-python)
- **Embedding**: multilingual-e5-large (sentence-transformers)
- **RAG**: Neo4j (vector + graph)
- **워크플로우**: LangGraph
- **파서**: MinerU2.5-2509-1.2B

### Database & Storage
- **RDBMS**: PostgreSQL 15
- **Cache**: Redis 7
- **Graph DB**: Neo4j 5.20 (Community Edition)
- **Vector Store**: Neo4j Vector Index

### Infrastructure
- **컨테이너화**: Docker, Docker Compose
- **리버스 프록시**: Nginx
- **CI/CD**: Git, GitHub

## 🚀 시작하기

### 사전 요구사항

- **Docker**: 24.0 이상
- **Docker Compose**: v2.0 이상
- **GPU**: NVIDIA GPU (LLM 추론용, 선택사항)
- **메모리**: 최소 16GB RAM
- **디스크**: 최소 50GB 여유 공간

### 1. 저장소 클론

```bash
git clone <repository-url>
cd pms-ic
```

### 2. 환경 변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# 중요: 프로덕션에서는 반드시 변경 필요
# - POSTGRES_PASSWORD
# - JWT_SECRET (최소 256비트)
# - NEO4J_PASSWORD
```

### 3. 모델 다운로드

```bash
# llm-service/models 디렉토리에 모델 파일 배치
mkdir -p llm-service/models

# Gemma 3 12B GGUF 모델 다운로드
# HuggingFace에서 다운로드: https://huggingface.co/...
# llm-service/models/google.gemma-3-12b-pt.Q5_K_M.gguf 경로에 배치

# LFM2 또는 MinerU2.5 모델 다운로드
# llm-service/models/LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf 경로에 배치
# llm-service/models/MinerU2.5-2509-1.2B.i1-Q6_K.gguf 경로에 배치
```

### 4. 서비스 시작

```bash
# 전체 서비스 시작 (GPU 사용)
docker-compose up -d

# GPU 없이 시작 (CPU 모드)
docker-compose up -d
# llm-service 환경변수에서 LLM_N_GPU_LAYERS=0 설정
```

### 5. RAG 데이터 인덱싱

```bash
# ragdata 디렉토리에 PDF 파일 추가
cp your-documents.pdf ragdata/

# Neo4j에 인덱싱
docker exec pms-llm-service python3 /app/load_ragdata_pdfs_neo4j.py --ragdata-dir /app/ragdata
```

### 6. 서비스 접속

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8083
- **Swagger API 문서**: http://localhost:8083/swagger-ui.html
- **LLM Service**: http://localhost:8000
- **Neo4j Browser**: http://localhost:7474 (neo4j / pmspassword123)
- **PgAdmin**: http://localhost:5050 (admin@pms.com / admin)

### 7. 기본 사용자로 로그인

```
이메일: admin@insure.com
비밀번호: admin123
```

## 📖 사용 가이드

### AI 챗봇 사용하기

1. 로그인 후 **채팅** 메뉴 선택
2. 질문 입력 (예: "프로젝트 관리 프로세스는?")
3. AI가 RAG 문서를 검색하여 답변 생성
4. 대화 히스토리가 자동 저장됨

### 프로젝트 생성하기

1. **프로젝트** 메뉴 → **새 프로젝트**
2. 프로젝트 정보 입력:
   - 프로젝트명, 설명
   - 시작일, 종료일
   - 예산
3. **저장** 클릭

### 리스크 관리하기

1. 프로젝트 상세 → **리스크** 탭
2. **새 리스크** 클릭
3. 리스크 정보 입력:
   - 리스크명, 설명
   - 발생 가능성 (1-5)
   - 영향도 (1-5)
   - 대응 계획
4. 리스크 매트릭스에서 시각적 확인

## 🏭 프로덕션 배포

### 1. 환경변수 설정

```bash
# .env 파일에서 보안 값 변경
export JWT_SECRET=$(openssl rand -base64 32)
export POSTGRES_PASSWORD=$(openssl rand -base64 24)
export NEO4J_PASSWORD=$(openssl rand -base64 24)
```

### 2. SSL 인증서 준비

```bash
# docker/nginx/ssl/ 디렉토리에 인증서 배치
cp your-cert.pem docker/nginx/ssl/cert.pem
cp your-key.pem docker/nginx/ssl/key.pem
```

### 3. 프로덕션 모드 시작

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 4. 헬스 체크

```bash
# Backend
curl http://localhost:8083/actuator/health

# LLM Service
curl http://localhost:8000/health

# Frontend (Nginx 사용 시)
curl http://localhost:80/health
```

## 📚 문서

자세한 문서는 다음을 참조하세요:

- [실행 가이드](실행가이드.md) - 상세 설치 및 실행 방법
- [빠른 시작 가이드](빠른_시작_가이드.md) - 5분 안에 시작하기
- [LLM 연동 가이드](LLM_연동_가이드.md) - AI 서비스 설정
- [RAG 시스템 가이드](RAG_시스템_가이드.md) - RAG 구성 및 최적화
- [Neo4j GraphRAG 가이드](NEO4J_GRAPHRAG_GUIDE.md) - GraphRAG 아키텍처
- [LangGraph 구현 가이드](LangGraph_구현_가이드.md) - 워크플로우 커스터마이징
- [Docker 가이드](README_DOCKER.md) - Docker 배포 가이드

## 🔧 트러블슈팅

### RAG 검색이 동작하지 않음
```bash
# Neo4j에 데이터가 인덱싱되었는지 확인
docker exec -it pms-neo4j cypher-shell -u neo4j -p pmspassword123
MATCH (d:Document) RETURN count(d);
MATCH (c:Chunk) RETURN count(c);

# 데이터가 없으면 재인덱싱
docker exec pms-llm-service python3 /app/load_ragdata_pdfs_neo4j.py --ragdata-dir /app/ragdata
```

### LLM 서비스가 느림
```bash
# GPU 사용 확인
docker exec pms-llm-service nvidia-smi

# GPU 레이어 수 조정 (docker-compose.yml)
# LLM_N_GPU_LAYERS: 50 → 30 (메모리 부족 시)
```

### 백엔드 연결 오류
```bash
# 데이터베이스 연결 확인
docker logs pms-backend | grep "database"

# PostgreSQL 상태 확인
docker exec pms-postgres pg_isready -U pms_user
```

## 📞 지원

GitHub Issues: [프로젝트 이슈 트래커]

---

**Built with ❤️ by PMS Insurance Claims Team**
