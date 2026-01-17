"""
Requirement Extractor Module
Extracts requirements from RFP documents using LLM and stores in Neo4j
"""

import json
import logging
import re
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, asdict

logger = logging.getLogger(__name__)


@dataclass
class ExtractedRequirement:
    """Extracted requirement from RFP document"""
    title: str
    description: str
    category: str  # FUNCTIONAL, NON_FUNCTIONAL, UI, INTEGRATION, SECURITY, AI, SI, COMMON
    priority: str  # CRITICAL, HIGH, MEDIUM, LOW
    source_text: str
    page_number: Optional[int] = None


class RequirementExtractor:
    """
    Extracts requirements from RFP documents using LLM
    """

    EXTRACTION_PROMPT = """다음 문서에서 요구사항을 추출해주세요.
각 요구사항에 대해 다음 정보를 JSON 배열 형식으로 제공해주세요:

{
  "requirements": [
    {
      "title": "요구사항 제목 (간결하게)",
      "description": "상세 설명",
      "category": "FUNCTIONAL | NON_FUNCTIONAL | UI | INTEGRATION | SECURITY | AI | SI | COMMON",
      "priority": "CRITICAL | HIGH | MEDIUM | LOW (문맥에서 추정)",
      "source_text": "원본 텍스트 발췌"
    }
  ]
}

카테고리 분류 기준:
- FUNCTIONAL: 시스템이 수행해야 하는 기능적 요구사항
- NON_FUNCTIONAL: 성능, 보안, 가용성 등 비기능적 요구사항
- UI: 사용자 인터페이스 관련 요구사항
- INTEGRATION: 외부 시스템 연동 요구사항
- SECURITY: 보안 관련 요구사항
- AI: AI/ML 관련 기능 요구사항
- SI: 시스템 통합 요구사항
- COMMON: 공통 모듈/기능 요구사항

우선순위 추정 기준:
- CRITICAL: "필수", "반드시", "핵심" 등의 표현이 있는 경우
- HIGH: "중요", "우선" 등의 표현이 있는 경우
- MEDIUM: 일반적인 요구사항
- LOW: "선택", "권장" 등의 표현이 있는 경우

문서 내용:
{document_text}

JSON 형식으로만 응답해주세요. 다른 설명은 필요 없습니다.
"""

    DEPENDENCY_PROMPT = """다음 요구사항들 간의 의존성을 분석해주세요.
어떤 요구사항이 다른 요구사항에 의존하는지 파악해주세요.

요구사항 목록:
{requirements}

JSON 배열 형식으로 의존성 관계를 출력해주세요:
{{
  "dependencies": [
    {{"from": "요구사항1 제목", "to": "요구사항2 제목", "type": "DEPENDS_ON"}}
  ]
}}

의존성 타입:
- DEPENDS_ON: from 요구사항이 to 요구사항에 의존함
- RELATED_TO: 관련성이 있음
- CONFLICTS_WITH: 상충됨

JSON 형식으로만 응답해주세요.
"""

    def __init__(self, llm_model, neo4j_service=None):
        """
        Initialize RequirementExtractor

        Args:
            llm_model: LLM model instance for text generation
            neo4j_service: Neo4j service for graph storage (optional)
        """
        self.llm = llm_model
        self.neo4j = neo4j_service

    def chunk_document(self, text: str, chunk_size: int = 4000, overlap: int = 200) -> List[str]:
        """
        Split document into chunks for processing

        Args:
            text: Document text
            chunk_size: Maximum chunk size
            overlap: Overlap between chunks

        Returns:
            List of text chunks
        """
        if len(text) <= chunk_size:
            return [text]

        chunks = []
        start = 0
        while start < len(text):
            end = start + chunk_size
            # Try to break at sentence boundary
            if end < len(text):
                # Look for sentence end
                for sep in ['. ', '.\n', '\n\n']:
                    last_sep = text[start:end].rfind(sep)
                    if last_sep > chunk_size // 2:
                        end = start + last_sep + len(sep)
                        break

            chunks.append(text[start:end].strip())
            start = end - overlap

        return chunks

    async def extract_requirements(
        self,
        document_text: str,
        rfp_id: str,
        project_id: str,
        tenant_id: str
    ) -> List[ExtractedRequirement]:
        """
        Extract requirements from RFP document

        Args:
            document_text: Full document text
            rfp_id: RFP identifier
            project_id: Project identifier
            tenant_id: Tenant identifier

        Returns:
            List of extracted requirements
        """
        logger.info(f"Starting requirement extraction for RFP: {rfp_id}")

        # Chunk document
        chunks = self.chunk_document(document_text)
        logger.info(f"Document split into {len(chunks)} chunks")

        all_requirements = []

        for i, chunk in enumerate(chunks):
            logger.info(f"Processing chunk {i + 1}/{len(chunks)}")

            try:
                prompt = self.EXTRACTION_PROMPT.format(document_text=chunk)
                result = await self.llm.generate(prompt)

                # Parse JSON response
                requirements = self._parse_requirements_response(result)
                all_requirements.extend(requirements)

            except Exception as e:
                logger.error(f"Failed to process chunk {i + 1}: {e}")
                continue

        # Remove duplicates
        unique_requirements = self._deduplicate_requirements(all_requirements)
        logger.info(f"Extracted {len(unique_requirements)} unique requirements")

        # Store in Neo4j if available
        if self.neo4j:
            await self._store_in_neo4j(unique_requirements, rfp_id, project_id, tenant_id)

        return unique_requirements

    def _parse_requirements_response(self, response: str) -> List[ExtractedRequirement]:
        """Parse LLM response to extract requirements"""
        requirements = []

        try:
            # Try to extract JSON from response
            json_match = re.search(r'\{[\s\S]*\}', response)
            if json_match:
                data = json.loads(json_match.group())
                req_list = data.get('requirements', [])

                for req in req_list:
                    requirements.append(ExtractedRequirement(
                        title=req.get('title', ''),
                        description=req.get('description', ''),
                        category=req.get('category', 'FUNCTIONAL'),
                        priority=req.get('priority', 'MEDIUM'),
                        source_text=req.get('source_text', ''),
                        page_number=req.get('page_number')
                    ))
        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse JSON response: {e}")

        return requirements

    def _deduplicate_requirements(
        self,
        requirements: List[ExtractedRequirement]
    ) -> List[ExtractedRequirement]:
        """Remove duplicate requirements based on title similarity"""
        unique = []
        seen_titles = set()

        for req in requirements:
            # Normalize title for comparison
            normalized = req.title.lower().strip()
            if normalized not in seen_titles:
                seen_titles.add(normalized)
                unique.append(req)

        return unique

    async def analyze_dependencies(
        self,
        requirements: List[ExtractedRequirement],
        tenant_id: str
    ) -> List[Dict[str, str]]:
        """
        Analyze dependencies between requirements

        Args:
            requirements: List of requirements
            tenant_id: Tenant identifier

        Returns:
            List of dependency relationships
        """
        if len(requirements) < 2:
            return []

        # Prepare requirements summary for LLM
        req_summary = json.dumps([
            {"title": r.title, "description": r.description}
            for r in requirements
        ], ensure_ascii=False, indent=2)

        try:
            prompt = self.DEPENDENCY_PROMPT.format(requirements=req_summary)
            result = await self.llm.generate(prompt)

            # Parse dependencies
            json_match = re.search(r'\{[\s\S]*\}', result)
            if json_match:
                data = json.loads(json_match.group())
                return data.get('dependencies', [])

        except Exception as e:
            logger.error(f"Failed to analyze dependencies: {e}")

        return []

    async def _store_in_neo4j(
        self,
        requirements: List[ExtractedRequirement],
        rfp_id: str,
        project_id: str,
        tenant_id: str
    ):
        """Store requirements in Neo4j graph database"""
        for i, req in enumerate(requirements):
            try:
                # Generate requirement code
                category_code = self._get_category_code(req.category)
                code = f"REQ-{project_id[:4].upper()}-{category_code}-{i+1:03d}"

                # Create requirement node
                await self.neo4j.create_requirement_node(
                    requirement_id=code,
                    data={
                        'code': code,
                        'title': req.title,
                        'description': req.description,
                        'category': req.category,
                        'priority': req.priority,
                        'source_text': req.source_text,
                        'status': 'IDENTIFIED',
                        'progress': 0
                    },
                    rfp_id=rfp_id,
                    tenant_id=tenant_id
                )
                logger.debug(f"Stored requirement: {code}")

            except Exception as e:
                logger.error(f"Failed to store requirement in Neo4j: {e}")

    def _get_category_code(self, category: str) -> str:
        """Get short code for category"""
        codes = {
            'FUNCTIONAL': 'FUNC',
            'NON_FUNCTIONAL': 'NFUNC',
            'UI': 'UI',
            'INTEGRATION': 'INT',
            'SECURITY': 'SEC',
            'AI': 'AI',
            'SI': 'SI',
            'COMMON': 'COM',
            'TECHNICAL': 'TECH',
            'BUSINESS': 'BIZ',
            'CONSTRAINT': 'CONS'
        }
        return codes.get(category, 'FUNC')


# Synchronous wrapper for non-async contexts
class RequirementExtractorSync:
    """Synchronous wrapper for RequirementExtractor"""

    def __init__(self, llm_model, neo4j_service=None):
        self.extractor = RequirementExtractor(llm_model, neo4j_service)

    def extract_requirements(
        self,
        document_text: str,
        rfp_id: str,
        project_id: str,
        tenant_id: str
    ) -> List[Dict[str, Any]]:
        """
        Synchronous requirement extraction

        Returns list of requirement dictionaries
        """
        import asyncio

        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)

        requirements = loop.run_until_complete(
            self.extractor.extract_requirements(
                document_text, rfp_id, project_id, tenant_id
            )
        )

        return [asdict(r) for r in requirements]
