"""
Response Validator for Gemma 3 12B Q5 Stability
특정 실패 패턴 감지 및 재시도 트리거
"""

import re
import logging
from typing import Optional, Tuple
from enum import Enum
from dataclasses import dataclass

logger = logging.getLogger(__name__)


class ResponseFailureType(Enum):
    """응답 실패 유형 분류"""
    UNABLE_TO_ANSWER = "unable_to_answer"
    INCOMPLETE_RESPONSE = "incomplete_response"
    MALFORMED_RESPONSE = "malformed_response"
    OUT_OF_CONTEXT = "out_of_context"
    REPETITIVE_RESPONSE = "repetitive_response"
    EMPTY_RESPONSE = "empty_response"
    TIMEOUT_CUTOFF = "timeout_cutoff"
    NONE = "none"


@dataclass
class ValidationResult:
    """응답 검증 결과"""
    is_valid: bool
    failure_type: ResponseFailureType
    confidence: float  # 0.0 ~ 1.0
    reason: str
    suggested_retry: bool


class ResponseValidator:
    """
    Gemma 3 LLM 응답 검증
    불안정한 응답 패턴 감지 및 분류
    """

    def __init__(self, min_response_length: int = 10, max_response_length: int = 4000):
        self.min_response_length = min_response_length
        self.max_response_length = max_response_length

        # 실패 패턴 정의
        self.unable_to_answer_patterns = [
            r"unable\s+to\s+answer",
            r"cannot\s+answer",
            r"답변할\s+수\s+없",
            r"답변\s+불가",
            r"모르겠",
            r"알\s+수\s+없",
            r"확실하지\s+않",
            r"판단할\s+수\s+없",
            r"정확한\s+답변\s+제공\s+불가",
            r"죄송.*?답변\s+드릴\s+수\s+없",
        ]

        self.incomplete_patterns = [
            r"\.{3,}$",  # 끝에 "..."
            r"등이\s+있습니다\.$",  # 미완성 문장
            r"^\s*(요약|Summary):",  # 미완성 답변
            r"다음과\s+같습니다\.",  # 불완전한 마침
        ]

        self.repetitive_patterns = [
            r"(.{20,})\1{2,}",  # 같은 문구 3번 이상 반복
            r"(네|예)\s+(네|예)\s+(네|예)",  # 반복 응답
            r"(좋습니다)\s+(좋습니다)",  # 중복 단어
        ]

    def validate(self, response: str, original_query: str = "") -> ValidationResult:
        """
        응답 검증

        Args:
            response: LLM의 응답 텍스트
            original_query: 원본 질문 (컨텍스트용)

        Returns:
            ValidationResult: 검증 결과
        """
        # 공백 정리
        response = response.strip() if response else ""

        # 1. 빈 응답 확인
        if not response or len(response) == 0:
            return ValidationResult(
                is_valid=False,
                failure_type=ResponseFailureType.EMPTY_RESPONSE,
                confidence=1.0,
                reason="응답이 비어있음",
                suggested_retry=True
            )

        # 2. 길이 검증
        if len(response) < self.min_response_length:
            return ValidationResult(
                is_valid=False,
                failure_type=ResponseFailureType.INCOMPLETE_RESPONSE,
                confidence=0.8,
                reason=f"응답이 너무 짧음 ({len(response)} chars < {self.min_response_length})",
                suggested_retry=True
            )

        if len(response) > self.max_response_length:
            return ValidationResult(
                is_valid=False,
                failure_type=ResponseFailureType.MALFORMED_RESPONSE,
                confidence=0.6,
                reason=f"응답이 너무 길음 ({len(response)} chars > {self.max_response_length})",
                suggested_retry=False
            )

        # 3. "Unable to Answer" 패턴 검증
        result = self._check_unable_to_answer(response)
        if result:
            return result

        # 4. 불완전한 응답 패턴
        result = self._check_incomplete_response(response)
        if result:
            return result

        # 5. 반복적인 응답 검증
        result = self._check_repetitive_response(response)
        if result:
            return result

        # 6. 타임아웃으로 인한 절단 응답 검증
        result = self._check_timeout_cutoff(response)
        if result:
            return result

        # 7. 형식 오류 검증
        result = self._check_malformed_response(response)
        if result:
            return result

        # 모든 검사 통과
        return ValidationResult(
            is_valid=True,
            failure_type=ResponseFailureType.NONE,
            confidence=0.95,
            reason="응답이 유효함",
            suggested_retry=False
        )

    def _check_unable_to_answer(self, response: str) -> Optional[ValidationResult]:
        """'Unable to answer' 패턴 확인"""
        response_lower = response.lower()

        for pattern in self.unable_to_answer_patterns:
            if re.search(pattern, response_lower, re.IGNORECASE | re.DOTALL):
                logger.warning(f"Detected 'Unable to Answer' pattern: {pattern}")
                return ValidationResult(
                    is_valid=False,
                    failure_type=ResponseFailureType.UNABLE_TO_ANSWER,
                    confidence=0.95,
                    reason=f"응답이 답변 불가 패턴 포함: {pattern}",
                    suggested_retry=True
                )

        return None

    def _check_incomplete_response(self, response: str) -> Optional[ValidationResult]:
        """불완전한 응답 패턴 확인"""
        for pattern in self.incomplete_patterns:
            if re.search(pattern, response, re.IGNORECASE | re.DOTALL):
                logger.warning(f"Detected incomplete pattern: {pattern}")
                return ValidationResult(
                    is_valid=False,
                    failure_type=ResponseFailureType.INCOMPLETE_RESPONSE,
                    confidence=0.7,
                    reason=f"응답이 불완전한 패턴 포함: {pattern}",
                    suggested_retry=True
                )

        return None

    def _check_repetitive_response(self, response: str) -> Optional[ValidationResult]:
        """반복적인 응답 검증"""
        for pattern in self.repetitive_patterns:
            matches = re.findall(pattern, response, re.IGNORECASE | re.DOTALL)
            if matches:
                logger.warning(f"Detected repetitive pattern: {pattern}")
                return ValidationResult(
                    is_valid=False,
                    failure_type=ResponseFailureType.REPETITIVE_RESPONSE,
                    confidence=0.8,
                    reason=f"응답에 반복적인 문구 포함",
                    suggested_retry=True
                )

        return None

    def _check_timeout_cutoff(self, response: str) -> Optional[ValidationResult]:
        """타임아웃으로 인한 절단 응답 검증"""
        # 특징: 갑작스럽게 끝나는 문장 (불완전한 마침)
        timeout_patterns = [
            r"[^\.\!\?:\n]$",  # 마침표 없이 끝남
            r"^.{5,10}$",  # 너무 짧으면서 마침표 없음
            r",\s*$",  # 쉼표로 끝남
            r":\s*$",  # 콜론으로 끝남
            r"^\d{1,3}\.\s",  # 리스트 형식인데 하나만
        ]

        # 추가 휴리스틱: 문장이 너무 많이 끝나지 않음
        period_count = response.count(".")
        comma_count = response.count(",")
        sentence_count = len(re.split(r"[.!?\n]", response))

        if period_count == 0 and comma_count > 2 and len(response) > 100:
            logger.warning("Detected likely timeout cutoff: no period in long response")
            return ValidationResult(
                is_valid=False,
                failure_type=ResponseFailureType.TIMEOUT_CUTOFF,
                confidence=0.6,
                reason="타임아웃으로 인한 응답 절단 추정",
                suggested_retry=True
            )

        return None

    def _check_malformed_response(self, response: str) -> Optional[ValidationResult]:
        """형식 오류 검증"""
        malformed_patterns = [
            r"^```",  # 코드 블록만 있음
            r"^```[\s\S]*```$",  # 코드 블록 형식만
            r"^#{6,}",  # 제목이 너무 많음
            r"^[\s\-\*]{3,}$",  # 구분선만
            r"^(?:[\s\-\*])*$",  # 공백과 기호만
        ]

        for pattern in malformed_patterns:
            if re.search(pattern, response, re.MULTILINE):
                logger.warning(f"Detected malformed pattern: {pattern}")
                return ValidationResult(
                    is_valid=False,
                    failure_type=ResponseFailureType.MALFORMED_RESPONSE,
                    confidence=0.85,
                    reason=f"응답의 형식이 잘못됨",
                    suggested_retry=True
                )

        return None

    def should_retry(self, validation_result: ValidationResult) -> bool:
        """재시도 여부 결정"""
        return validation_result.suggested_retry and not validation_result.is_valid

    def get_retry_suggestion(self, validation_result: ValidationResult) -> str:
        """재시도 전략 제안"""
        suggestions = {
            ResponseFailureType.UNABLE_TO_ANSWER: "쿼리 키워드 개선 필요",
            ResponseFailureType.INCOMPLETE_RESPONSE: "더 명확한 질문 필요",
            ResponseFailureType.MALFORMED_RESPONSE: "쿼리 리포매팅 필요",
            ResponseFailureType.REPETITIVE_RESPONSE: "다른 키워드로 재시도 필요",
            ResponseFailureType.TIMEOUT_CUTOFF: "타임아웃 처리 - 컨텍스트 축소 후 재시도",
            ResponseFailureType.EMPTY_RESPONSE: "LLM 서비스 상태 확인 필요",
        }
        return suggestions.get(validation_result.failure_type, "쿼리 개선 후 재시도")


class BatchResponseValidator:
    """배치 응답 검증 (모니터링/메트릭용)"""

    def __init__(self):
        self.validator = ResponseValidator()
        self.failure_counts = {}
        self.total_validations = 0

    def validate_batch(self, responses: list, queries: list = None) -> list:
        """배치 검증"""
        if queries is None:
            queries = [""] * len(responses)

        results = []
        for response, query in zip(responses, queries):
            result = self.validator.validate(response, query)
            results.append(result)

            # 통계 업데이트
            if not result.is_valid:
                failure_type = result.failure_type.value
                self.failure_counts[failure_type] = self.failure_counts.get(failure_type, 0) + 1

            self.total_validations += 1

        return results

    def get_stats(self) -> dict:
        """통계 반환"""
        return {
            "total_validations": self.total_validations,
            "failure_counts": self.failure_counts,
            "failure_rate": (sum(self.failure_counts.values()) / self.total_validations
                           if self.total_validations > 0 else 0.0),
            "most_common_failure": max(self.failure_counts.items(), key=lambda x: x[1], default=(None, 0))
        }

    def reset_stats(self):
        """통계 초기화"""
        self.failure_counts = {}
        self.total_validations = 0
