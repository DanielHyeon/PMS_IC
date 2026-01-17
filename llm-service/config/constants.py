"""
LLM Service configuration constants
Centralizes magic numbers and configuration values
"""

from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class RAGConfig:
    """RAG search and quality configuration"""
    MIN_RELEVANCE_SCORE: float = 0.3
    QUALITY_THRESHOLD: float = 0.6
    MAX_QUERY_RETRIES: int = 4  # 증가: 2 → 4 (Gemma 3 안정성 개선)
    FUZZY_MATCH_THRESHOLD: int = 70
    DEFAULT_TOP_K: int = 5
    KEYWORD_MATCH_GOOD_RATIO: float = 0.5

    # Response validation thresholds
    MIN_RESPONSE_LENGTH: int = 10
    MAX_RESPONSE_LENGTH: int = 4000

    # Timeout configuration (ms)
    LLM_RESPONSE_TIMEOUT: int = 30000  # 30초
    QUERY_TIMEOUT: int = 60000  # 1분


@dataclass(frozen=True)
class LLMConfig:
    """LLM generation configuration"""
    MAX_TOKENS: int = 8182
    TEMPERATURE: float = 0.7
    TOP_P: float = 0.9
    REPEAT_PENALTY: float = 1.1
    CONTEXT_MESSAGE_LIMIT: int = 5


@dataclass(frozen=True)
class CacheConfig:
    """Redis cache configuration"""
    SESSION_TTL_HOURS: int = 1
    MESSAGE_CACHE_PREFIX: str = "chat:session:"


@dataclass(frozen=True)
class ConfidenceScores:
    """Response confidence scores by intent type"""
    CASUAL: float = 0.95
    PMS_QUERY: float = 0.70
    GENERAL: float = 0.80
    DEFAULT: float = 0.75
    MAX_CONFIDENCE: float = 0.95
    RAG_BOOST_PER_DOC: float = 0.05
    MAX_RAG_BOOST: float = 0.15


# Singleton instances
RAG = RAGConfig()
LLM = LLMConfig()
CACHE = CacheConfig()
CONFIDENCE = ConfidenceScores()


# Paths
PROMPTS_DIR = Path(__file__).parent.parent / "prompts"


def get_prompt(name: str) -> str:
    """Load prompt from file"""
    prompt_file = PROMPTS_DIR / f"{name}.txt"
    if prompt_file.exists():
        return prompt_file.read_text(encoding="utf-8").strip()
    raise FileNotFoundError(f"Prompt file not found: {prompt_file}")
