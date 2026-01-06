"""
보험금지급심사 PDF를 벡터 DB에 추가하는 스크립트
"""

import os
import sys
from rag_service import RAGService
import logging

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)

def main():
    try:
        # RAG 서비스 초기화
        logger.info("Initializing RAG service...")
        rag_service = RAGService()

        # PDF 파일 경로
        pdf_path = "/app/ragdata/보험금지급심사 AI기반 수행 단계별 절차와 방법론.pdf"

        # 파일 존재 확인
        if not os.path.exists(pdf_path):
            logger.error(f"PDF file not found: {pdf_path}")
            return False

        logger.info(f"Processing PDF: {pdf_path}")

        # PDF에서 텍스트 추출
        logger.info("Extracting text from PDF...")
        text_content = rag_service.extract_text_from_file(pdf_path, 'pdf')

        if not text_content:
            logger.error("Failed to extract text from PDF")
            return False

        logger.info(f"Extracted {len(text_content)} characters from PDF")

        # 문서 메타데이터 설정
        document = {
            'id': 'insurance_claim_review_methodology',
            'content': text_content,
            'metadata': {
                'title': '보험금지급심사 AI기반 수행 단계별 절차와 방법론',
                'file_name': '보험금지급심사 AI기반 수행 단계별 절차와 방법론.pdf',
                'file_type': 'pdf',
                'category': 'methodology',
                'source': 'ragdata',
                'description': '보험금 지급 심사를 위한 AI 기반 수행 절차 및 방법론 문서'
            }
        }

        # 벡터 DB에 추가
        logger.info("Adding document to vector DB...")
        success = rag_service.add_document(document)

        if success:
            logger.info("✓ PDF successfully added to vector DB")

            # 통계 확인
            stats = rag_service.get_collection_stats()
            logger.info(f"Collection stats: {stats}")

            # 테스트 검색
            logger.info("\nTesting search with sample queries...")
            test_queries = [
                "보험금 지급 심사 절차",
                "AI 기반 심사 방법",
                "단계별 절차"
            ]

            for query in test_queries:
                results = rag_service.search(query, top_k=2)
                logger.info(f"\nQuery: {query}")
                logger.info(f"Found {len(results)} results")
                if results:
                    logger.info(f"Top result preview: {results[0]['content'][:100]}...")

            return True
        else:
            logger.error("Failed to add PDF to vector DB")
            return False

    except Exception as e:
        logger.error(f"Error: {e}", exc_info=True)
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
