"""
Test Suite for Gemma 3 12B Q5 Stability Improvements
불안정성 시나리오 시뮬레이션 및 테스트
"""

import pytest
import logging
from typing import List
from unittest.mock import Mock, patch, MagicMock
from response_validator import ResponseValidator, ResponseFailureType
from timeout_retry_handler import (
    TimeoutRetryConfig,
    CombinedTimeoutRetry,
    TimeoutException,
    RetryHandler,
    AdaptiveTimeoutRetry
)
from response_monitoring import ResponseMonitor, ResponseMetrics, ResponseMonitoringLogger
from datetime import datetime
import time

logger = logging.getLogger(__name__)


class TestResponseValidator:
    """응답 검증 테스트"""

    def setup_method(self):
        """테스트 전 설정"""
        self.validator = ResponseValidator()

    def test_unable_to_answer_detection(self):
        """'Unable to answer' 패턴 감지"""
        test_cases = [
            "Unable to answer this question",
            "답변할 수 없습니다",
            "답변 불가",
            "모르겠습니다",
        ]

        for response in test_cases:
            result = self.validator.validate(response)
            assert not result.is_valid, f"Failed to detect: {response}"
            assert result.failure_type == ResponseFailureType.UNABLE_TO_ANSWER

    def test_incomplete_response_detection(self):
        """불완전한 응답 감지"""
        test_cases = [
            "프로젝트 관리는...",  # 끝에 "..."
            "다음과 같습니다.",    # 불완전한 마침
        ]

        for response in test_cases:
            result = self.validator.validate(response)
            assert not result.is_valid, f"Failed to detect: {response}"
            assert result.failure_type == ResponseFailureType.INCOMPLETE_RESPONSE

    def test_empty_response_detection(self):
        """빈 응답 감지"""
        result = self.validator.validate("")
        assert not result.is_valid
        assert result.failure_type == ResponseFailureType.EMPTY_RESPONSE

    def test_too_short_response_detection(self):
        """너무 짧은 응답 감지"""
        result = self.validator.validate("네")
        assert not result.is_valid
        assert result.failure_type == ResponseFailureType.INCOMPLETE_RESPONSE

    def test_valid_response(self):
        """유효한 응답 검증"""
        valid_response = (
            "프로젝트 관리는 조직의 목표를 달성하기 위해 자원, 시간, 비용 등을 "
            "효율적으로 관리하는 프로세스입니다. 이는 계획, 실행, 모니터링, "
            "제어, 종료 단계를 포함합니다."
        )
        result = self.validator.validate(valid_response)
        assert result.is_valid
        assert result.failure_type == ResponseFailureType.NONE

    def test_repetitive_response_detection(self):
        """반복적인 응답 감지"""
        repetitive = "좋습니다 좋습니다 좋습니다"
        result = self.validator.validate(repetitive)
        assert not result.is_valid
        assert result.failure_type == ResponseFailureType.REPETITIVE_RESPONSE

    def test_timeout_cutoff_detection(self):
        """타임아웃 절단 감지"""
        cutoff_response = "프로젝트 관리의 중요한 측면은 다음과 같습니다,"
        result = self.validator.validate(cutoff_response)
        # 마침표 없이 쉼표로 끝나는 경우 타임아웃 의심
        assert result.failure_type in [
            ResponseFailureType.TIMEOUT_CUTOFF,
            ResponseFailureType.INCOMPLETE_RESPONSE
        ]


class TestRetryHandler:
    """재시도 핸들러 테스트"""

    def setup_method(self):
        """테스트 전 설정"""
        self.config = TimeoutRetryConfig(
            timeout_seconds=5,
            max_retries=3,
            backoff_factor=1.5,
            initial_delay_ms=50,
            max_delay_ms=500,
            jitter_enabled=False  # 테스트용 지터 비활성화
        )
        self.handler = RetryHandler(self.config)

    def test_backoff_delay_calculation(self):
        """백오프 딜레이 계산 검증"""
        delays = [self.handler.get_backoff_delay(i) for i in range(3)]

        # 첫 번째: 50ms = 0.05s
        assert 0.04 < delays[0] < 0.06

        # 두 번째: 50 * 1.5 = 75ms = 0.075s
        assert 0.065 < delays[1] < 0.085

        # 세 번째: 50 * 1.5^2 = 112.5ms = 0.1125s
        assert 0.10 < delays[2] < 0.125

    def test_max_retry_limit(self):
        """최대 재시도 한계 검증"""
        attempt = 3
        exception = RuntimeError("Test error")

        should_retry = self.handler.should_retry(attempt, exception)
        assert not should_retry

    def test_non_retryable_exception(self):
        """재시도 불가능한 예외 처리"""
        attempt = 0
        exception = ValueError("Invalid value")

        should_retry = self.handler.should_retry(attempt, exception)
        assert not should_retry

    def test_execute_with_retry_success(self):
        """성공적인 재시도 실행"""
        call_count = 0

        def failing_function():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise RuntimeError("Temporary failure")
            return "Success"

        result = self.handler.execute_with_retry(failing_function)
        assert result == "Success"
        assert call_count == 2

    def test_execute_with_retry_max_attempts(self):
        """최대 재시도 횟수 도달"""
        def always_fails():
            raise RuntimeError("Persistent failure")

        with pytest.raises(RuntimeError):
            self.handler.execute_with_retry(always_fails)


class TestCombinedTimeoutRetry:
    """타임아웃 + 재시도 결합 테스트"""

    def setup_method(self):
        """테스트 전 설정"""
        self.config = TimeoutRetryConfig(
            timeout_seconds=2,
            max_retries=2,
            backoff_factor=1.5,
            initial_delay_ms=50,
        )
        self.handler = CombinedTimeoutRetry(self.config)

    def test_successful_execution_within_timeout(self):
        """타임아웃 내 성공적 실행"""
        def quick_function():
            return "Success"

        result = self.handler.execute(quick_function)
        assert result == "Success"

    def test_execution_exceeding_timeout(self):
        """타임아웃 초과"""
        def slow_function():
            time.sleep(3)
            return "Should not reach here"

        with pytest.raises(TimeoutException):
            self.handler.execute(slow_function)


class TestAdaptiveTimeoutRetry:
    """적응형 타임아웃 + 재시도 테스트"""

    def setup_method(self):
        """테스트 전 설정"""
        self.initial_config = TimeoutRetryConfig(
            timeout_seconds=5,
            max_retries=3,
        )
        self.handler = AdaptiveTimeoutRetry(self.initial_config)

    def test_timeout_increase_on_repeated_failures(self):
        """반복 실패 시 타임아웃 증가"""
        # 3번 연속 타임아웃
        for i in range(3):
            self.handler.record_execution(
                success=False,
                execution_time_seconds=5.5,
                timeout_occurred=True
            )

        initial_timeout = self.handler.current_config.timeout_seconds
        self.handler.adjust_config()
        adjusted_timeout = self.handler.current_config.timeout_seconds

        assert adjusted_timeout > initial_timeout

    def test_timeout_decrease_on_consistent_success(self):
        """연속 성공 시 타임아웃 감소"""
        # 초기 타임아웃을 높게 설정
        self.handler.current_config.timeout_seconds = 10

        # 3번 연속 성공
        for i in range(3):
            self.handler.record_execution(
                success=True,
                execution_time_seconds=0.5,
                timeout_occurred=False
            )

        initial_timeout = self.handler.current_config.timeout_seconds
        self.handler.adjust_config()
        adjusted_timeout = self.handler.current_config.timeout_seconds

        assert adjusted_timeout < initial_timeout


class TestResponseMonitoring:
    """응답 모니터링 테스트"""

    def setup_method(self):
        """테스트 전 설정"""
        self.monitor = ResponseMonitor(window_size_hours=24)

    def test_record_metric(self):
        """메트릭 기록"""
        metric = ResponseMetrics(
            timestamp=datetime.now(),
            response_id="test-001",
            query="프로젝트 진척",
            response_length=100,
            is_valid=True,
            rag_doc_count=3,
            confidence_score=0.85
        )

        self.monitor.record_metric(metric)
        assert len(self.monitor.metrics) == 1

    def test_get_stats(self):
        """통계 조회"""
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
            self.monitor.record_metric(metric)

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
        self.monitor.record_metric(metric)

        stats = self.monitor.get_stats()

        assert stats["total_requests"] == 3
        assert stats["valid_responses"] == 2
        assert stats["invalid_responses"] == 1
        assert stats["success_rate"] == pytest.approx(66.67, rel=0.1)

    def test_critical_patterns_detection(self):
        """위험한 패턴 감지"""
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
            self.monitor.record_metric(metric)

        patterns = self.monitor.get_critical_patterns(threshold=0.5)

        assert len(patterns) > 0
        assert patterns[0]["failure_type"] == "unable_to_answer"
        assert patterns[0]["rate"] >= 50


class TestIntegrationGemma3Stability:
    """Gemma 3 안정성 통합 테스트"""

    def test_full_recovery_flow(self):
        """전체 회복 흐름"""
        validator = ResponseValidator()
        retry_config = TimeoutRetryConfig(
            timeout_seconds=5,
            max_retries=3,
            backoff_factor=1.5,
        )
        retry_handler = RetryHandler(retry_config)

        # 시나리오: 첫 응답 실패, 재시도 후 성공
        responses = [
            "Unable to answer",  # 첫 번째: 실패
            "프로젝트 관리의 정의는 조직의 목표를 달성하기 위해..."  # 두 번째: 성공
        ]

        first_validation = validator.validate(responses[0])
        assert not first_validation.is_valid

        second_validation = validator.validate(responses[1])
        assert second_validation.is_valid

    def test_stability_metrics_collection(self):
        """안정성 메트릭 수집"""
        monitor = ResponseMonitor(window_size_hours=24)

        # 10개의 응답 시뮬레이션 (7개 성공, 3개 실패)
        for i in range(7):
            metric = ResponseMetrics(
                timestamp=datetime.now(),
                response_id=f"ok-{i:03d}",
                query="테스트",
                response_length=100 + i * 10,
                is_valid=True,
                rag_doc_count=3,
                confidence_score=0.75 + i * 0.02,
            )
            monitor.record_metric(metric)

        for i in range(3):
            metric = ResponseMetrics(
                timestamp=datetime.now(),
                response_id=f"fail-{i:03d}",
                query="테스트",
                response_length=10,
                is_valid=False,
                failure_type=["unable_to_answer", "incomplete_response", "timeout_cutoff"][i],
                retry_count=i,
                total_retries_applied=i,
            )
            monitor.record_metric(metric)

        stats = monitor.get_stats()

        assert stats["total_requests"] == 10
        assert stats["success_rate"] == 70.0
        assert len(stats["failure_distribution"]) == 3


# 테스트 실행 헬퍼 함수
def run_stability_test_suite():
    """안정성 테스트 스위트 실행"""
    print("=" * 80)
    print("Gemma 3 12B Q5 Stability Test Suite")
    print("=" * 80)

    # pytest 실행
    pytest.main([
        __file__,
        "-v",
        "--tb=short",
        "-s"
    ])


if __name__ == "__main__":
    run_stability_test_suite()
