"""
MinerU2.5 기반 RAG 시스템 초기화 스크립트
기존 벡터 DB를 초기화하고 개선된 파싱으로 데이터를 다시 저장
"""

import logging
from rag_service_v2 import RAGServiceV2
from init_rag_mock_data import MOCK_DOCUMENTS

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """메인 실행 함수"""
    logger.info("=" * 80)
    logger.info("MinerU2.5 기반 RAG 시스템 초기화")
    logger.info("=" * 80)

    try:
        # RAG 서비스 초기화
        logger.info("\n1. RAG 서비스 초기화...")
        rag_service = RAGServiceV2()

        # 기존 컬렉션 초기화
        logger.info("\n2. 기존 벡터 DB 초기화...")
        if rag_service.reset_collection():
            logger.info("✅ 벡터 DB가 초기화되었습니다.")
        else:
            logger.error("❌ 벡터 DB 초기화 실패")
            return

        # Mock 데이터 재처리
        logger.info(f"\n3. Mock 데이터 재처리 ({len(MOCK_DOCUMENTS)}개 문서)...")
        logger.info("   - MinerU 기반 구조 파싱 적용")
        logger.info("   - Layout-Aware Chunking 적용")

        success_count = rag_service.add_documents(MOCK_DOCUMENTS)

        logger.info(f"\n✅ {success_count}/{len(MOCK_DOCUMENTS)} 문서 처리 완료")

        # 통계 조회
        logger.info("\n4. 벡터 DB 통계 조회...")
        stats = rag_service.get_collection_stats()
        logger.info(f"""
컬렉션 통계:
- 컬렉션명: {stats.get('collection_name')}
- 전체 청크 수: {stats.get('total_chunks')}
- 구조화된 청크: {stats.get('structured_chunks')}
- 표 포함 청크: {stats.get('chunks_with_tables')}
- 리스트 포함 청크: {stats.get('chunks_with_lists')}
- 파서: {stats.get('parser')}
        """)

        # 검색 테스트
        logger.info("\n5. 검색 품질 테스트...")
        test_queries = [
            "현재 프로젝트 진행률은?",
            "OCR 모델의 정확도는?",
            "프로젝트 팀원은 누가 있나요?",
            "활성 이슈는 몇 건인가요?",
        ]

        for query in test_queries:
            logger.info(f"\n질문: {query}")
            results = rag_service.search(query, top_k=2)

            for i, result in enumerate(results, 1):
                logger.info(f"  결과 {i}:")
                logger.info(f"    - 관련성 점수: {result.get('relevance_score', 0):.3f}")
                logger.info(f"    - 거리: {result.get('distance', 0):.3f}")
                logger.info(f"    - 제목: {result.get('metadata', {}).get('title', 'N/A')}")
                logger.info(f"    - 구조화: {result.get('metadata', {}).get('is_structured', False)}")
                content_preview = result.get('content', '')[:150].replace('\n', ' ')
                logger.info(f"    - 내용: {content_preview}...")

        logger.info("\n" + "=" * 80)
        logger.info("✅ RAG 시스템 초기화 완료!")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"\n❌ 초기화 실패: {e}", exc_info=True)


if __name__ == "__main__":
    main()
