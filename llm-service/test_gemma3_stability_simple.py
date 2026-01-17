"""
Simple Test Suite for Gemma 3 12B Q5 Stability Improvements
스탠드얼론 테스트 (pytest 불필요)
"""

import sys
import logging
from datetime import datetime
from response_validator import ResponseValidator, ResponseFailureType
from timeout_retry_handler import TimeoutRetryConfig, RetryHandler, AdaptiveTimeoutRetry
from response_monitoring import ResponseMonitor, ResponseMetrics

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TestResult:
    """테스트 결과"""
    def __init__(self, name: str):
        self.name = name
        self.passed = False
        self.error = None

    def __str__(self):
        status = "✅ PASS" if self.passed else "❌ FAIL"
        msg = f"{status} | {self.name}"
        if self.error:
            msg += f"\n     Error: {self.error}"
        return msg


def test_unable_to_answer_detection():
    """'Unable to answer' 패턴 감지"""
    result = TestResult("test_unable_to_answer_detection")
    try:
        validator = ResponseValidator()
        test_cases = [
            ("Unable to answer this question", ResponseFailureType.UNABLE_TO_ANSWER),
            ("답변할 수 없습니다", ResponseFailureType.UNABLE_TO_ANSWER),
        ]

        for response, expected_type in test_cases:
            validation = validator.validate(response)
            if validation.is_valid:
                result.error = f"Failed to detect: {response}"
                return result
            if validation.failure_type != expected_type:
                result.error = f"Wrong failure type for: {response} (got {validation.failure_type})"
                return result

        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_incomplete_response_detection():
    """불완전한 응답 감지"""
    result = TestResult("test_incomplete_response_detection")
    try:
        validator = ResponseValidator()
        test_cases = [
            "프로젝트 관리는...",  # 끝에 "..."
            "다음과 같습니다.",    # 불완전한 마침
        ]

        for response in test_cases:
            validation = validator.validate(response)
            if validation.is_valid:
                result.error = f"Failed to detect: {response}"
                return result
            if validation.failure_type != ResponseFailureType.INCOMPLETE_RESPONSE:
                result.error = f"Wrong failure type for: {response}"
                return result

        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_empty_response_detection():
    """빈 응답 감지"""
    result = TestResult("test_empty_response_detection")
    try:
        validator = ResponseValidator()
        validation = validator.validate("")
        if validation.is_valid:
            result.error = "Failed to detect empty response"
            return result
        if validation.failure_type != ResponseFailureType.EMPTY_RESPONSE:
            result.error = f"Wrong failure type: {validation.failure_type}"
            return result
        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_valid_response():
    """유효한 응답 검증"""
    result = TestResult("test_valid_response")
    try:
        validator = ResponseValidator()
        valid_response = (
            "프로젝트 관리는 조직의 목표를 달성하기 위해 자원, 시간, 비용 등을 "
            "효율적으로 관리하는 프로세스입니다. 이는 계획, 실행, 모니터링, "
            "제어, 종료 단계를 포함합니다."
        )
        validation = validator.validate(valid_response)
        if not validation.is_valid:
            result.error = f"Failed to validate good response: {validation.reason}"
            return result
        if validation.failure_type != ResponseFailureType.NONE:
            result.error = f"Wrong failure type: {validation.failure_type}"
            return result
        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_backoff_delay_calculation():
    """백오프 딜레이 계산 검증"""
    result = TestResult("test_backoff_delay_calculation")
    try:
        config = TimeoutRetryConfig(
            timeout_seconds=5,
            max_retries=3,
            backoff_factor=1.5,
            initial_delay_ms=50,
            max_delay_ms=500,
            jitter_enabled=False
        )
        handler = RetryHandler(config)

        delays = [handler.get_backoff_delay(i) for i in range(3)]

        # 첫 번째: 50ms = 0.05s
        if not (0.04 < delays[0] < 0.06):
            result.error = f"First delay out of range: {delays[0]}"
            return result

        # 두 번째: 50 * 1.5 = 75ms = 0.075s
        if not (0.065 < delays[1] < 0.085):
            result.error = f"Second delay out of range: {delays[1]}"
            return result

        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_response_monitoring():
    """응답 모니터링 테스트"""
    result = TestResult("test_response_monitoring")
    try:
        monitor = ResponseMonitor(window_size_hours=24)

        # 유효한 응답 2개 추가
        for i in range(2):
            metric = ResponseMetrics(
                timestamp=datetime.now(),
                response_id=f"test-{i:03d}",
                query="테스트",
                response_length=100,
                is_valid=True,
                rag_doc_count=3,
                confidence_score=0.85
            )
            monitor.record_metric(metric)

        # 실패한 응답 1개 추가
        metric = ResponseMetrics(
            timestamp=datetime.now(),
            response_id="test-003",
            query="테스트",
            response_length=0,
            is_valid=False,
            failure_type="unable_to_answer",
            rag_doc_count=0,
            confidence_score=0.0
        )
        monitor.record_metric(metric)

        stats = monitor.get_stats()

        if stats["total_requests"] != 3:
            result.error = f"Expected 3 requests, got {stats['total_requests']}"
            return result

        if stats["valid_responses"] != 2:
            result.error = f"Expected 2 valid responses, got {stats['valid_responses']}"
            return result

        # 성공률은 약 66.67% (부동소수점 비교)
        if not (66.0 < stats["success_rate"] < 67.0):
            result.error = f"Expected ~66.67% success rate, got {stats['success_rate']}"
            return result

        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def test_critical_patterns_detection():
    """위험한 패턴 감지"""
    result = TestResult("test_critical_patterns_detection")
    try:
        monitor = ResponseMonitor(window_size_hours=24)

        # 많은 실패 추가
        for i in range(5):
            metric = ResponseMetrics(
                timestamp=datetime.now(),
                response_id=f"fail-{i:03d}",
                query="테스트",
                response_length=0,
                is_valid=False,
                failure_type="unable_to_answer",
                rag_doc_count=0,
            )
            monitor.record_metric(metric)

        patterns = monitor.get_critical_patterns(threshold=0.5)

        if len(patterns) == 0:
            result.error = "No critical patterns detected"
            return result

        if patterns[0]["failure_type"] != "unable_to_answer":
            result.error = f"Wrong pattern type: {patterns[0]['failure_type']}"
            return result

        if patterns[0]["rate"] < 50:
            result.error = f"Rate should be >= 50%, got {patterns[0]['rate']}"
            return result

        result.passed = True
    except Exception as e:
        result.error = str(e)
    return result


def run_all_tests():
    """모든 테스트 실행"""
    print("=" * 80)
    print("Gemma 3 12B Q5 Stability - Test Suite")
    print("=" * 80)
    print()

    tests = [
        # Response Validator 테스트
        test_unable_to_answer_detection,
        test_incomplete_response_detection,
        test_empty_response_detection,
        test_valid_response,

        # Timeout/Retry 테스트
        test_backoff_delay_calculation,

        # Monitoring 테스트
        test_response_monitoring,
        test_critical_patterns_detection,
    ]

    results = []
    for test in tests:
        try:
            result = test()
            results.append(result)
            print(result)
        except Exception as e:
            result = TestResult(test.__name__)
            result.error = f"Unhandled exception: {str(e)}"
            results.append(result)
            print(result)

    print()
    print("=" * 80)
    print("Test Summary")
    print("=" * 80)

    passed = sum(1 for r in results if r.passed)
    failed = sum(1 for r in results if not r.passed)
    total = len(results)

    print(f"Total:  {total}")
    print(f"Passed: {passed} ✅")
    print(f"Failed: {failed} ❌")
    print(f"Success Rate: {passed/total*100:.1f}%")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    exit_code = run_all_tests()
    sys.exit(exit_code)
