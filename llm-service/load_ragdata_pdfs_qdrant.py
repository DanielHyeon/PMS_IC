"""
ragdata/ 폴더의 PDF 파일들을 읽어서 Qdrant 벡터 DB에 추가
MinerU2.5 모델을 사용한 고급 문서 파싱 적용
"""

import logging
import os
import sys
from pathlib import Path

# 환경변수 설정 - MinerU 모델 사용
os.environ["USE_MINERU_MODEL"] = "true"

from rag_service_qdrant import RAGServiceQdrant

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


def load_pdf_files(ragdata_dir: str = "../ragdata"):
    """
    ragdata 디렉토리의 PDF 파일들을 로드하여 Qdrant 벡터 DB에 추가

    Args:
        ragdata_dir: PDF 파일이 있는 디렉토리 경로
    """

    logger.info("=" * 80)
    logger.info("ragdata PDF 파일 Qdrant 벡터 DB 추가")
    logger.info("=" * 80)

    try:
        # 1. RAG 서비스 초기화
        logger.info("\n1️⃣  RAG 서비스 초기화 (Qdrant + MinerU)...")
        rag_service = RAGServiceQdrant()

        # MinerU 모델 로드 확인
        if rag_service.parser.model:
            logger.info("✅ MinerU2.5 모델이 성공적으로 로드되었습니다!")
        else:
            logger.warning("⚠️  휴리스틱 파싱 모드로 실행됩니다.")

        # 2. ragdata 디렉토리 확인
        logger.info(f"\n2️⃣  ragdata 디렉토리 확인: {ragdata_dir}")

        ragdata_path = Path(ragdata_dir)
        if not ragdata_path.exists():
            logger.error(f"❌ 디렉토리를 찾을 수 없습니다: {ragdata_dir}")
            return

        # PDF 파일 목록 가져오기
        pdf_files = list(ragdata_path.glob("*.pdf"))

        if not pdf_files:
            logger.warning(f"⚠️  PDF 파일을 찾을 수 없습니다: {ragdata_dir}")
            return

        logger.info(f"✅ {len(pdf_files)}개의 PDF 파일 발견:")
        for i, pdf_file in enumerate(pdf_files, 1):
            file_size = pdf_file.stat().st_size / (1024 * 1024)  # MB
            logger.info(f"   {i}. {pdf_file.name} ({file_size:.1f} MB)")

        # 3. 각 PDF 파일 처리
        logger.info(f"\n3️⃣  PDF 파일 처리 시작...")
        logger.info("   - MinerU2.5 모델로 구조 파싱")
        logger.info("   - Layout-Aware Chunking 적용")
        logger.info("   - Qdrant 벡터 DB에 저장\n")

        success_count = 0
        total_chunks = 0

        for i, pdf_file in enumerate(pdf_files, 1):
            try:
                logger.info(f"\n📄 [{i}/{len(pdf_files)}] 처리 중: {pdf_file.name}")

                # PDF에서 텍스트 추출
                logger.info(f"   텍스트 추출 중...")
                text = rag_service.extract_text_from_file(str(pdf_file), 'pdf')

                if not text or len(text.strip()) < 100:
                    logger.warning(f"   ⚠️  텍스트가 너무 짧거나 비어있습니다 (길이: {len(text)})")
                    continue

                logger.info(f"   추출된 텍스트 길이: {len(text):,} 문자")

                # 문서 ID 생성 (파일명 기반)
                doc_id = f"ragdata_{pdf_file.stem}"

                # 문서 객체 생성
                document = {
                    'id': doc_id,
                    'content': text,
                    'metadata': {
                        'type': 'pdf',
                        'source': 'ragdata',
                        'filename': pdf_file.name,
                        'category': 'reference_document',
                        'file_size_mb': round(pdf_file.stat().st_size / (1024 * 1024), 2)
                    }
                }

                # Qdrant 벡터 DB에 추가
                logger.info(f"   MinerU 파싱 및 벡터화 중...")
                if rag_service.add_document(document):
                    success_count += 1

                    # 저장된 청크 수는 통계에서 확인
                    logger.info(f"   ✅ 완료")
                    total_chunks += 1  # 간단한 카운트
                else:
                    logger.error(f"   ❌ 실패")

            except Exception as e:
                logger.error(f"   ❌ 처리 실패: {e}", exc_info=True)
                continue

        # 4. 최종 통계
        logger.info("\n" + "=" * 80)
        logger.info("4️⃣  처리 완료 통계")
        logger.info("=" * 80)
        logger.info(f"✅ 성공: {success_count}/{len(pdf_files)} 파일")

        # 5. Qdrant 벡터 DB 전체 통계
        logger.info("\n5️⃣  Qdrant 벡터 DB 전체 통계:")
        logger.info("-" * 80)
        stats = rag_service.get_collection_stats()
        logger.info(f"""
컬렉션: {stats.get('collection_name')}
- 전체 청크: {stats.get('total_chunks'):,}개
- 벡터 크기: {stats.get('vector_size')}
- 거리 메트릭: {stats.get('distance')}
- 파서: {stats.get('parser')}
        """)

        # 6. 검색 테스트
        logger.info("\n6️⃣  검색 품질 테스트:")
        logger.info("-" * 80)

        test_queries = [
            "플래닝 포커가 뭐야?",
            "소프트웨어 테스팅 방법론은?",
            "스크럼과 XP의 차이점은?",
            "프로젝트 관리 프로세스는?",
        ]

        for query in test_queries:
            logger.info(f"\n질문: {query}")
            results = rag_service.search(query, top_k=2)

            if results:
                for i, result in enumerate(results, 1):
                    metadata = result.get('metadata', {})
                    logger.info(f"  결과 {i}:")
                    logger.info(f"    파일: {metadata.get('filename', 'N/A')}")
                    logger.info(f"    관련성: {result.get('relevance_score', 0):.3f}")
                    logger.info(f"    거리: {result.get('distance', 0):.3f}")

                    content_preview = result.get('content', '')[:100].replace('\n', ' ')
                    logger.info(f"    내용: {content_preview}...")
            else:
                logger.info("  결과 없음")

        logger.info("\n" + "=" * 80)
        logger.info("✅ ragdata PDF 파일 Qdrant 벡터 DB 추가 완료!")
        logger.info("=" * 80)

    except Exception as e:
        logger.error(f"\n❌ 처리 실패: {e}", exc_info=True)
        sys.exit(1)


def main():
    """메인 실행 함수"""

    # ragdata 디렉토리 경로 설정
    # Docker 환경에서는 /app/ragdata 사용
    if os.path.exists("/app/ragdata"):
        ragdata_dir = "/app/ragdata"
    else:
        ragdata_dir = os.path.join(
            os.path.dirname(os.path.dirname(__file__)),
            "ragdata"
        )

    logger.info(f"\n🚀 ragdata PDF 파일 로드 시작...")
    logger.info(f"   경로: {ragdata_dir}\n")

    # PDF 파일 로드
    load_pdf_files(ragdata_dir)


if __name__ == "__main__":
    main()
