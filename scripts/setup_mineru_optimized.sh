#!/bin/bash
# setup_mineru_optimized.sh
# MinerU 최적화 환경 설정 스크립트
# Reference: docs/MinerU_최적화_방안.md

set -e

echo "=== MinerU 최적화 환경 설정 ==="
echo "Date: $(date)"
echo ""

# 1. 환경 변수 설정
export MINERU_DEVICE_MODE="${MINERU_DEVICE_MODE:-cuda}"
export MINERU_MODEL_SOURCE="${MINERU_MODEL_SOURCE:-local}"
export MINERU_BACKEND="${MINERU_BACKEND:-hybrid-auto-engine}"
export MINERU_VIRTUAL_VRAM_SIZE="${MINERU_VIRTUAL_VRAM_SIZE:-8}"

# OCR 파이프라인 설정
export USE_MINERU_OCR="${USE_MINERU_OCR:-true}"
export MINERU_OCR_ENGINE="${MINERU_OCR_ENGINE:-mineru_cli}"
export MINERU_OCR_METHOD="${MINERU_OCR_METHOD:-auto}"
export OCR_DPI="${OCR_DPI:-200}"
export OCR_LANG="${OCR_LANG:-kor+eng}"

echo "[1/5] 환경 변수 설정 완료"
echo "  MINERU_DEVICE_MODE: $MINERU_DEVICE_MODE"
echo "  MINERU_BACKEND: $MINERU_BACKEND"
echo "  MINERU_VIRTUAL_VRAM_SIZE: ${MINERU_VIRTUAL_VRAM_SIZE}GB"
echo ""

# 2. 설정 파일 위치 확인 및 생성
CONFIG_DIR="$HOME/.config/magic_pdf"
CONFIG_FILE="$CONFIG_DIR/magic-pdf.json"

if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
    CONFIG_DIR="$USERPROFILE"
    CONFIG_FILE="$CONFIG_DIR/magic-pdf.json"
fi

# 3. 설정 디렉토리 생성
mkdir -p "$CONFIG_DIR"

echo "[2/5] 설정 디렉토리 준비: $CONFIG_DIR"

# 4. 최적화된 설정 파일 생성
cat > "$CONFIG_FILE" << 'EOF'
{
  "config_version": "1.2.1",
  "device-mode": "cuda",
  "models-dir": "/root/.cache/models",
  "layoutreader-model-dir": "/root/.cache/layoutreader",
  
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
  },
  
  "latex-delimiter-config": {
    "display": { "left": "\\[", "right": "\\]" },
    "inline": { "left": "\\(", "right": "\\)" }
  }
}
EOF

echo "[3/5] 설정 파일 생성 완료: $CONFIG_FILE"
echo ""

# 5. GPU 상태 확인
echo "[4/5] GPU 상태 확인"
if command -v nvidia-smi &> /dev/null; then
    echo "=== NVIDIA GPU 정보 ==="
    nvidia-smi --query-gpu=name,memory.total,memory.free,driver_version --format=csv,noheader 2>/dev/null || echo "  GPU 정보 조회 실패"
    echo ""
    
    # VRAM 크기에 따른 권장 설정 출력
    VRAM_MB=$(nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits 2>/dev/null | head -1 | tr -d ' ')
    if [[ -n "$VRAM_MB" ]]; then
        VRAM_GB=$((VRAM_MB / 1024))
        echo "  총 VRAM: ${VRAM_GB}GB"
        
        if [[ $VRAM_GB -lt 8 ]]; then
            echo "  권장: Pipeline 백엔드 (VRAM < 8GB)"
            echo "  export MINERU_BACKEND=pipeline"
        elif [[ $VRAM_GB -lt 16 ]]; then
            echo "  권장: Hybrid 백엔드 (8-16GB VRAM)"
            echo "  현재 설정이 적합합니다."
        else
            echo "  권장: VLM 백엔드 또는 병렬 처리 (16GB+ VRAM)"
            echo "  export MINERU_BACKEND=vlm-vllm"
            echo "  export MINERU_VIRTUAL_VRAM_SIZE=8"
        fi
    fi
else
    echo "  nvidia-smi 명령어를 찾을 수 없습니다."
    echo "  CPU 모드로 실행됩니다."
    echo "  export MINERU_DEVICE_MODE=cpu"
    echo "  export MINERU_BACKEND=pipeline"
fi
echo ""

# 6. 설정 요약 출력
echo "[5/5] 최적화 설정 요약"
echo "=============================================="
echo "설정 파일: $CONFIG_FILE"
echo ""
echo "주요 설정값:"
echo "  - 레이아웃 모델: doclayout_yolo (10x 빠름)"
echo "  - 표 인식: rapid_table (max_time: 200ms)"
echo "  - 수식 인식: 비활성화 (필요시 활성화)"
echo "  - 백엔드: $MINERU_BACKEND"
echo ""
echo "환경 변수 (.env 또는 docker-compose.yml에 추가):"
echo "  MINERU_DEVICE_MODE=$MINERU_DEVICE_MODE"
echo "  MINERU_BACKEND=$MINERU_BACKEND"
echo "  MINERU_VIRTUAL_VRAM_SIZE=$MINERU_VIRTUAL_VRAM_SIZE"
echo "  USE_MINERU_OCR=true"
echo "  MINERU_OCR_ENGINE=mineru_cli"
echo "  MINERU_OCR_METHOD=auto"
echo "=============================================="
echo ""
echo "=== MinerU 최적화 설정 완료 ==="
