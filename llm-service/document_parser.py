"""
MinerU2.5-2509-1.2B 기반 고급 문서 파싱 모듈
Layout-aware document parsing with structure understanding
"""

import logging
from typing import List, Dict, Optional, Tuple
from dataclasses import dataclass
from enum import Enum
import re
import os
import json
from llama_cpp import Llama

logger = logging.getLogger(__name__)


class BlockType(Enum):
    """문서 블록 타입"""
    TITLE = "title"
    HEADING = "heading"
    PARAGRAPH = "paragraph"
    TABLE = "table"
    LIST = "list"
    FORMULA = "formula"
    CODE = "code"
    FOOTER = "footer"
    HEADER = "header"


@dataclass
class DocumentBlock:
    """문서 구조 블록"""
    type: BlockType
    content: str
    bbox: Optional[List[int]] = None  # [x1, y1, x2, y2]
    page: int = 1
    metadata: Optional[Dict] = None

    def __post_init__(self):
        if self.metadata is None:
            self.metadata = {}


class MinerUDocumentParser:
    """
    MinerU2.5-2509-1.2B 기반 문서 파서

    GGUF 모델을 llama-cpp-python으로 로드하여 실제 문서 구조 파싱 수행
    """

    def __init__(self, device: str = "cpu", use_mock: bool = False, model_path: str = None):
        """
        Args:
            device: 'cpu' 또는 'cuda'
            use_mock: True이면 MinerU 모델 대신 휴리스틱 파싱 사용
            model_path: MinerU GGUF 모델 경로 (기본값: ./models/MinerU2.5-2509-1.2B.i1-Q6_K.gguf)
        """
        self.device = device
        self.use_mock = use_mock
        self.model = None

        # 기본 모델 경로 설정
        if model_path is None:
            env_model_path = os.getenv("MINERU_MODEL_PATH")
            model_path = env_model_path or os.path.join(
                os.path.dirname(__file__),
                "models",
                "MinerU2.5-2509-1.2B.i1-Q6_K.gguf"
            )

        self.model_path = model_path

        if not use_mock:
            try:
                logger.info(f"Loading MinerU2.5 model from {model_path}...")

                # 모델 파일 존재 확인
                if not os.path.exists(model_path):
                    logger.error(f"Model file not found: {model_path}")
                    logger.warning("Falling back to heuristic-based parsing.")
                    self.use_mock = True
                    return

                # llama-cpp-python으로 GGUF 모델 로드
                self.model = Llama(
                    model_path=model_path,
                    n_ctx=4096,  # 문서 파싱을 위한 충분한 컨텍스트
                    n_threads=4,
                    verbose=False,
                    n_gpu_layers=0 if device == "cpu" else -1  # CPU 또는 GPU
                )

                logger.info("✅ MinerU2.5 model loaded successfully!")
                logger.info(f"   Model: {os.path.basename(model_path)}")
                logger.info(f"   Device: {device}")

            except Exception as e:
                logger.error(f"Failed to load MinerU model: {e}")
                logger.warning("Falling back to heuristic-based parsing.")
                self.use_mock = True
                self.model = None

    def parse_document(self, text: str, metadata: Optional[Dict] = None) -> List[DocumentBlock]:
        """
        문서를 구조적 블록으로 파싱

        Args:
            text: 문서 텍스트
            metadata: 문서 메타데이터

        Returns:
            DocumentBlock 리스트
        """
        if self.use_mock:
            return self._parse_with_heuristics(text, metadata)
        else:
            # 실제 MinerU 모델 사용 (구현 예정)
            return self._parse_with_mineru_model(text, metadata)

    def _parse_with_heuristics(self, text: str, metadata: Optional[Dict] = None) -> List[DocumentBlock]:
        """
        휴리스틱 기반 구조 파싱 (MinerU 시뮬레이션)
        실제로는 MinerU 모델이 이미지에서 직접 구조를 추출하지만,
        여기서는 텍스트 기반으로 구조를 추론합니다.
        """
        blocks = []
        lines = text.split('\n')
        current_paragraph = []

        for i, line in enumerate(lines):
            line = line.strip()

            if not line:
                # 빈 줄 - 이전 문단 종료
                if current_paragraph:
                    content = '\n'.join(current_paragraph)
                    blocks.append(self._create_block(content, i))
                    current_paragraph = []
                continue

            # 제목/헤딩 감지
            if self._is_title(line):
                if current_paragraph:
                    content = '\n'.join(current_paragraph)
                    blocks.append(self._create_block(content, i))
                    current_paragraph = []

                blocks.append(DocumentBlock(
                    type=BlockType.TITLE,
                    content=line,
                    page=1,
                    metadata={'line_number': i, 'is_major_heading': True}
                ))
                continue

            # 헤딩 감지 (부제목)
            if self._is_heading(line):
                if current_paragraph:
                    content = '\n'.join(current_paragraph)
                    blocks.append(self._create_block(content, i))
                    current_paragraph = []

                blocks.append(DocumentBlock(
                    type=BlockType.HEADING,
                    content=line,
                    page=1,
                    metadata={'line_number': i}
                ))
                continue

            # 표 감지 (간단한 휴리스틱)
            if self._is_table_line(line):
                if current_paragraph:
                    content = '\n'.join(current_paragraph)
                    blocks.append(self._create_block(content, i))
                    current_paragraph = []

                blocks.append(DocumentBlock(
                    type=BlockType.TABLE,
                    content=line,
                    page=1,
                    metadata={'line_number': i, 'is_structured_data': True}
                ))
                continue

            # 리스트 항목 감지
            if self._is_list_item(line):
                if current_paragraph:
                    content = '\n'.join(current_paragraph)
                    blocks.append(self._create_block(content, i))
                    current_paragraph = []

                blocks.append(DocumentBlock(
                    type=BlockType.LIST,
                    content=line,
                    page=1,
                    metadata={'line_number': i}
                ))
                continue

            # 일반 문단
            current_paragraph.append(line)

        # 마지막 문단 처리
        if current_paragraph:
            content = '\n'.join(current_paragraph)
            blocks.append(self._create_block(content, len(lines)))

        logger.info(f"Parsed document into {len(blocks)} blocks")
        return blocks

    def _create_block(self, content: str, line_number: int) -> DocumentBlock:
        """블록 생성 헬퍼"""
        return DocumentBlock(
            type=BlockType.PARAGRAPH,
            content=content,
            page=1,
            metadata={'line_number': line_number}
        )

    def _is_title(self, line: str) -> bool:
        """제목 여부 판단"""
        # 짧고, 콜론으로 끝나거나, 번호가 없고 중요한 키워드 포함
        if len(line) > 100:
            return False

        title_patterns = [
            r'^[0-9]+\.\s*.{3,50}$',  # "1. 제목"
            r'^[가-힣A-Za-z\s]{3,50}:$',  # "제목:"
            r'^[■□▪▫●○◆◇★☆]\s*.{3,50}$',  # "■ 제목"
            r'^#{1,3}\s+.+$',  # "# 제목" (마크다운)
        ]

        for pattern in title_patterns:
            if re.match(pattern, line):
                return True

        # 전체 대문자 or 키워드 포함
        if line.isupper() or any(kw in line for kw in ['프로젝트명', '목표', '개요', '구성', '단계']):
            if len(line) < 50:
                return True

        return False

    def _is_heading(self, line: str) -> bool:
        """헤딩(부제목) 여부 판단"""
        heading_patterns = [
            r'^[0-9]+\.[0-9]+\s+.+$',  # "1.1 헤딩"
            r'^[가-힣]+\s*\([0-9]+\)$',  # "개요 (1)"
            r'^\[[가-힣A-Za-z]+\]',  # "[카테고리]"
            r'^[▶▷►▸]\s+.+$',  # "▶ 항목"
        ]

        for pattern in heading_patterns:
            if re.match(pattern, line):
                return True

        return False

    def _is_table_line(self, line: str) -> bool:
        """표 라인 여부 판단"""
        # 파이프(|) 또는 탭으로 구분된 컬럼
        if '|' in line and line.count('|') >= 2:
            return True

        # 콜론(:)과 숫자가 많은 구조화된 데이터
        if ':' in line and any(char.isdigit() for char in line):
            return True

        return False

    def _is_list_item(self, line: str) -> bool:
        """리스트 항목 여부 판단"""
        list_patterns = [
            r'^[-•·※]\s+.+$',  # "- 항목"
            r'^[0-9]+\)\s+.+$',  # "1) 항목"
            r'^[a-zA-Z]\.\s+.+$',  # "a. 항목"
            r'^\*\s+.+$',  # "* 항목"
        ]

        for pattern in list_patterns:
            if re.match(pattern, line):
                return True

        return False

    def _parse_with_mineru_model(self, text: str, metadata: Optional[Dict] = None) -> List[DocumentBlock]:
        """
        실제 MinerU2.5 모델을 사용한 파싱

        MinerU는 문서 이미지 분석에 특화되어 있지만,
        GGUF 형식으로는 텍스트 기반 구조 분석 프롬프팅을 사용합니다.
        """
        if self.model is None:
            logger.warning("MinerU model not loaded. Falling back to heuristics.")
            return self._parse_with_heuristics(text, metadata)

        try:
            max_chars = int(os.getenv("MINERU_PARSE_MAX_CHARS", "12000"))
            chunk_tokens = int(os.getenv("MINERU_PARSE_CHUNK_TOKENS", "3000"))
            chunk_overlap = int(os.getenv("MINERU_PARSE_CHUNK_OVERLAP", "200"))

            if len(text) <= max_chars:
                parsed_blocks = self._parse_text_with_model(text)
            else:
                chunks = self._split_text_by_tokens(text, chunk_tokens, chunk_overlap)
                parsed_blocks = self._merge_blocks([
                    self._parse_text_with_model(chunk, chunk_index=idx)
                    for idx, chunk in enumerate(chunks)
                ])

            logger.info(f"MinerU parsed document into {len(parsed_blocks)} blocks")
            return parsed_blocks

        except Exception as e:
            logger.error(f"MinerU model parsing failed: {e}")
            logger.warning("Falling back to heuristic parsing.")
            return self._parse_with_heuristics(text, metadata)

    def _parse_text_with_model(self, text: str, chunk_index: int = 0) -> List[DocumentBlock]:
        """단일 텍스트 청크를 MinerU 모델로 파싱."""
        prompt = self._create_parsing_prompt(text)
        logger.debug("Sending chunk %d to MinerU for parsing (length: %d)", chunk_index, len(text))

        response = self.model(
            prompt,
            max_tokens=2048,
            temperature=0.1,
            top_p=0.9,
            stop=["</document>", "\n\n---\n\n"],
            echo=False
        )

        result_text = response['choices'][0]['text'].strip()
        logger.debug("MinerU response length for chunk %d: %d", chunk_index, len(result_text))

        blocks = self._parse_model_response(result_text, text)
        for block in blocks:
            block.metadata.setdefault("chunk_index", chunk_index)
        return blocks

    def _split_text_by_tokens(self, text: str, chunk_size: int, overlap: int) -> List[str]:
        """Llama 토크나이저 기반으로 텍스트를 청크 분할."""
        if not text:
            return []

        try:
            tokens = self.model.tokenize(text.encode("utf-8"))
        except Exception as exc:
            logger.warning("Tokenizer unavailable; falling back to char splits: %s", exc)
            return [text[i:i + chunk_size] for i in range(0, len(text), max(chunk_size - overlap, 1))]

        if not tokens:
            return []

        chunks: List[str] = []
        step = max(chunk_size - overlap, 1)
        for start in range(0, len(tokens), step):
            token_slice = tokens[start:start + chunk_size]
            if not token_slice:
                continue
            try:
                if hasattr(self.model, "detokenize"):
                    chunk_bytes = self.model.detokenize(token_slice)
                    chunk_text = chunk_bytes.decode("utf-8", errors="ignore")
                else:
                    raise AttributeError("detokenize not available")
            except Exception:
                ratio_start = int(len(text) * (start / len(tokens)))
                ratio_end = int(len(text) * (min(start + chunk_size, len(tokens)) / len(tokens)))
                chunk_text = text[ratio_start:ratio_end]
            if chunk_text.strip():
                chunks.append(chunk_text)

        return chunks

    def _merge_blocks(self, block_lists: List[List[DocumentBlock]]) -> List[DocumentBlock]:
        """청크별 파싱 결과를 평탄화."""
        merged: List[DocumentBlock] = []
        for blocks in block_lists:
            merged.extend(blocks)
        return merged

    def _create_parsing_prompt(self, text: str) -> str:
        """
        MinerU 모델용 문서 구조 분석 프롬프트 생성

        MinerU는 문서 파싱에 특화되어 있으므로,
        문서 구조 요소를 식별하도록 지시합니다.
        """
        prompt = f"""You are a document structure analyzer. Analyze the following document and identify structural elements.

For each element, identify its type and content:
- TITLE: Main section headings
- HEADING: Sub-section headings
- PARAGRAPH: Regular text paragraphs
- TABLE: Tabular data or structured information
- LIST: Bulleted or numbered lists
- FORMULA: Mathematical formulas or equations

Return your analysis in JSON format as an array of blocks:
[
  {{"type": "TITLE", "content": "...", "line": 1}},
  {{"type": "PARAGRAPH", "content": "...", "line": 5}},
  {{"type": "TABLE", "content": "...", "line": 10}}
]

Document to analyze:
---
{text}
---

Structural analysis (JSON):"""

        return prompt

    def _parse_model_response(self, response: str, original_text: str) -> List[DocumentBlock]:
        """
        MinerU 모델 응답을 DocumentBlock 리스트로 변환
        """
        blocks = []

        try:
            # JSON 파싱 시도
            # 응답에서 JSON 배열 추출
            json_start = response.find('[')
            json_end = response.rfind(']') + 1

            if json_start >= 0 and json_end > json_start:
                json_str = response[json_start:json_end]
                parsed_data = json.loads(json_str)

                for item in parsed_data:
                    try:
                        block_type_str = item.get('type', 'PARAGRAPH').upper()

                        # BlockType enum으로 변환
                        if block_type_str in [bt.name for bt in BlockType]:
                            block_type = BlockType[block_type_str]
                        else:
                            block_type = BlockType.PARAGRAPH

                        block = DocumentBlock(
                            type=block_type,
                            content=item.get('content', ''),
                            page=1,
                            metadata={
                                'line': item.get('line', 0),
                                'source': 'mineru_model',
                                'confidence': item.get('confidence', 1.0)
                            }
                        )
                        blocks.append(block)

                    except Exception as e:
                        logger.warning(f"Failed to parse block item: {e}")
                        continue

                if blocks:
                    logger.info(f"Successfully parsed {len(blocks)} blocks from MinerU response")
                    return blocks

            # JSON 파싱 실패 시 휴리스틱으로 대체
            logger.warning("Could not parse JSON from MinerU response. Using heuristic parsing.")

        except json.JSONDecodeError as e:
            logger.warning(f"JSON decode error: {e}. Using heuristic parsing.")
        except Exception as e:
            logger.error(f"Error parsing model response: {e}")

        # 파싱 실패 시 휴리스틱 파싱 사용
        return self._parse_with_heuristics(original_text, None)


class LayoutAwareChunker:
    """
    구조 인식 청킹 - MinerU 파싱 결과를 의미 단위로 청크화
    """

    def __init__(self, max_chunk_size: int = 1000, overlap: int = 100):
        """
        Args:
            max_chunk_size: 청크 최대 길이 (문자 수)
            overlap: 청크 간 중복 문자 수
        """
        self.max_chunk_size = max_chunk_size
        self.overlap = overlap

    def chunk_blocks(self, blocks: List[DocumentBlock]) -> List[Dict[str, any]]:
        """
        DocumentBlock 리스트를 의미 단위 청크로 변환

        핵심 원칙:
        1. 제목 + 관련 문단을 하나의 청크로
        2. 표는 독립적인 청크로
        3. 리스트는 가능한 한 함께 유지

        Returns:
            청크 딕셔너리 리스트
        """
        chunks = []
        current_chunk = []
        current_title = None
        current_size = 0

        for block in blocks:
            block_text = self._serialize_block(block)
            block_size = len(block_text)

            # 제목 블록 - 새로운 청크 시작
            if block.type == BlockType.TITLE:
                if current_chunk:
                    chunks.append(self._create_chunk(current_chunk, current_title))
                    current_chunk = []
                    current_size = 0

                current_title = block.content
                current_chunk.append(block)
                current_size += block_size

            # 표 블록 - 독립적인 청크로
            elif block.type == BlockType.TABLE:
                # 현재 청크 저장
                if current_chunk:
                    chunks.append(self._create_chunk(current_chunk, current_title))
                    current_chunk = []
                    current_size = 0

                # 표를 별도 청크로
                chunks.append(self._create_chunk([block], current_title))

            # 일반 블록 - 크기 제한 확인
            else:
                if current_size + block_size > self.max_chunk_size and current_chunk:
                    # 청크 저장
                    chunks.append(self._create_chunk(current_chunk, current_title))

                    # 새 청크 시작 (제목 유지)
                    current_chunk = []
                    current_size = 0

                    # 오버랩을 위해 이전 블록의 일부 포함 (선택적)
                    if len(current_chunk) > 0:
                        last_block = current_chunk[-1]
                        current_chunk = [last_block]
                        current_size = len(self._serialize_block(last_block))

                current_chunk.append(block)
                current_size += block_size

        # 마지막 청크 저장
        if current_chunk:
            chunks.append(self._create_chunk(current_chunk, current_title))

        logger.info(f"Created {len(chunks)} layout-aware chunks from {len(blocks)} blocks")
        return chunks

    def _serialize_block(self, block: DocumentBlock) -> str:
        """블록을 텍스트로 직렬화 (LLM이 이해할 수 있는 형식)"""
        if block.type == BlockType.TITLE:
            return f"[TITLE] {block.content}"
        elif block.type == BlockType.HEADING:
            return f"[HEADING] {block.content}"
        elif block.type == BlockType.TABLE:
            return f"[TABLE]\n{block.content}"
        elif block.type == BlockType.LIST:
            return f"[LIST] {block.content}"
        elif block.type == BlockType.FORMULA:
            return f"[FORMULA] {block.content}"
        else:
            return block.content

    def _create_chunk(self, blocks: List[DocumentBlock], title: Optional[str] = None) -> Dict[str, any]:
        """블록들을 하나의 청크로 변환"""
        serialized_parts = []

        # 제목 추가 (컨텍스트 제공)
        if title:
            serialized_parts.append(f"[CONTEXT] {title}")

        # 블록들 직렬화
        for block in blocks:
            serialized_parts.append(self._serialize_block(block))

        chunk_text = "\n\n".join(serialized_parts)

        # 메타데이터 수집
        block_types = [block.type.value for block in blocks]
        has_table = BlockType.TABLE.value in block_types
        has_list = BlockType.LIST.value in block_types

        return {
            'content': chunk_text,
            'metadata': {
                'title': title,
                'block_types': block_types,  # Qdrant는 리스트를 허용
                'has_table': has_table,
                'has_list': has_list,
                'block_count': len(blocks),
                'is_structured': has_table or has_list
            }
        }


def parse_and_chunk_document(text: str, metadata: Optional[Dict] = None) -> List[Dict[str, any]]:
    """
    문서를 파싱하고 의미 단위로 청킹하는 편의 함수

    Args:
        text: 문서 텍스트
        metadata: 문서 메타데이터

    Returns:
        청크 딕셔너리 리스트
    """
    parser = MinerUDocumentParser(use_mock=True)
    chunker = LayoutAwareChunker(max_chunk_size=1000, overlap=100)

    blocks = parser.parse_document(text, metadata)
    chunks = chunker.chunk_blocks(blocks)

    return chunks
