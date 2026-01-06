"""
MinerU2.5 실제 모델을 사용한 RAG 시스템 초기화
"""

import logging
import os
import sys

# 환경변수 설정 - MinerU 모델 사용
os.environ["USE_MINERU_MODEL"] = "true"

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
    logger.info("MinerU2.5 실제 모델 기반 RAG 시스템 초기화")
    logger.info("=" * 80)

    try:
        # 1. RAG 서비스 초기화
        logger.info("\n1️⃣  RAG 서비스 초기화 (MinerU 모델 로드)...")
        rag_service = RAGServiceV2()

        # MinerU 모델 로드 확인
        if rag_service.parser.model is None:
            logger.warning("\n⚠️  MinerU 모델이 로드되지 않았습니다.")
            logger.warning("   휴리스틱 파싱 모드로 실행됩니다.")
            logger.info("\n계속하시겠습니까? (y/n)")

            # Docker 환경에서는 자동으로 계속
            if os.getenv("DOCKER_ENV"):
                logger.info("   Docker 환경 - 자동으로 계속합니다.")
            else:
                response = input().lower()
                if response != 'y':
                    logger.info("초기화 취소됨")
                    return

        else:
            logger.info("✅ MinerU2.5 모델이 성공적으로 로드되었습니다!")
            logger.info(f"   모델: {os.path.basename(rag_service.parser.model_path)}")

        # 2. 기존 컬렉션 초기화
        logger.info("\n2️⃣  기존 벡터 DB 초기화...")
        if rag_service.reset_collection():
            logger.info("✅ 벡터 DB가 초기화되었습니다.")
        else:
            logger.error("❌ 벡터 DB 초기화 실패")
            return

        # 3. Mock 데이터 재처리
        logger.info(f"\n3️⃣  Mock 데이터 재처리 ({len(MOCK_DOCUMENTS)}개 문서)...")
        logger.info("   - MinerU2.5 모델 기반 구조 파싱 적용")
        logger.info("   - Layout-Aware Chunking 적용")

        success_count = rag_service.add_documents(MOCK_DOCUMENTS)

        logger.info(f"\n✅ {success_count}/{len(MOCK_DOCUMENTS)} 문서 처리 완료")

        # 4. 통계 조회
        logger.info("\n4️⃣  벡터 DB 통계 조회...")
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

        # 5. 검색 품질 테스트
        logger.info("\n5️⃣  검색 품질 테스트...")
        test_queries = [
            "현재 프로젝트 진행률은?",
            "OCR 모델의 정확도는?",
            "프로젝트 팀원은 누가 있나요?",
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

                # 모델 소스 표시
                source = result.get('metadata', {}).get('source', 'unknown')
                if source == 'mineru_model':
                    logger.info(f"    - 파싱 소스: 🤖 MinerU 모델")
                else:
                    logger.info(f"    - 파싱 소스: 📐 휴리스틱")

                content_preview = result.get('content', '')[:150].replace('\n', ' ')
                logger.info(f"    - 내용: {content_preview}...")

        logger.info("\n" + "=" * 80)
        logger.info("✅ RAG 시스템 초기화 완료!")
        logger.info("=" * 80)

        # 6. 사용 안내
        logger.info("\n📘 사용 방법:")
        logger.info("1. 프론트엔드에서 AI 챗봇 테스트")
        logger.info("2. API 직접 호출:")
        logger.info("   curl -X POST http://localhost:8000/api/documents/search \\")
        logger.info("     -H 'Content-Type: application/json' \\")
        logger.info("     -d '{\"query\": \"현재 프로젝트 진행률은?\", \"top_k\": 3}'")

    except Exception as e:
        logger.error(f"\n❌ 초기화 실패: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
