"""
완전한 RAG 시스템 초기화
1. Mock 데이터 추가
2. ragdata/ PDF 파일 추가
3. MinerU2.5 모델 사용
"""

import logging
import os
import sys

# 환경변수 설정
os.environ["USE_MINERU_MODEL"] = "true"

from rag_service_v2 import RAGServiceV2
from init_rag_mock_data import MOCK_DOCUMENTS
from pathlib import Path

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def main():
    """메인 실행 함수"""

    logger.info("=" * 80)
    logger.info("완전한 RAG 시스템 초기화")
    logger.info("MinerU2.5 모델 + Mock 데이터 + ragdata PDF")
    logger.info("=" * 80)

    try:
        # 1. RAG 서비스 초기화
        logger.info("\n" + "=" * 80)
        logger.info("STEP 1: RAG 서비스 초기화")
        logger.info("=" * 80)

        logger.info("MinerU2.5 모델 로드 중...")
        rag_service = RAGServiceV2()

        if rag_service.parser.model:
            logger.info("✅ MinerU2.5 모델 로드 성공")
            logger.info(f"   모델: {os.path.basename(rag_service.parser.model_path)}")
        else:
            logger.warning("⚠️  휴리스틱 파싱 모드")

        # 2. 벡터 DB 초기화
        logger.info("\n" + "=" * 80)
        logger.info("STEP 2: 벡터 DB 초기화")
        logger.info("=" * 80)

        logger.info("기존 데이터 삭제 중...")
        if rag_service.reset_collection():
            logger.info("✅ 벡터 DB 초기화 완료")
        else:
            logger.error("❌ 벡터 DB 초기화 실패")
            return

        # 3. Mock 데이터 추가
        logger.info("\n" + "=" * 80)
        logger.info("STEP 3: Mock 데이터 추가")
        logger.info("=" * 80)

        logger.info(f"Mock 데이터 {len(MOCK_DOCUMENTS)}개 처리 중...")
        mock_success = rag_service.add_documents(MOCK_DOCUMENTS)
        logger.info(f"✅ Mock 데이터: {mock_success}/{len(MOCK_DOCUMENTS)} 추가 완료")

        # 4. ragdata PDF 파일 추가
        logger.info("\n" + "=" * 80)
        logger.info("STEP 4: ragdata PDF 파일 추가")
        logger.info("=" * 80)

        ragdata_dir = Path(os.path.dirname(os.path.dirname(__file__))) / "ragdata"

        if not ragdata_dir.exists():
            logger.warning(f"⚠️  ragdata 디렉토리를 찾을 수 없습니다: {ragdata_dir}")
            pdf_success = 0
            pdf_total = 0
        else:
            pdf_files = list(ragdata_dir.glob("*.pdf"))
            pdf_total = len(pdf_files)

            if pdf_files:
                logger.info(f"{len(pdf_files)}개의 PDF 파일 발견:")
                for i, pdf_file in enumerate(pdf_files, 1):
                    file_size = pdf_file.stat().st_size / (1024 * 1024)
                    logger.info(f"  {i}. {pdf_file.name} ({file_size:.1f} MB)")

                logger.info("\nPDF 파일 처리 시작...")
                pdf_success = 0

                for i, pdf_file in enumerate(pdf_files, 1):
                    try:
                        logger.info(f"\n[{i}/{len(pdf_files)}] {pdf_file.name}")

                        # 텍스트 추출
                        text = rag_service.extract_text_from_file(str(pdf_file), 'pdf')

                        if not text or len(text.strip()) < 100:
                            logger.warning(f"  ⚠️  텍스트가 비어있거나 너무 짧음")
                            continue

                        logger.info(f"  텍스트 길이: {len(text):,} 문자")

                        # 문서 추가
                        document = {
                            'id': f"ragdata_{pdf_file.stem}",
                            'content': text,
                            'metadata': {
                                'type': 'pdf',
                                'source': 'ragdata',
                                'filename': pdf_file.name,
                                'category': 'reference_document'
                            }
                        }

                        if rag_service.add_document(document):
                            pdf_success += 1
                            logger.info(f"  ✅ 완료")
                        else:
                            logger.error(f"  ❌ 실패")

                    except Exception as e:
                        logger.error(f"  ❌ 오류: {e}")
                        continue

            else:
                logger.warning("⚠️  PDF 파일이 없습니다")
                pdf_success = 0
                pdf_total = 0

        # 5. 최종 통계
        logger.info("\n" + "=" * 80)
        logger.info("STEP 5: 최종 통계")
        logger.info("=" * 80)

        stats = rag_service.get_collection_stats()

        logger.info(f"""
📊 처리 결과:
  - Mock 데이터: {mock_success}/{len(MOCK_DOCUMENTS)} 추가
  - PDF 파일: {pdf_success}/{pdf_total} 추가

📦 벡터 DB 상태:
  - 컬렉션: {stats.get('collection_name')}
  - 전체 청크: {stats.get('total_chunks'):,}개
  - 구조화된 청크: {stats.get('structured_chunks'):,}개
  - 표 포함 청크: {stats.get('chunks_with_tables'):,}개
  - 리스트 포함 청크: {stats.get('chunks_with_lists'):,}개
  - 파서: {stats.get('parser')}
        """)

        # 6. 검색 테스트
        logger.info("\n" + "=" * 80)
        logger.info("STEP 6: 검색 품질 테스트")
        logger.info("=" * 80)

        test_queries = [
            "현재 프로젝트 진행률은?",  # Mock 데이터
            "소프트웨어 테스팅 방법론은?",  # PDF 데이터
            "스크럼 스프린트 계획은?",  # PDF 데이터
        ]

        for query in test_queries:
            logger.info(f"\n질문: {query}")
            results = rag_service.search(query, top_k=2)

            if results:
                for i, result in enumerate(results, 1):
                    metadata = result.get('metadata', {})
                    logger.info(f"  [{i}] {metadata.get('filename', metadata.get('category', 'N/A'))}")
                    logger.info(f"      관련성: {result.get('relevance_score', 0):.3f}")

                    # 파싱 소스
                    if 'source' in metadata:
                        source_marker = "🤖" if metadata.get('source') == 'mineru_model' else "📐"
                        logger.info(f"      파싱: {source_marker}")
            else:
                logger.info("  결과 없음")

        logger.info("\n" + "=" * 80)
        logger.info("✅ RAG 시스템 초기화 완료!")
        logger.info("=" * 80)

        logger.info(f"""
🎯 다음 단계:
1. 프론트엔드에서 AI 챗봇 테스트
2. API 직접 호출:
   curl -X POST http://localhost:8000/api/documents/search \\
     -H 'Content-Type: application/json' \\
     -d '{{"query": "소프트웨어 테스팅이란?", "top_k": 3}}'

3. 통계 조회:
   curl http://localhost:8000/api/documents/stats
        """)

    except Exception as e:
        logger.error(f"\n❌ 초기화 실패: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
