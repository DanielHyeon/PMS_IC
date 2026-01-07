"""
Load PDFs from ragdata/ into Neo4j GraphRAG (vector + graph).
"""

import argparse
import logging
import os
import sys
from datetime import datetime
from pathlib import Path

# Default: use MinerU model for parsing
os.environ.setdefault("USE_MINERU_MODEL", "true")

from rag_service_neo4j import RAGServiceNeo4j
from pdf_ocr_pipeline import extract_text_from_pdf

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def extract_text_from_pdf_for_neo4j(file_path: str) -> str:
    """Extract text from a PDF file (supports OCR pipeline)."""
    return extract_text_from_pdf(file_path)


def clear_neo4j(rag_service: RAGServiceNeo4j) -> None:
    """Delete all nodes and relationships in Neo4j."""
    logger.warning("Clearing Neo4j database (all nodes and relationships)...")
    with rag_service.driver.session() as session:
        session.run("MATCH (n) DETACH DELETE n")
    logger.info("Neo4j database cleared")


def count_chunks_for_doc(rag_service: RAGServiceNeo4j, doc_id: str) -> int:
    """Count chunks for a document."""
    with rag_service.driver.session() as session:
        result = session.run(
            """
            MATCH (d:Document {doc_id: $doc_id})-[:HAS_CHUNK]->(c:Chunk)
            RETURN count(c) AS chunk_count
            """,
            doc_id=doc_id,
        )
        record = result.single()
        return int(record["chunk_count"]) if record else 0


def load_pdf_files(ragdata_dir: str, clear_db: bool) -> None:
    logger.info("=" * 80)
    logger.info("ragdata PDF files -> Neo4j GraphRAG indexing")
    logger.info("=" * 80)

    try:
        rag_service = RAGServiceNeo4j()

        if clear_db:
            clear_neo4j(rag_service)

        ragdata_path = Path(ragdata_dir)
        if not ragdata_path.exists():
            logger.error("ragdata directory not found: %s", ragdata_dir)
            return

        pdf_files = list(ragdata_path.glob("*.pdf"))
        if not pdf_files:
            logger.warning("No PDF files found in: %s", ragdata_dir)
            return

        logger.info("Found %d PDF files:", len(pdf_files))
        for i, pdf_file in enumerate(pdf_files, 1):
            file_size = pdf_file.stat().st_size / (1024 * 1024)
            logger.info("  %d. %s (%.1f MB)", i, pdf_file.name, file_size)

        success_count = 0
        total_chunks = 0

        for i, pdf_file in enumerate(pdf_files, 1):
            try:
                logger.info("\n[%d/%d] Processing: %s", i, len(pdf_files), pdf_file.name)
                text = extract_text_from_pdf_for_neo4j(str(pdf_file))

                if not text or len(text.strip()) < 100:
                    logger.warning("  Text is empty or too short (len=%d)", len(text or ""))
                    continue

                doc_id = f"ragdata_{pdf_file.stem}"
                created_at = datetime.utcnow().replace(microsecond=0).isoformat() + "Z"
                document = {
                    "id": doc_id,
                    "content": text,
                    "metadata": {
                        "file_name": pdf_file.name,
                        "file_path": str(pdf_file),
                        "file_type": "pdf",
                        "source": "ragdata",
                        "category": "reference_document",
                        "created_at": created_at,
                    },
                }

                if rag_service.add_document(document):
                    success_count += 1
                    chunk_count = count_chunks_for_doc(rag_service, doc_id)
                    total_chunks += chunk_count
                    logger.info("  ✅ Added (%d chunks)", chunk_count)
                else:
                    logger.error("  ❌ Failed to add document")
            except Exception as exc:
                logger.error("  ❌ Error processing %s: %s", pdf_file.name, exc, exc_info=True)
                continue

        logger.info("\n" + "=" * 80)
        logger.info("Indexing complete")
        logger.info("=" * 80)
        logger.info("Success: %d/%d files", success_count, len(pdf_files))
        logger.info("Total chunks: %d", total_chunks)

        stats = rag_service.get_collection_stats()
        logger.info("Neo4j stats: %s", stats)

        test_queries = [
            "스크럼 스프린트 계획은?",
            "XP 핵심 프랙티스는?",
            "프로젝트 관리 프로세스는?",
            "소프트웨어 테스팅 방법론은?",
        ]

        for query in test_queries:
            logger.info("\nQuery: %s", query)
            results = rag_service.search(query, top_k=2)
            if not results:
                logger.info("  No results")
                continue
            for idx, result in enumerate(results, 1):
                metadata = result.get("metadata", {})
                title = metadata.get("doc_title") or metadata.get("title") or metadata.get("doc_id")
                snippet = (result.get("content") or "")[:120].replace("\n", " ")
                logger.info("  [%d] %s (score=%.3f)", idx, title, result.get("relevance_score", 0))
                logger.info("      %s...", snippet)

        rag_service.close()

    except Exception as exc:
        logger.error("Indexing failed: %s", exc, exc_info=True)
        sys.exit(1)


def main() -> None:
    parser = argparse.ArgumentParser(description="Load ragdata PDFs into Neo4j GraphRAG")
    parser.add_argument(
        "--ragdata-dir",
        default="/app/ragdata" if os.path.exists("/app/ragdata") else os.path.join(
            os.path.dirname(os.path.dirname(__file__)), "ragdata"
        ),
        help="Path to ragdata directory (default: auto-detect)",
    )
    parser.add_argument(
        "--clear-neo4j",
        action="store_true",
        help="Delete all existing Neo4j data before indexing",
    )

    args = parser.parse_args()

    logger.info("Starting ragdata indexing into Neo4j...")
    logger.info("ragdata_dir: %s", args.ragdata_dir)
    logger.info("clear_neo4j: %s", args.clear_neo4j)

    load_pdf_files(args.ragdata_dir, args.clear_neo4j)


if __name__ == "__main__":
    main()
