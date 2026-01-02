"""
로컬 LLM 서비스 (GGUF 모델 사용)
llama-cpp-python을 사용하여 GGUF 모델을 실행합니다.
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
from llama_cpp import Llama
import os
import logging

app = Flask(__name__)
CORS(app)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 모델 경로 설정
MODEL_PATH = os.getenv("MODEL_PATH", "./models/LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf")
MAX_TOKENS = int(os.getenv("MAX_TOKENS", "512"))
TEMPERATURE = float(os.getenv("TEMPERATURE", "0.7"))
TOP_P = float(os.getenv("TOP_P", "0.9"))

# 전역 모델 인스턴스
llm = None

def load_model():
    """모델 로드"""
    global llm
    if llm is None:
        logger.info(f"Loading model from {MODEL_PATH}")
        try:
            llm = Llama(
                model_path=MODEL_PATH,
                n_ctx=2048,  # 컨텍스트 길이
                n_threads=4,  # CPU 스레드 수
                verbose=False
            )
            logger.info("Model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise
    return llm

@app.route("/health", methods=["GET"])
def health():
    """헬스 체크"""
    return jsonify({"status": "healthy", "model_loaded": llm is not None})

@app.route("/api/chat", methods=["POST"])
def chat():
    """채팅 요청 처리"""
    try:
        data = request.json
        message = data.get("message", "")
        context = data.get("context", [])
        
        if not message:
            return jsonify({"error": "Message is required"}), 400
        
        # 모델 로드
        model = load_model()
        
        # 컨텍스트를 프롬프트로 변환
        prompt = build_prompt(message, context)
        
        # 모델 추론
        response = model(
            prompt,
            max_tokens=MAX_TOKENS,
            temperature=TEMPERATURE,
            top_p=TOP_P,
            stop=["</s>", "\n\n\n"],
            echo=False
        )
        
        reply = response["choices"][0]["text"].strip()
        
        return jsonify({
            "reply": reply,
            "confidence": 0.85,
            "suggestions": []
        })
        
    except Exception as e:
        logger.error(f"Error processing chat request: {e}", exc_info=True)
        return jsonify({
            "error": "Failed to process chat request",
            "message": str(e)
        }), 500

def build_prompt(message: str, context: list) -> str:
    """대화 컨텍스트를 프롬프트로 변환"""
    prompt_parts = []
    
    # 시스템 프롬프트
    system_prompt = "You are a helpful AI assistant for a project management system. Provide clear and concise answers."
    prompt_parts.append(f"System: {system_prompt}")
    
    # 컨텍스트 메시지 추가
    for msg in context[-5:]:  # 최근 5개 메시지만 사용
        role = msg.get("role", "user")
        content = msg.get("content", "")
        if role == "user":
            prompt_parts.append(f"User: {content}")
        elif role == "assistant":
            prompt_parts.append(f"Assistant: {content}")
    
    # 현재 메시지 추가
    prompt_parts.append(f"User: {message}")
    prompt_parts.append("Assistant:")
    
    return "\n".join(prompt_parts)

if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    app.run(host="0.0.0.0", port=port, debug=False)

