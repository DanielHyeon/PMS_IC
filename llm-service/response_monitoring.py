"""
Response Monitoring and Metrics System
응답 실패 유형별 모니터링 및 메트릭 수집
"""

import logging
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from collections import defaultdict
import json

logger = logging.getLogger(__name__)


@dataclass
class ResponseMetrics:
    """응답 메트릭"""
    timestamp: datetime
    response_id: str
    query: str
    response_length: int
    is_valid: bool
    failure_type: Optional[str] = None
    retry_count: int = 0
    total_retries_applied: int = 0
    processing_time_ms: float = 0.0
    rag_doc_count: int = 0
    confidence_score: float = 0.0
    model_name: str = "unknown"


@dataclass
class FailureStats:
    """실패 유형별 통계"""
    failure_type: str
    count: int = 0
    total_retries: int = 0
    success_rate_after_retry: float = 0.0
    avg_processing_time_ms: float = 0.0
    examples: List[str] = field(default_factory=list)

    def add_example(self, query: str, max_examples: int = 5):
        """예제 추가 (최대 5개)"""
        if len(self.examples) < max_examples:
            self.examples.append(query)


class ResponseMonitor:
    """응답 모니터링 시스템"""

    def __init__(self, window_size_hours: int = 24):
        self.window_size = timedelta(hours=window_size_hours)
        self.metrics: List[ResponseMetrics] = []
        self.failure_stats: Dict[str, FailureStats] = defaultdict(lambda: FailureStats(failure_type="unknown"))

    def record_metric(self, metric: ResponseMetrics):
        """메트릭 기록"""
        self.metrics.append(metric)
        logger.info(
            f"Recorded metric: response_id={metric.response_id}, "
            f"is_valid={metric.is_valid}, "
            f"failure_type={metric.failure_type}, "
            f"processing_time={metric.processing_time_ms}ms"
        )

        # 실패 통계 업데이트
        if not metric.is_valid and metric.failure_type:
            stats = self.failure_stats[metric.failure_type]
            stats.count += 1
            stats.total_retries += metric.total_retries_applied
            stats.add_example(metric.query)

    def get_stats(self, failure_type: Optional[str] = None) -> Dict:
        """통계 조회"""
        now = datetime.now()
        window_start = now - self.window_size

        # 윈도우 내 메트릭만 필터링
        recent_metrics = [m for m in self.metrics if m.timestamp >= window_start]

        if failure_type:
            # 특정 실패 유형의 통계
            type_stats = self.failure_stats.get(failure_type)
            if type_stats:
                return {
                    "failure_type": failure_type,
                    "count": type_stats.count,
                    "total_retries": type_stats.total_retries,
                    "success_rate_after_retry": type_stats.success_rate_after_retry,
                    "avg_processing_time_ms": type_stats.avg_processing_time_ms,
                    "examples": type_stats.examples
                }
            return {}

        # 전체 통계
        total = len(recent_metrics)
        valid = sum(1 for m in recent_metrics if m.is_valid)
        invalid = total - valid

        response_times = [m.processing_time_ms for m in recent_metrics]
        avg_response_time = sum(response_times) / len(response_times) if response_times else 0

        avg_rag_docs = sum(m.rag_doc_count for m in recent_metrics) / total if total > 0 else 0
        avg_confidence = sum(m.confidence_score for m in recent_metrics) / total if total > 0 else 0

        # 실패 유형별 분포
        failure_distribution = {}
        for metric in recent_metrics:
            if not metric.is_valid:
                failure_type = metric.failure_type or "unknown"
                failure_distribution[failure_type] = failure_distribution.get(failure_type, 0) + 1

        return {
            "window_hours": self.window_size.total_seconds() / 3600,
            "total_requests": total,
            "valid_responses": valid,
            "invalid_responses": invalid,
            "success_rate": round(valid / total * 100, 2) if total > 0 else 0.0,
            "avg_response_time_ms": round(avg_response_time, 2),
            "avg_rag_docs_retrieved": round(avg_rag_docs, 2),
            "avg_confidence_score": round(avg_confidence, 3),
            "failure_distribution": failure_distribution,
            "failure_stats": {
                ft: {
                    "count": fs.count,
                    "total_retries": fs.total_retries,
                    "avg_retries_per_failure": round(fs.total_retries / fs.count, 2) if fs.count > 0 else 0,
                    "success_rate_after_retry": round(fs.success_rate_after_retry * 100, 2),
                    "examples": fs.examples[:3]  # 상위 3개 예제
                }
                for ft, fs in self.failure_stats.items()
            }
        }

    def get_critical_patterns(self, threshold: float = 0.1) -> List[Dict]:
        """위험한 패턴 감지 (실패율이 threshold 이상)"""
        stats = self.get_stats()
        failure_distribution = stats.get("failure_distribution", {})
        total_invalid = stats.get("invalid_responses", 0)

        critical_patterns = []
        for failure_type, count in failure_distribution.items():
            rate = count / total_invalid if total_invalid > 0 else 0
            if rate >= threshold:
                critical_patterns.append({
                    "failure_type": failure_type,
                    "rate": round(rate * 100, 2),
                    "count": count,
                    "recommendation": self._get_recommendation(failure_type)
                })

        return sorted(critical_patterns, key=lambda x: x["rate"], reverse=True)

    def _get_recommendation(self, failure_type: str) -> str:
        """실패 유형별 권장 조치"""
        recommendations = {
            "unable_to_answer": "RAG 문서 품질 검토, 쿼리 개선 전략 확인",
            "incomplete_response": "MAX_TOKENS 증가, 컨텍스트 윈도우 확대",
            "timeout_cutoff": "LLM_RESPONSE_TIMEOUT 증가, 배치 크기 감소",
            "repetitive_response": "TEMPERATURE 감소, TOP_P 조정",
            "malformed_response": "프롬프트 구조 검토, 모델 매개변수 재설정",
            "empty_response": "LLM 서비스 상태 확인, GPU 메모리 체크"
        }
        return recommendations.get(failure_type, "응답 생성 로직 검토 필요")

    def export_metrics(self, filepath: str):
        """메트릭 내보내기 (JSON)"""
        data = {
            "export_timestamp": datetime.now().isoformat(),
            "summary": self.get_stats(),
            "critical_patterns": self.get_critical_patterns(),
            "metrics_count": len(self.metrics)
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False, default=str)

        logger.info(f"Metrics exported to {filepath}")

    def reset(self):
        """모니터 초기화"""
        self.metrics.clear()
        self.failure_stats.clear()
        logger.info("Monitor reset")


class ResponseMonitoringLogger:
    """응답 모니터링 로거 (상세 로깅)"""

    def __init__(self, log_file: str = "/tmp/gemma3_response_monitoring.log"):
        self.log_file = log_file
        self.logger = logging.getLogger("response_monitoring")

        # 파일 핸들러 설정
        handler = logging.FileHandler(log_file)
        formatter = logging.Formatter(
            "%(asctime)s | %(levelname)s | %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S"
        )
        handler.setFormatter(formatter)
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)

    def log_response_validation(
        self,
        response_id: str,
        query: str,
        validation_result,
        retry_count: int = 0
    ):
        """응답 검증 결과 로깅"""
        self.logger.info(
            f"RESPONSE_VALIDATION | "
            f"id={response_id} | "
            f"query='{query[:50]}...' | "
            f"is_valid={validation_result.is_valid} | "
            f"failure_type={validation_result.failure_type.value} | "
            f"confidence={validation_result.confidence:.2f} | "
            f"retry_count={retry_count} | "
            f"reason={validation_result.reason}"
        )

    def log_retry_attempt(
        self,
        response_id: str,
        retry_num: int,
        max_retries: int,
        original_query: str,
        refined_query: str,
        failure_type: str
    ):
        """재시도 시도 로깅"""
        self.logger.info(
            f"RETRY_ATTEMPT | "
            f"id={response_id} | "
            f"attempt={retry_num}/{max_retries} | "
            f"failure_type={failure_type} | "
            f"original='{original_query[:40]}...' | "
            f"refined='{refined_query[:40]}...'"
        )

    def log_recovery_success(
        self,
        response_id: str,
        retry_num: int,
        original_query: str,
        response_length: int
    ):
        """회복 성공 로깅"""
        self.logger.info(
            f"RECOVERY_SUCCESS | "
            f"id={response_id} | "
            f"retry_num={retry_num} | "
            f"query='{original_query[:50]}...' | "
            f"response_length={response_length}"
        )

    def log_recovery_failed(
        self,
        response_id: str,
        max_retries: int,
        final_failure_type: str
    ):
        """회복 실패 로깅"""
        self.logger.info(
            f"RECOVERY_FAILED | "
            f"id={response_id} | "
            f"max_retries_reached={max_retries} | "
            f"final_failure_type={final_failure_type}"
        )

    def get_log_tail(self, lines: int = 50) -> str:
        """로그 마지막 N줄 조회"""
        try:
            with open(self.log_file, "r", encoding="utf-8") as f:
                all_lines = f.readlines()
                return "".join(all_lines[-lines:])
        except FileNotFoundError:
            return f"Log file not found: {self.log_file}"


# 전역 모니터 인스턴스
_monitor: Optional[ResponseMonitor] = None
_monitoring_logger: Optional[ResponseMonitoringLogger] = None


def get_monitor() -> ResponseMonitor:
    """전역 모니터 인스턴스 조회"""
    global _monitor
    if _monitor is None:
        _monitor = ResponseMonitor()
    return _monitor


def get_monitoring_logger() -> ResponseMonitoringLogger:
    """전역 모니터링 로거 인스턴스 조회"""
    global _monitoring_logger
    if _monitoring_logger is None:
        _monitoring_logger = ResponseMonitoringLogger()
    return _monitoring_logger
