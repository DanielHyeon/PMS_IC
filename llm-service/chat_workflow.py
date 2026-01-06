"""
LangGraph 기반 채팅 워크플로우
RAG와 일반 LLM을 지능적으로 라우팅
"""

from typing import TypedDict, Literal, List, Optional, Union
from langgraph.graph import StateGraph, END
from llama_cpp import Llama
import logging
import re

# RAG 서비스 임포트 (타입 호환성)
try:
    from rag_service_qdrant import RAGServiceQdrant as RAGService
except ImportError:
    try:
        from rag_service import RAGService
    except ImportError:
        RAGService = None

logger = logging.getLogger(__name__)


# 상태 스키마 정의
class ChatState(TypedDict):
    """채팅 워크플로우 상태"""
    message: str  # 사용자 메시지
    context: List[dict]  # 대화 컨텍스트
    intent: Optional[str]  # 의도 분류 결과 (casual, pms_query, general)
    retrieved_docs: List[str]  # RAG 검색 결과
    response: Optional[str]  # 최종 응답
    confidence: float  # 응답 신뢰도
    debug_info: dict  # 디버깅 정보


class ChatWorkflow:
    """LangGraph 기반 채팅 워크플로우"""

    def __init__(self, llm: Llama, rag_service: Optional[RAGService] = None, model_path: Optional[str] = None):
        self.llm = llm
        self.rag_service = rag_service
        self.model_path = model_path
        self.graph = self._build_graph()

    def _build_graph(self) -> StateGraph:
        """워크플로우 그래프 구축"""

        # 그래프 초기화
        workflow = StateGraph(ChatState)

        # 노드 추가
        workflow.add_node("classify_intent", self.classify_intent_node)
        workflow.add_node("rag_search", self.rag_search_node)
        workflow.add_node("skip_rag", self.skip_rag_node)
        workflow.add_node("generate_response", self.generate_response_node)

        # 엔트리 포인트 설정
        workflow.set_entry_point("classify_intent")

        # 조건부 라우팅: 의도에 따라 분기
        workflow.add_conditional_edges(
            "classify_intent",
            self.route_by_intent,
            {
                "casual": "skip_rag",      # 일상 대화 → RAG 스킵
                "pms_query": "rag_search",  # PMS 관련 → RAG 검색
                "general": "rag_search"     # 일반 질문 → RAG 검색 (안전)
            }
        )

        # RAG 검색 후 응답 생성
        workflow.add_edge("rag_search", "generate_response")
        workflow.add_edge("skip_rag", "generate_response")

        # 응답 생성 후 종료
        workflow.add_edge("generate_response", END)

        return workflow.compile()

    def classify_intent_node(self, state: ChatState) -> ChatState:
        """노드 1: 의도 분류"""
        message = state["message"]

        logger.info(f"Classifying intent for message: {message[:50]}...")

        # 키워드 기반 분류 (빠름)
        intent = self._classify_with_keywords(message)

        # LLM 기반 분류가 필요한 경우 (애매한 경우)
        if intent == "uncertain":
            intent = self._classify_with_llm(message)

        state["intent"] = intent
        state["debug_info"] = state.get("debug_info", {})
        state["debug_info"]["intent"] = intent

        logger.info(f"Intent classified as: {intent}")

        return state

    def _classify_with_keywords(self, message: str) -> str:
        """키워드 기반 의도 분류"""
        message_lower = message.lower()

        # 일상 대화 패턴
        casual_patterns = [
            "안녕", "고마워", "감사", "미안", "죄송",
            "잘가", "반가", "수고", "ㅎㅎ", "ㅋㅋ", "ㄱㅅ"
        ]

        # PMS 관련 키워드
        pms_keywords = [
            "프로젝트", "일정", "계획", "산출물", "문서", "wbs",
            "리스크", "이슈", "마일스톤", "단계", "요구사항",
            "설계", "개발", "테스트", "보고서", "작업", "업무",
            "deliverable", "project", "task", "milestone"
        ]

        # 짧은 인사말 체크 (20자 미만)
        if len(message) < 20:
            for pattern in casual_patterns:
                if pattern in message_lower:
                    return "casual"

        # PMS 관련 키워드 체크
        for keyword in pms_keywords:
            if keyword in message_lower:
                return "pms_query"

        # 의문사가 있으면 일반 질문
        question_words = ["뭐", "무엇", "어디", "언제", "누가", "어떻게", "왜", "?", "무슨"]
        if any(word in message_lower for word in question_words):
            return "general"

        # 애매한 경우
        if len(message) < 30:
            return "uncertain"

        return "general"

    def _classify_with_llm(self, message: str) -> str:
        """LLM 기반 의도 분류 (키워드 분류 실패 시)"""

        prompt = f"""<start_of_turn>system
당신은 메시지 의도를 분류하는 AI입니다.
다음 카테고리 중 하나로 분류하세요:
- CASUAL: 일상적인 인사, 감사, 잡담
- PMS_QUERY: 프로젝트, 문서, 산출물, 업무 관련 질문
- GENERAL: 그 외 일반적인 질문

한 단어로만 답변하세요: CASUAL, PMS_QUERY, GENERAL
<end_of_turn>
<start_of_turn>user
메시지: {message}
<end_of_turn>
<start_of_turn>model
"""

        try:
            # KV 캐시 초기화
            self.llm.reset()

            response = self.llm(
                prompt,
                max_tokens=10,
                temperature=0.1,
                stop=["<end_of_turn>", "\n"],
                echo=False
            )

            intent_text = response["choices"][0]["text"].strip().upper()

            # 응답 파싱
            if "CASUAL" in intent_text:
                return "casual"
            elif "PMS_QUERY" in intent_text or "PMS" in intent_text:
                return "pms_query"
            else:
                return "general"

        except Exception as e:
            logger.error(f"LLM classification failed: {e}")
            return "general"  # 실패 시 안전하게 일반으로 처리

    def route_by_intent(self, state: ChatState) -> Literal["casual", "pms_query", "general"]:
        """의도에 따른 라우팅"""
        intent = state.get("intent", "general")
        logger.info(f"Routing to: {intent}")
        return intent

    def rag_search_node(self, state: ChatState) -> ChatState:
        """노드 2: RAG 검색"""
        message = state["message"]

        logger.info(f"Performing RAG search for: {message[:50]}...")

        # 요청에서 이미 문서가 전달된 경우, 검색 생략 (질문과의 간단한 일치 필터 적용)
        if state.get("retrieved_docs"):
            filtered_docs = self._filter_docs_by_query(message, state["retrieved_docs"])
            state["retrieved_docs"] = filtered_docs
            state["debug_info"]["rag_docs_count"] = len(state["retrieved_docs"])
            return state

        if self.rag_service:
            try:
                filter_metadata = None
                if state.get("intent") == "pms_query":
                    filter_metadata = {"type": "project"}

                results = self.rag_service.search(message, top_k=3, filter_metadata=filter_metadata)
                retrieved_docs = [doc['content'] for doc in results]
                retrieved_docs = self._filter_docs_by_query(message, retrieved_docs)

                state["retrieved_docs"] = retrieved_docs
                state["debug_info"]["rag_docs_count"] = len(retrieved_docs)

                logger.info(f"RAG search found {len(retrieved_docs)} documents")

            except Exception as e:
                logger.error(f"RAG search failed: {e}")
                state["retrieved_docs"] = []
                state["debug_info"]["rag_error"] = str(e)
        else:
            logger.warning("RAG service not available")
            state["retrieved_docs"] = []

        return state

    def skip_rag_node(self, state: ChatState) -> ChatState:
        """노드 3: RAG 스킵 (일상 대화)"""
        logger.info("Skipping RAG for casual conversation")

        state["retrieved_docs"] = []
        state["debug_info"]["rag_skipped"] = True

        return state

    def generate_response_node(self, state: ChatState) -> ChatState:
        """노드 4: 응답 생성"""
        message = state["message"]
        context = state.get("context", [])
        retrieved_docs = state.get("retrieved_docs", [])
        intent = state.get("intent", "general")

        logger.info(f"Generating response with {len(retrieved_docs)} RAG docs")

        # 프롬프트 구성
        prompt = self._build_prompt(message, context, retrieved_docs, intent)

        # 모델 이름 질문에 대한 사전 처리 (LLM 호출 전에 확인)
        original_message_lower = message.lower()
        is_model_name_question = any(keyword in original_message_lower for keyword in 
                                    ["모델", "model", "이름", "name", "너는", "당신은", "너의", "당신의"])
        
        logger.info(f"Checking model name question: message='{message}', is_model_name_question={is_model_name_question}")
        
        # 정확한 모델 이름 가져오기
        if self.model_path:
            import os
            model_file = os.path.basename(self.model_path)
            if "lfm2" in model_file.lower():
                correct_name = "Llama Forge Model 2 (LFM2)"
            elif "gemma" in model_file.lower():
                correct_name = "Gemma 3"
            else:
                correct_name = "로컬 LLM"
        else:
            correct_name = "로컬 LLM"
        
        # 모델 이름 질문인 경우 LLM 호출을 건너뛰고 직접 답변
        if is_model_name_question:
            logger.info(f"Model name question detected, returning direct answer: {correct_name}")
            reply = f"저는 {correct_name} 모델입니다."
        else:
            try:
                # KV 캐시 초기화
                self.llm.reset()

                # LLM 추론
                response = self.llm(
                    prompt,
                    max_tokens=8182,
                    temperature=0.7,
                    top_p=0.9,
                    stop=["<end_of_turn>", "<start_of_turn>", "</s>", "<|im_end|>"],
                    echo=False,
                    repeat_penalty=1.1
                )

                reply = response["choices"][0]["text"].strip()

                # 원본 응답 로깅 (디버깅용)
                logger.info(f"Raw model response: {repr(reply)}")

                # 후처리
                reply = self._clean_response(reply)

                # 클리닝 후 응답 로깅
                logger.info(f"Cleaned response: {repr(reply)}")
                
                # 잘못된 모델 이름이 포함되어 있는지 추가 검증
                wrong_names = ["니콜라스", "nicolas", "알렉스", "alex", "사라", "sara", 
                              "gpt-4", "chatgpt", "claude", "gemini", "palm"]
                reply_lower_check = reply.lower()
                has_wrong_name = any(wrong in reply_lower_check for wrong in wrong_names)
                
                if has_wrong_name:
                    logger.warning(f"Detected wrong model name in response, replacing with: {correct_name}")
                    reply = f"저는 {correct_name} 모델입니다."
            except Exception as e:
                logger.error(f"Response generation failed: {e}")
                reply = "죄송합니다. 응답 생성 중 오류가 발생했습니다."

        # 신뢰도 계산
        confidence = self._calculate_confidence(intent, retrieved_docs)

        state["response"] = reply
        state["confidence"] = confidence
        state["debug_info"]["prompt_length"] = len(prompt)

        logger.info(f"Response generated: {reply[:50]}... (confidence: {confidence})")

        return state

    def _filter_docs_by_query(self, message: str, retrieved_docs: List[str]) -> List[str]:
        """질문과 직접 관련된 문서만 남기는 간단한 필터"""
        if not retrieved_docs:
            return []

        stopwords = {
            "프로젝트", "대해", "알려줘", "알려", "해주세요", "해줘",
            "설명", "정보", "현황에", "현황을", "현황은"
        }

        suffixes = ["에서", "에게", "부터", "까지", "으로", "으로써", "으로서",
                    "으로써", "으로", "에서", "으로", "과", "와", "을", "를", "이", "가",
                    "에", "의", "도", "만", "은", "는", "께"]

        tokens = []
        for raw in message.split():
            token = raw.strip(".,!?;:()[]{}\"'").lower()
            if len(token) < 2:
                continue
            for suffix in suffixes:
                if token.endswith(suffix) and len(token) > len(suffix):
                    token = token[: -len(suffix)]
                    break
            if not token or token in stopwords:
                continue
            if len(token) >= 2:
                tokens.append(token)

        if not tokens:
            return []

        filtered = []
        for doc in retrieved_docs:
            doc_text = (doc or "").lower()
            if any(token in doc_text for token in tokens):
                filtered.append(doc)

        return filtered

    def _build_prompt(self, message: str, context: List[dict],
                     retrieved_docs: List[str], intent: str) -> str:
        """프롬프트 구성"""

        prompt_parts = []

        tools_json_schema = "없음"
        
        # 현재 사용 중인 모델 정보 가져오기
        model_name = "로컬 LLM"
        if self.model_path:
            # 파일명에서 모델 이름 추출
            import os
            model_file = os.path.basename(self.model_path)
            if "gemma" in model_file.lower():
                model_name = "Gemma 3"
            elif "lfm2" in model_file.lower():
                model_name = "Llama Forge Model 2 (LFM2)"
            elif "llama" in model_file.lower():
                model_name = "Llama 기반 모델"
            else:
                model_name = "로컬 LLM"
        
        system_prompt = f"""당신은 친절하고 도움이 되는 한국어 AI 어시스턴트입니다.
항상 한국어로 자연스럽게 답변하세요.

사용자의 질문에는 짧지 않게 3~6문장으로 답변하세요.
핵심 정의 → 목적/배경 → 간단한 예시 순서로 설명하세요.
모르는 내용은 솔직하게 "모르겠습니다"라고 말하세요."""

        # LFM2 모델은 <|im_start|>와 <|im_end|> 토큰 사용
        prompt_parts.append("<|im_start|>system")
        prompt_parts.append(system_prompt)
        prompt_parts.append("<|im_end|>")

        # 컨텍스트 메시지 (최근 5개)
        for msg in context[-5:]:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role == "user":
                prompt_parts.append("<|im_start|>user")
                prompt_parts.append(content)
                prompt_parts.append("<|im_end|>")
            elif role == "assistant":
                prompt_parts.append("<|im_start|>assistant")
                prompt_parts.append(content)
                prompt_parts.append("<|im_end|>")

        # 현재 질문과 RAG 문서
        prompt_parts.append("<|im_start|>user")

        if retrieved_docs and len(retrieved_docs) > 0:
            prompt_parts.append(message)
            prompt_parts.append("\n관련 문서:")
            for i, doc in enumerate(retrieved_docs, 1):
                doc_content = doc if isinstance(doc, str) else doc.get('content', str(doc))
                prompt_parts.append(f"{i}. {doc_content}")
        else:
            prompt_parts.append(message)

        prompt_parts.append("<|im_end|>")
        prompt_parts.append("<|im_start|>assistant")

        return "\n".join(prompt_parts)

    def _clean_response(self, reply: str) -> str:
        """응답 정리"""

        # Gemma 특수 토큰 제거
        reply = reply.replace("<start_of_turn>", "").replace("<end_of_turn>", "")
        # im_end 토큰 제거 (깨지는 문자 방지)
        reply = reply.replace("<|im_end|>", "").replace("|im_end|>", "").replace("<|im_end", "")

        # 삼중 따옴표로 감싸진 블록 제거 (모델 이름, 구분선, 질문 등 포함)
        reply = re.sub(r"'''[\s\S]*?'''", "", reply)
        reply = re.sub(r'"""[\s\S]*?"""', "", reply)
        
        # 모델 이름과 구분선이 포함된 앞부분 제거
        # 예: "Llama Forge Model 2 (LFM2)\n===\n질문내용"
        reply = re.sub(r"^.*?(Llama Forge Model|Gemma|LFM2|로컬 LLM).*?\n=+\n.*?\n", "", reply, flags=re.MULTILINE | re.IGNORECASE)
        reply = re.sub(r"^.*?=+\n.*?\n", "", reply, flags=re.MULTILINE)
        
        # 불필요한 role 레이블 제거
        if reply.startswith("model"):
            reply = reply[5:].strip()
        if reply.startswith("assistant"):
            reply = reply[9:].strip()

        # 프롬프트 형식 태그 제거
        reply = reply.replace("<think>", "")
        reply = reply.replace("system", "")
        reply = reply.replace("사용자:", "")
        reply = reply.replace("user:", "")
        reply = reply.replace("USER", "")
        reply = reply.replace("_assistant", "")
        reply = reply.replace("assistant", "")
        
        # "현재 질문에 대한 답변을 작성해 주세요" 같은 프롬프트 텍스트 제거
        unwanted_patterns = [
            "현재 질문에 대한 답변을 작성해 주세요",
            "현재 질문에 대한 답변을 작성해주세요",
            "답변을 작성해 주세요",
            "답변을 작성해주세요",
            "Please write an answer",
            "Write an answer",
        ]
        
        # 메타 설명 텍스트 제거 (뒤에 붙는 불필요한 설명)
        meta_patterns = [
            r"제공된 정보로.*?완벽하게 답변했습니다.*?",
            r"제공된 정보로.*?답변했습니다.*?",
            r"이제 사용자님의 요청대로.*?제공",
            r"이제 사용자의 요청대로.*?제공",
            r"사용자님의 요청대로.*?설명.*?제공",
            r"사용자의 요청대로.*?설명.*?제공",
            r"요청대로.*?한국어로.*?제공",
            r"요청하신.*?한국어로.*?제공",
        ]
        for pattern in meta_patterns:
            reply = re.sub(pattern, "", reply, flags=re.IGNORECASE | re.DOTALL)
        
        # 잘못된 모델 이름 필터링 (모델 이름 질문인 경우)
        wrong_model_names = ["니콜라스", "nicolas", "알렉스", "alex", "사라", "sara", 
                            "gpt-4", "chatgpt", "claude", "gemini", "palm", "gpt4"]
        
        # 정확한 모델 이름 가져오기
        correct_name = "로컬 LLM"
        if self.model_path:
            import os
            model_file = os.path.basename(self.model_path)
            if "lfm2" in model_file.lower():
                correct_name = "Llama Forge Model 2 (LFM2)"
            elif "gemma" in model_file.lower():
                correct_name = "Gemma 3"
        
        # 잘못된 모델 이름이 포함된 경우 강제로 교체
        reply_lower = reply.lower()
        found_wrong_name = False
        for wrong_name in wrong_model_names:
            if wrong_name.lower() in reply_lower:
                found_wrong_name = True
                # 정확한 모델 이름으로 완전히 교체
                reply = f"저는 {correct_name} 모델입니다."
                break
        
        # 모델 이름 관련 키워드가 있고, 정확한 모델 이름이 없는 경우
        model_keywords = ["모델", "model", "이름", "name", "너는", "당신은", "너의", "당신의"]
        has_model_keyword = any(keyword in reply_lower for keyword in model_keywords)
        has_correct_name = any(correct in reply for correct in ["Llama", "Gemma", "로컬 LLM", "LFM2"])
        
        if has_model_keyword and not has_correct_name:
            # 잘못된 답변이 나온 경우 강제 교체
            reply = f"저는 {correct_name} 모델입니다."
        for pattern in unwanted_patterns:
            reply = reply.replace(pattern, "")
            # 대소문자 구분 없이 제거
            reply = re.sub(re.escape(pattern), "", reply, flags=re.IGNORECASE)

        # assistant 접두어 정리
        cleaned_lines = []
        for line in reply.splitlines():
            stripped = line.strip()
            lower = stripped.lower()
            
            # 모델 이름과 구분선이 포함된 줄 제거
            if re.search(r"(llama forge model|gemma|lfm2|로컬 llm).*?===", lower) or re.search(r"^=+$", stripped):
                stripped = ""
            # 삼중 따옴표로 시작하거나 끝나는 줄 제거
            elif stripped.startswith("'''") or stripped.endswith("'''") or stripped.startswith('"""') or stripped.endswith('"""'):
                stripped = ""
            # 불필요한 패턴 제거
            elif lower.startswith("assistant:") or lower.startswith("assistant："):
                stripped = stripped.split(":", 1)[1].strip() if ":" in stripped else ""
            elif lower == "assistant" or lower == "system" or lower == "user":
                stripped = ""
            elif stripped.startswith("사용자:") or stripped.startswith("사용자："):
                stripped = ""
            elif stripped.startswith("system") or stripped.startswith("user"):
                stripped = ""
            elif "<think>" in stripped.lower():
                stripped = ""
            elif any(pattern in stripped for pattern in unwanted_patterns):
                stripped = ""
            # 메타 설명 텍스트가 포함된 줄 제거
            elif re.search(r"제공된 정보로.*?답변했습니다", lower) or re.search(r"이제 사용자.*?요청대로", lower) or re.search(r"요청.*?한국어로.*?제공", lower):
                stripped = ""
            
            if stripped:
                cleaned_lines.append(stripped)
        
        if cleaned_lines:
            reply = "\n".join(cleaned_lines)
        else:
            # 모든 줄이 제거된 경우 원본에서 첫 번째 의미있는 줄만 사용
            lines = reply.splitlines()
            for line in lines:
                stripped = line.strip()
                if stripped and not any(unwanted in stripped.lower() for unwanted in ["system", "user", "assistant", "사용자", "<redacted"]):
                    reply = stripped
                    break

        # 응답 앞부분에서 모델 이름과 구분선 제거
        # 예: "Llama Forge Model 2 (LFM2)\n===\n질문내용\n\n답변내용" -> "답변내용"
        lines = reply.splitlines()
        start_idx = 0
        for i, line in enumerate(lines):
            stripped = line.strip()
            # 모델 이름이나 구분선이 있는 줄은 건너뛰기
            if re.search(r"(llama forge model|gemma|lfm2|로컬 llm)", stripped, re.IGNORECASE) or re.match(r"^=+$", stripped):
                start_idx = i + 1
            # 사용자 질문처럼 보이는 줄도 건너뛰기 (질문으로 끝나는 경우)
            elif (stripped.endswith("?") or stripped.endswith("주세요") or stripped.endswith("해주세요")) and i < len(lines) - 1:
                start_idx = i + 1
            else:
                break
        
        if start_idx > 0:
            reply = "\n".join(lines[start_idx:]).strip()
        
        # 응답 뒷부분에서 메타 설명 제거
        lines = reply.splitlines()
        end_idx = len(lines)
        for i in range(len(lines) - 1, -1, -1):
            line = lines[i].strip()
            # 메타 설명 패턴이 있으면 그 줄부터 끝까지 제거
            if re.search(r"제공된 정보로.*?답변했습니다", line, re.IGNORECASE) or \
               re.search(r"이제 사용자.*?요청대로", line, re.IGNORECASE) or \
               re.search(r"요청.*?한국어로.*?제공", line, re.IGNORECASE) or \
               re.search(r"완벽하게 답변했습니다", line, re.IGNORECASE):
                end_idx = i
                break
        
        if end_idx < len(lines):
            reply = "\n".join(lines[:end_idx]).strip()

        # 중복 응답 방지
        if "<start_of_turn>" in reply:
            reply = reply.split("<start_of_turn>")[0].strip()
        
        # im_end 토큰이 남아있으면 제거
        if "<|im_end|>" in reply:
            reply = reply.split("<|im_end|>")[0].strip()
        if "|im_end|>" in reply:
            reply = reply.split("|im_end|>")[0].strip()

        # 과도하게 긴 응답 제한
        if "\n\n\n" in reply:
            reply = reply.split("\n\n\n")[0].strip()
        
        # 제어 문자 및 깨지는 문자 제거 (인코딩 문제 방지)
        import string
        # 인쇄 가능한 문자와 공백만 유지
        printable_chars = set(string.printable)
        # 한글, 한자, 일본어 등 유니코드 문자도 허용
        cleaned_chars = []
        for char in reply:
            # 인쇄 가능한 ASCII 문자이거나, 유니코드 문자(한글 등)인 경우만 유지
            if char in printable_chars or ord(char) > 127:
                # 제어 문자 제거 (탭, 줄바꿈, 캐리지 리턴은 유지)
                if ord(char) < 32 and char not in ['\n', '\r', '\t']:
                    continue
                cleaned_chars.append(char)
        reply = ''.join(cleaned_chars)
        
        # 앞뒤 공백 정리
        reply = reply.strip()
        
        # 응답 끝에 남은 불완전한 태그나 특수 문자 제거
        # 예: "<", "<start", "<end", "<|" 등
        while reply and reply[-1] in ['<', '>', '|']:
            reply = reply[:-1].strip()
        
        # 불완전한 태그 패턴 제거 (끝부분에 남은 것들)
        reply = re.sub(r'<[^>]*$', '', reply)  # 끝에 불완전한 태그 제거
        reply = re.sub(r'\|[^>]*$', '', reply)  # 끝에 불완전한 토큰 제거
        
        # 다시 앞뒤 공백 정리
        reply = reply.strip()

        return reply

    def _calculate_confidence(self, intent: str, retrieved_docs: List[str]) -> float:
        """신뢰도 계산"""

        base_confidence = {
            "casual": 0.95,      # 일상 대화는 높은 신뢰도
            "pms_query": 0.70,   # PMS 질문은 RAG 의존
            "general": 0.80      # 일반 질문은 중간
        }.get(intent, 0.75)

        # RAG 문서가 있으면 신뢰도 증가
        if retrieved_docs and len(retrieved_docs) > 0:
            rag_boost = min(0.15, len(retrieved_docs) * 0.05)
            base_confidence = min(0.95, base_confidence + rag_boost)

        return round(base_confidence, 2)

    def run(self, message: str, context: List[dict] = None, retrieved_docs: List[str] = None) -> dict:
        """워크플로우 실행"""

        initial_state: ChatState = {
            "message": message,
            "context": context or [],
            "intent": None,
            "retrieved_docs": retrieved_docs or [],
            "response": None,
            "confidence": 0.0,
            "debug_info": {}
        }

        logger.info(f"Starting workflow for message: {message[:50]}...")

        # 그래프 실행
        final_state = self.graph.invoke(initial_state)

        logger.info(f"Workflow completed. Intent: {final_state.get('intent')}, "
                   f"RAG docs: {len(final_state.get('retrieved_docs', []))}")

        return {
            "reply": final_state.get("response", "응답을 생성할 수 없습니다."),
            "confidence": final_state.get("confidence", 0.0),
            "intent": final_state.get("intent"),
            "rag_docs_count": len(final_state.get("retrieved_docs", [])),
            "debug_info": final_state.get("debug_info", {})
        }
