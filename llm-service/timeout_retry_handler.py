"""
Timeout and Retry Handler for Gemma 3 12B Q5
타임아웃 및 재시도 로직 구현
"""

import logging
import signal
import threading
from typing import Callable, Any, Optional, Tuple
from contextlib import contextmanager
from dataclasses import dataclass
from functools import wraps
import time

logger = logging.getLogger(__name__)


@dataclass
class TimeoutRetryConfig:
    """타임아웃 및 재시도 설정"""
    timeout_seconds: int = 30
    max_retries: int = 3
    backoff_factor: float = 1.5  # 지수 백오프 계수
    initial_delay_ms: int = 100
    max_delay_ms: int = 5000
    jitter_enabled: bool = True  # 지터 추가


class TimeoutException(Exception):
    """타임아웃 예외"""
    pass


class RetryableException(Exception):
    """재시도 가능한 예외"""
    pass


class TimeoutHandler:
    """타임아웃 핸들러 (스레드 기반)"""

    def __init__(self, timeout_seconds: int):
        self.timeout_seconds = timeout_seconds
        self.result = None
        self.exception = None
        self.thread = None
        self.completed = False

    def run_with_timeout(self, func: Callable, *args, **kwargs) -> Any:
        """타임아웃과 함께 함수 실행"""
        self.completed = False
        self.result = None
        self.exception = None

        def target():
            try:
                self.result = func(*args, **kwargs)
            except Exception as e:
                self.exception = e
            finally:
                self.completed = True

        self.thread = threading.Thread(target=target, daemon=True)
        self.thread.start()
        self.thread.join(timeout=self.timeout_seconds)

        if self.thread.is_alive():
            logger.warning(f"Function timed out after {self.timeout_seconds} seconds")
            raise TimeoutException(
                f"Operation timed out after {self.timeout_seconds} seconds"
            )

        if self.exception:
            raise self.exception

        return self.result


class RetryHandler:
    """재시도 핸들러 (지수 백오프 + 지터)"""

    def __init__(self, config: TimeoutRetryConfig):
        self.config = config

    def get_backoff_delay(self, attempt: int) -> float:
        """재시도 대기 시간 계산 (밀리초)"""
        import random

        delay = self.config.initial_delay_ms * (self.config.backoff_factor ** attempt)
        delay = min(delay, self.config.max_delay_ms)

        if self.config.jitter_enabled:
            # 지터 추가: 0~delay 범위의 랜덤 값
            jitter = random.uniform(0, delay * 0.1)  # 10% 지터
            delay += jitter

        return delay / 1000  # 초 단위로 변환

    def should_retry(self, attempt: int, exception: Exception) -> bool:
        """재시도 가능 여부 판단"""
        if attempt >= self.config.max_retries:
            logger.info(f"Max retries ({self.config.max_retries}) reached")
            return False

        # 특정 예외는 재시도하지 않음
        non_retryable_exceptions = (TypeError, ValueError, AttributeError, KeyError)
        if isinstance(exception, non_retryable_exceptions):
            logger.debug(f"Non-retryable exception: {type(exception).__name__}")
            return False

        logger.info(f"Attempt {attempt + 1}/{self.config.max_retries} will be retried")
        return True

    def execute_with_retry(
        self,
        func: Callable,
        *args,
        on_retry_callback: Optional[Callable] = None,
        **kwargs
    ) -> Any:
        """
        재시도 로직과 함께 함수 실행

        Args:
            func: 실행할 함수
            *args: 함수 인자
            on_retry_callback: 재시도 시 호출할 콜백 (attempt_num, delay_seconds 전달)
            **kwargs: 함수 키워드 인자

        Returns:
            함수 실행 결과
        """
        last_exception = None

        for attempt in range(self.config.max_retries):
            try:
                logger.info(f"Executing with retry: attempt {attempt + 1}/{self.config.max_retries}")
                return func(*args, **kwargs)

            except Exception as e:
                last_exception = e
                logger.warning(f"Attempt {attempt + 1} failed: {type(e).__name__}: {str(e)}")

                if not self.should_retry(attempt, e):
                    raise

                # 재시도 대기
                delay = self.get_backoff_delay(attempt)
                logger.info(f"Retrying in {delay:.2f} seconds...")

                if on_retry_callback:
                    on_retry_callback(attempt + 1, delay)

                time.sleep(delay)

        # 최대 재시도 횟수 도달
        raise last_exception


class CombinedTimeoutRetry:
    """타임아웃 + 재시도 결합 핸들러"""

    def __init__(self, config: TimeoutRetryConfig):
        self.config = config
        self.timeout_handler = TimeoutHandler(config.timeout_seconds)
        self.retry_handler = RetryHandler(config)

    def execute(
        self,
        func: Callable,
        *args,
        on_retry_callback: Optional[Callable] = None,
        **kwargs
    ) -> Any:
        """
        타임아웃과 재시도를 모두 적용하여 함수 실행
        """
        def func_with_timeout(*args, **kwargs):
            return self.timeout_handler.run_with_timeout(func, *args, **kwargs)

        return self.retry_handler.execute_with_retry(
            func_with_timeout,
            *args,
            on_retry_callback=on_retry_callback,
            **kwargs
        )


def with_timeout_retry(config: TimeoutRetryConfig):
    """
    데코레이터: 타임아웃과 재시도를 자동으로 적용

    Usage:
        @with_timeout_retry(TimeoutRetryConfig(timeout_seconds=30, max_retries=3))
        def my_function():
            pass
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args, **kwargs):
            handler = CombinedTimeoutRetry(config)
            return handler.execute(func, *args, **kwargs)
        return wrapper
    return decorator


@contextmanager
def timeout_context(seconds: int):
    """
    컨텍스트 매니저: 특정 시간 내에 코드 블록 실행

    Usage:
        with timeout_context(30):
            expensive_operation()
    """
    timeout_handler = TimeoutHandler(seconds)

    def signal_handler(signum, frame):
        raise TimeoutException(f"Operation timed out after {seconds} seconds")

    # 신호 기반 타임아웃 설정 (Unix only)
    try:
        signal.signal(signal.SIGALRM, signal_handler)
        signal.alarm(seconds)
        yield
    finally:
        signal.alarm(0)  # 타임아웃 해제


class AdaptiveTimeoutRetry:
    """적응형 타임아웃 및 재시도 핸들러"""

    def __init__(self, initial_config: TimeoutRetryConfig):
        self.initial_config = initial_config
        self.current_config = TimeoutRetryConfig(**initial_config.__dict__)
        self.execution_history = []

    def record_execution(
        self,
        success: bool,
        execution_time_seconds: float,
        timeout_occurred: bool = False
    ):
        """실행 기록 저장"""
        self.execution_history.append({
            "success": success,
            "execution_time": execution_time_seconds,
            "timeout_occurred": timeout_occurred,
            "timestamp": time.time()
        })

        # 최근 10개 기록만 유지
        if len(self.execution_history) > 10:
            self.execution_history = self.execution_history[-10:]

    def adjust_config(self):
        """실행 이력 기반 설정 조정"""
        if len(self.execution_history) < 3:
            return

        recent_history = self.execution_history[-3:]
        timeout_count = sum(1 for h in recent_history if h["timeout_occurred"])
        avg_time = sum(h["execution_time"] for h in recent_history) / len(recent_history)

        # 타임아웃이 빈번하면 타임아웃 시간 증가
        if timeout_count >= 2:
            new_timeout = int(self.current_config.timeout_seconds * 1.5)
            logger.info(f"Increasing timeout from {self.current_config.timeout_seconds}s to {new_timeout}s")
            self.current_config.timeout_seconds = new_timeout

        # 대부분 성공하면 타임아웃 시간 감소 (최대 요청 처리량 증가)
        success_count = sum(1 for h in recent_history if h["success"])
        if success_count == 3 and self.current_config.timeout_seconds > self.initial_config.timeout_seconds:
            new_timeout = max(self.initial_config.timeout_seconds,
                            int(self.current_config.timeout_seconds * 0.8))
            logger.info(f"Decreasing timeout from {self.current_config.timeout_seconds}s to {new_timeout}s")
            self.current_config.timeout_seconds = new_timeout

    def execute_with_adaptation(
        self,
        func: Callable,
        *args,
        **kwargs
    ) -> Any:
        """적응형 타임아웃과 재시도를 적용하여 실행"""
        handler = CombinedTimeoutRetry(self.current_config)
        start_time = time.time()
        timeout_occurred = False

        try:
            result = handler.execute(func, *args, **kwargs)
            execution_time = time.time() - start_time
            self.record_execution(success=True, execution_time_seconds=execution_time)
            self.adjust_config()
            return result

        except TimeoutException:
            execution_time = time.time() - start_time
            timeout_occurred = True
            self.record_execution(
                success=False,
                execution_time_seconds=execution_time,
                timeout_occurred=timeout_occurred
            )
            self.adjust_config()
            raise

        except Exception as e:
            execution_time = time.time() - start_time
            self.record_execution(
                success=False,
                execution_time_seconds=execution_time,
                timeout_occurred=timeout_occurred
            )
            raise


# 기본 설정 (Gemma 3 12B Q5용)
DEFAULT_GEMMA3_TIMEOUT_RETRY_CONFIG = TimeoutRetryConfig(
    timeout_seconds=30,  # 30초 타임아웃
    max_retries=3,       # 최대 3회 재시도
    backoff_factor=1.5,
    initial_delay_ms=100,
    max_delay_ms=5000,
    jitter_enabled=True
)
