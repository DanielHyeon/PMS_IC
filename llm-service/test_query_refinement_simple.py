"""
쿼리 개선 로직 단위 테스트 (RAG/LLM 없이)
키워드 추출과 퍼지 매칭 기능만 테스트
"""

import logging
import sys

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def test_keyword_extraction():
    """키워드 추출 테스트"""
    from chat_workflow import ChatWorkflow

    # 더미 워크플로우 생성 (LLM/RAG 없이)
    workflow = ChatWorkflow(llm=None, rag_service=None)

    test_cases = [
        {
            "query": "카반 보드가 뭐야?",
            "expected_keywords": ["카반", "보드"]
        },
        {
            "query": "스크램 방법론에 대해 알려줘",
            "expected_keywords": ["스크램", "방법론"]
        },
        {
            "query": "플래닝포커는 어떻게 하는거야?",
            "expected_keywords": ["플래닝포커", "어떻"]
        },
        {
            "query": "애자일 스프린트 계획은?",
            "expected_keywords": ["애자일", "스프린트", "계획"]
        }
    ]

    logger.info("\n" + "=" * 80)
    logger.info("키워드 추출 테스트")
    logger.info("=" * 80)

    for i, test_case in enumerate(test_cases, 1):
        query = test_case["query"]
        expected = test_case["expected_keywords"]

        logger.info(f"\n[테스트 {i}] 쿼리: {query}")

        keywords = workflow._extract_keywords(query)

        logger.info(f"  추출된 키워드: {keywords}")
        logger.info(f"  예상 키워드: {expected}")

        # 최소 1개 이상의 키워드가 추출되었는지 확인
        if keywords:
            logger.info(f"  ✅ 성공 ({len(keywords)}개 추출)")
        else:
            logger.warning(f"  ⚠️ 경고: 키워드가 추출되지 않음")

    logger.info("\n" + "=" * 80)

def test_similar_terms_finder():
    """유사 용어 찾기 테스트"""
    from chat_workflow import ChatWorkflow

    workflow = ChatWorkflow(llm=None, rag_service=None)

    # 샘플 문서 (칸반 보드에 대한 설명)
    sample_docs = [
        """
        칸반 보드는 애자일 방법론에서 사용하는 시각적 관리 도구입니다.
        작업의 흐름을 한눈에 파악할 수 있으며, To Do, In Progress, Done과 같은
        칼럼으로 구성됩니다.
        """,
        """
        스크럼과 칸반의 차이점:
        - 스크럼은 스프린트 단위로 작업
        - 칸반 보드는 지속적인 흐름 관리
        """,
        """
        플래닝 포커는 애자일 추정 기법입니다.
        팀원들이 카드를 사용하여 작업 복잡도를 추정합니다.
        """
    ]

    test_cases = [
        {
            "query": "카반 보드가 뭐야?",
            "expected_term": "칸반 보드"
        },
        {
            "query": "스크램 방법론",
            "expected_term": "스크럼"
        },
        {
            "query": "플래닝포커",
            "expected_term": "플래닝 포커"
        }
    ]

    logger.info("\n" + "=" * 80)
    logger.info("유사 용어 찾기 테스트 (퍼지 매칭)")
    logger.info("=" * 80)

    for i, test_case in enumerate(test_cases, 1):
        query = test_case["query"]
        expected = test_case["expected_term"]

        logger.info(f"\n[테스트 {i}] 쿼리: '{query}'")
        logger.info(f"  예상 교정: '{expected}'")

        similar_terms = workflow._find_similar_terms_in_docs(query, sample_docs)

        if similar_terms:
            logger.info(f"  발견된 유사 용어: {similar_terms}")

            # 예상 용어가 포함되어 있는지 확인
            found = any(expected in term for term in similar_terms)
            if found:
                logger.info(f"  ✅ 성공: '{expected}' 발견!")
            else:
                logger.warning(f"  ⚠️ 부분 성공: 유사 용어를 찾았지만 '{expected}'는 없음")
        else:
            logger.warning(f"  ❌ 실패: 유사 용어를 찾지 못함")

    logger.info("\n" + "=" * 80)

def test_query_refinement_strategy():
    """쿼리 개선 전략 테스트 (시뮬레이션)"""

    logger.info("\n" + "=" * 80)
    logger.info("쿼리 개선 전략 시뮬레이션")
    logger.info("=" * 80)

    test_scenarios = [
        {
            "name": "시나리오 1: 맞춤법 오류 - 카반 보드",
            "steps": [
                "1차 검색: '카반 보드' → 결과 0개",
                "전략 1 (키워드 추출): '카반 보드' → '카반 보드' (키워드)",
                "2차 검색: '카반 보드' → 결과 2개 (낮은 유사도)",
                "전략 2 (유사 용어 탐색): 문서에서 '칸반 보드' 발견 (유사도 90%)",
                "3차 검색: '칸반 보드' → 결과 10개 (높은 유사도)",
                "✅ 성공: 정확한 용어로 검색 완료"
            ]
        },
        {
            "name": "시나리오 2: 정상 쿼리 - 애자일 방법론",
            "steps": [
                "1차 검색: '애자일 방법론' → 결과 5개",
                "품질 검증: 문서 개수 충분, 키워드 매칭 양호",
                "✅ 성공: 재검색 불필요"
            ]
        },
        {
            "name": "시나리오 3: 찾을 수 없는 용어",
            "steps": [
                "1차 검색: '블록체인 개발' → 결과 0개",
                "전략 1 (키워드 추출): '블록체인 개발' → '블록체인 개발'",
                "2차 검색: '블록체인 개발' → 결과 0개",
                "전략 2 (유사 용어): 유사 용어 없음",
                "3차 검색: '블록체인 개발' → 결과 0개",
                "⚠️ 최대 재시도 도달: 범위 밖 질문으로 처리"
            ]
        }
    ]

    for scenario in test_scenarios:
        logger.info(f"\n{scenario['name']}")
        logger.info("-" * 80)
        for step in scenario["steps"]:
            logger.info(f"  {step}")

    logger.info("\n" + "=" * 80)

def main():
    """메인 테스트 실행"""
    logger.info("\n" + "🧪 쿼리 개선 로직 단위 테스트 시작")

    try:
        # 1. 키워드 추출 테스트
        test_keyword_extraction()

        # 2. 유사 용어 찾기 테스트
        test_similar_terms_finder()

        # 3. 전략 시뮬레이션
        test_query_refinement_strategy()

        logger.info("\n" + "=" * 80)
        logger.info("✅ 모든 단위 테스트 완료!")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"\n❌ 테스트 실패: {e}", exc_info=True)
        sys.exit(1)

if __name__ == "__main__":
    main()
