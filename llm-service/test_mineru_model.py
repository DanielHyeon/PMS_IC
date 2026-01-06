"""
MinerU2.5 모델 로드 및 파싱 테스트
"""

import logging
import sys
import os

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def test_mineru_model():
    """MinerU 모델 로드 및 파싱 테스트"""

    try:
        from document_parser import MinerUDocumentParser

        logger.info("=" * 80)
        logger.info("MinerU2.5 모델 테스트 시작")
        logger.info("=" * 80)

        # 1. 모델 로드 테스트
        logger.info("\n1️⃣  MinerU 모델 로드 중...")
        parser = MinerUDocumentParser(use_mock=False, device="cpu")

        if parser.model is None:
            logger.error("❌ 모델 로드 실패 - mock 모드로 전환됨")
            logger.info("   모델 경로를 확인하세요:")
            logger.info(f"   {parser.model_path}")
            return False

        logger.info("✅ MinerU 모델 로드 성공!")

        # 2. 샘플 문서 파싱 테스트
        logger.info("\n2️⃣  샘플 문서 파싱 테스트...")

        sample_text = """
프로젝트명: 보험 Claim Automation AI 시스템 구축

프로젝트 개요:
이 프로젝트는 보험 청구 심사 프로세스의 AI 자동화를 목표로 합니다.

주요 목표:
- 업무 효율성 30% 향상
- OCR 정확도 98% 달성
- 진단서 분류 정확도 95% 달성

프로젝트 진행 현황:

단계          진행률    상태
1단계        100%      완료
2단계        100%      완료
3단계        85%       진행중
4단계        0%        대기

팀 구성:
• AI 개발팀: 12명
• QA팀: 4명
• 현업 분석가: 5명
        """

        logger.info(f"   문서 길이: {len(sample_text)} 문자")

        blocks = parser.parse_document(sample_text.strip())

        logger.info(f"\n✅ 파싱 완료: {len(blocks)}개 블록 추출")

        # 3. 결과 출력
        logger.info("\n3️⃣  파싱 결과:")
        logger.info("-" * 80)

        for i, block in enumerate(blocks[:10], 1):  # 처음 10개만 출력
            logger.info(f"\n블록 {i}:")
            logger.info(f"  타입: {block.type.value}")
            logger.info(f"  내용: {block.content[:100]}...")
            if block.metadata:
                logger.info(f"  메타데이터: {block.metadata}")

        if len(blocks) > 10:
            logger.info(f"\n... (총 {len(blocks)}개 블록 중 10개만 표시)")

        # 4. 블록 타입 통계
        logger.info("\n4️⃣  블록 타입 통계:")
        logger.info("-" * 80)

        from collections import Counter
        type_counts = Counter([block.type.value for block in blocks])

        for block_type, count in type_counts.most_common():
            logger.info(f"  {block_type:15s}: {count:3d}개")

        logger.info("\n" + "=" * 80)
        logger.info("✅ 모든 테스트 성공!")
        logger.info("=" * 80)

        return True

    except Exception as e:
        logger.error(f"\n❌ 테스트 실패: {e}", exc_info=True)
        return False


def test_with_heuristics():
    """휴리스틱 파싱과 비교 테스트"""

    try:
        from document_parser import MinerUDocumentParser

        logger.info("\n" + "=" * 80)
        logger.info("휴리스틱 vs MinerU 모델 비교")
        logger.info("=" * 80)

        sample_text = """
프로젝트 KPI

1. 전체 진행률: 62%
   - 목표: 100% (2024-12-31)
   - 현재 상태: 순조

2. 예산 사용률: 58%
   - 사용액: 8.7억원 / 15억원
   - 상태: 양호

성과 지표:
• OCR 정확도: 96.5% (목표: 98%)
• 분류 정확도: 92.3% (목표: 95%)
• 완료 작업: 142/230건
        """

        # 휴리스틱 파싱
        logger.info("\n🔹 휴리스틱 파싱:")
        heuristic_parser = MinerUDocumentParser(use_mock=True)
        heuristic_blocks = heuristic_parser.parse_document(sample_text.strip())
        logger.info(f"   추출된 블록 수: {len(heuristic_blocks)}")

        # MinerU 모델 파싱
        logger.info("\n🔹 MinerU 모델 파싱:")
        model_parser = MinerUDocumentParser(use_mock=False, device="cpu")

        if model_parser.model:
            model_blocks = model_parser.parse_document(sample_text.strip())
            logger.info(f"   추출된 블록 수: {len(model_blocks)}")

            # 비교
            logger.info(f"\n📊 차이: {abs(len(model_blocks) - len(heuristic_blocks))}개 블록")
        else:
            logger.warning("   MinerU 모델 로드 실패 - 비교 불가")

        return True

    except Exception as e:
        logger.error(f"비교 테스트 실패: {e}", exc_info=True)
        return False


if __name__ == "__main__":
    logger.info("\n🚀 MinerU2.5 모델 테스트 시작...\n")

    # 모델 경로 확인
    model_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)),
        "models",
        "MinerU2.5-2509-1.2B.i1-Q6_K.gguf"
    )

    logger.info(f"모델 경로: {model_path}")
    logger.info(f"모델 존재 여부: {'✅ 있음' if os.path.exists(model_path) else '❌ 없음'}")

    if not os.path.exists(model_path):
        logger.error("\n⚠️  모델 파일을 찾을 수 없습니다!")
        logger.error(f"   경로: {model_path}")
        sys.exit(1)

    # 테스트 실행
    success = test_mineru_model()

    if success:
        logger.info("\n추가 테스트: 휴리스틱 vs 모델 비교")
        test_with_heuristics()

    sys.exit(0 if success else 1)
