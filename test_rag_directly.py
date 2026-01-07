#!/usr/bin/env python3
"""RAG 서비스를 직접 테스트"""
import sys
sys.path.insert(0, '/home/daniel/projects/pms-ic/llm-service')

from rag_service_neo4j import RAGServiceNeo4j
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

try:
    logger.info("Creating RAG service...")
    rag = RAGServiceNeo4j()

    logger.info("Searching for '스크럼이란?'...")
    results = rag.search("스크럼이란?", top_k=3)

    logger.info(f"Results: {len(results)}")
    for i, result in enumerate(results):
        logger.info(f"\n=== Result {i+1} ===")
        logger.info(f"Type: {type(result)}")
        logger.info(f"Keys: {result.keys() if isinstance(result, dict) else 'not a dict'}")
        if isinstance(result, dict):
            logger.info(f"Content preview: {result.get('content', '')[:200]}")

except Exception as e:
    logger.error(f"Error: {e}", exc_info=True)
