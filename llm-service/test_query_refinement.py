"""
쿼리 개선 기능 테스트 스크립트
맞춤법 오류가 있는 쿼리를 자동으로 교정하는지 확인
"""

import logging
import sys

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def test_query_refinement():
    """쿼리 개선 기능 테스트"""

    try:
        # 모듈 임포트
        from chat_workflow import ChatWorkflow
        from llama_cpp import Llama
        from rag_service_neo4j import RAGServiceNeo4j

        logger.info("=" * 80)
        logger.info("쿼리 개선 기능 테스트 시작")
        logger.info("=" * 80)

        # RAG 서비스 로드
        logger.info("RAG 서비스 로딩...")
        rag_service = RAGServiceNeo4j()
        logger.info("✅ RAG 서비스 로드 완료")

        # LLM 모델 로드 (테스트이므로 간단한 설정)
        logger.info("LLM 모델 로딩...")
        model_path = "./models/google.gemma-3-12b-pt.Q5_K_M.gguf"

        try:
            llm = Llama(
                model_path=model_path,
                n_ctx=2048,
                n_threads=4,
                verbose=False,
                n_gpu_layers=0
            )
            logger.info("✅ LLM 모델 로드 완료")
        except Exception as e:
            logger.error(f"LLM 모델 로드 실패 (테스트는 계속 진행): {e}")
            llm = None

        # 워크플로우 초기화
        logger.info("워크플로우 초기화...")
        workflow = ChatWorkflow(llm, rag_service, model_path=model_path)
        logger.info("✅ 워크플로우 초기화 완료")

        # 테스트 케이스
        test_cases = [
            {
                "query": "카반 보드가 뭐야?",
                "expected_correction": "칸반 보드",
                "description": "맞춤법 오류: 카반 → 칸반"
            },
            {
                "query": "스크램 방법론에 대해 알려줘",
                "expected_correction": "스크럼",
                "description": "맞춤법 오류: 스크램 → 스크럼"
            },
            {
                "query": "플래닝포커는 어떻게 하는거야?",
                "expected_correction": "플래닝 포커",
                "description": "띄어쓰기 오류"
            },
            {
                "query": "간트차트 작성 방법",
                "expected_correction": "간트 차트",
                "description": "띄어쓰기 오류"
            },
            {
                "query": "애자일 방법론이 뭐야?",
                "expected_correction": None,
                "description": "정상 쿼리 (교정 불필요)"
            }
        ]

        logger.info("\n" + "=" * 80)
        logger.info("테스트 케이스 실행")
        logger.info("=" * 80 + "\n")

        for i, test_case in enumerate(test_cases, 1):
            query = test_case["query"]
            expected = test_case["expected_correction"]
            description = test_case["description"]

            logger.info(f"\n{'='*80}")
            logger.info(f"테스트 케이스 {i}: {description}")
            logger.info(f"{'='*80}")
            logger.info(f"입력 쿼리: {query}")
            if expected:
                logger.info(f"예상 교정: {expected}")

            try:
                # 워크플로우 실행 (LLM 없이도 쿼리 개선은 동작)
                result = workflow.run(query, context=[])

                # 결과 출력
                debug_info = result.get("debug_info", {})
                query_refinement = debug_info.get("query_refinement")

                logger.info(f"\n📊 결과:")
                logger.info(f"  - RAG 문서 수: {result.get('rag_docs_count', 0)}")
                logger.info(f"  - 의도: {result.get('intent')}")

                if query_refinement:
                    logger.info(f"\n✨ 쿼리 개선 정보:")
                    logger.info(f"  - 원본 쿼리: {query_refinement['original_query']}")
                    logger.info(f"  - 최종 쿼리: {query_refinement['final_query']}")
                    logger.info(f"  - 재시도 횟수: {query_refinement['retry_count']}")
                    logger.info(f"  - 추출된 용어: {query_refinement['extracted_terms']}")

                    # 모든 시도별 쿼리 출력
                    for key, value in debug_info.items():
                        if key.startswith("refined_query_"):
                            logger.info(f"  - {key}: {value}")
                        if key.startswith("search_query_attempt_"):
                            logger.info(f"  - {key}: {value}")
                else:
                    logger.info(f"\n⚠️ 쿼리 개선 없음 (1차 검색 성공)")

                # 품질 점수 출력
                if "rag_quality_score" in debug_info:
                    logger.info(f"\n📈 RAG 품질 점수: {debug_info['rag_quality_score']:.2f}")
                    logger.info(f"  - 이유: {', '.join(debug_info.get('rag_quality_reasons', []))}")

                # 응답 출력 (LLM이 있을 때만)
                if llm and result.get("reply"):
                    logger.info(f"\n💬 응답:")
                    logger.info(f"  {result['reply'][:200]}...")

                logger.info(f"\n{'✅' if result.get('rag_docs_count', 0) > 0 else '❌'} 테스트 완료\n")

            except Exception as e:
                logger.error(f"❌ 테스트 실패: {e}", exc_info=True)

        logger.info("\n" + "=" * 80)
        logger.info("모든 테스트 완료!")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"테스트 실패: {e}", exc_info=True)
        sys.exit(1)

if __name__ == "__main__":
    test_query_refinement()
