package com.insuretech.pms.support;

import com.insuretech.pms.task.entity.KanbanColumn;
import com.insuretech.pms.task.entity.Task;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fluent builder for Task entity in tests.
 * Provides default Korean test data.
 */
public class TestTaskBuilder {

    private String id = UUID.randomUUID().toString();
    private KanbanColumn column;
    private String phaseId = null;
    private String title = "테스트 작업";
    private String description = "테스트 작업 설명";
    private String assigneeId = null;
    private Task.Priority priority = Task.Priority.MEDIUM;
    private Task.TaskStatus status = Task.TaskStatus.TODO;
    private LocalDate dueDate = LocalDate.now().plusDays(7);
    private Integer orderNum = null;
    private String tags = null;
    private Task.TrackType trackType = Task.TrackType.COMMON;

    public TestTaskBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestTaskBuilder column(KanbanColumn column) {
        this.column = column;
        return this;
    }

    public TestTaskBuilder phaseId(String phaseId) {
        this.phaseId = phaseId;
        return this;
    }

    public TestTaskBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TestTaskBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestTaskBuilder assigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public TestTaskBuilder priority(Task.Priority priority) {
        this.priority = priority;
        return this;
    }

    public TestTaskBuilder status(Task.TaskStatus status) {
        this.status = status;
        return this;
    }

    public TestTaskBuilder dueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public TestTaskBuilder orderNum(Integer orderNum) {
        this.orderNum = orderNum;
        return this;
    }

    public TestTaskBuilder tags(String tags) {
        this.tags = tags;
        return this;
    }

    public TestTaskBuilder trackType(Task.TrackType trackType) {
        this.trackType = trackType;
        return this;
    }

    public TestTaskBuilder asDatasetPreparation() {
        this.title = "데이터셋 확보 및 검증";
        this.description = "학습에 필요한 보험 청구 데이터 수집 및 품질 검증";
        this.trackType = Task.TrackType.AI;
        this.priority = Task.Priority.HIGH;
        return this;
    }

    public TestTaskBuilder asModelTuning() {
        this.title = "모델 파라미터 튜닝";
        this.description = "하이퍼파라미터 최적화를 통한 모델 성능 개선";
        this.trackType = Task.TrackType.AI;
        this.priority = Task.Priority.HIGH;
        return this;
    }

    public TestTaskBuilder asAPIEndpointDevelopment() {
        this.title = "API 엔드포인트 개발";
        this.description = "REST API 기반 예측 요청 인터페이스 구현";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.MEDIUM;
        return this;
    }

    public TestTaskBuilder asLoggingSetup() {
        this.title = "로깅 및 모니터링 설정";
        this.description = "프로덕션 환경에서의 성능 모니터링 구성";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.MEDIUM;
        return this;
    }

    public TestTaskBuilder asErrorHandling() {
        this.title = "에러 처리 및 복구 로직";
        this.description = "시스템 오류 발생 시 적절한 예외 처리 로직 구현";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.MEDIUM;
        return this;
    }

    public TestTaskBuilder asUIScreen() {
        this.title = "로그인 화면 UI 개발";
        this.description = "로그인 화면 및 입력 폼 UI 구현";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.HIGH;
        return this;
    }

    public TestTaskBuilder asSecurityTest() {
        this.title = "보안 취약점 테스트";
        this.description = "OWASP Top 10에 따른 보안 검사 수행";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.CRITICAL;
        return this;
    }

    public TestTaskBuilder asIntegrationTest() {
        this.title = "통합 테스트 케이스 작성";
        this.description = "모듈 간 통합 테스트 케이스 작성 및 실행";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.MEDIUM;
        return this;
    }

    public TestTaskBuilder asDocumentation() {
        this.title = "API 문서 작성";
        this.description = "REST API 엔드포인트 문서 및 사용 가이드 작성";
        this.trackType = Task.TrackType.COMMON;
        this.priority = Task.Priority.LOW;
        return this;
    }

    public TestTaskBuilder highPriority() {
        this.priority = Task.Priority.HIGH;
        return this;
    }

    public TestTaskBuilder criticalPriority() {
        this.priority = Task.Priority.CRITICAL;
        return this;
    }

    public TestTaskBuilder lowPriority() {
        this.priority = Task.Priority.LOW;
        return this;
    }

    public TestTaskBuilder todo() {
        this.status = Task.TaskStatus.TODO;
        return this;
    }

    public TestTaskBuilder inProgress() {
        this.status = Task.TaskStatus.IN_PROGRESS;
        this.assigneeId = "user-004";
        return this;
    }

    public TestTaskBuilder review() {
        this.status = Task.TaskStatus.REVIEW;
        return this;
    }

    public TestTaskBuilder done() {
        this.status = Task.TaskStatus.DONE;
        return this;
    }

    public TestTaskBuilder trackTypeAI() {
        this.trackType = Task.TrackType.AI;
        return this;
    }

    public TestTaskBuilder trackTypeSI() {
        this.trackType = Task.TrackType.SI;
        return this;
    }

    public TestTaskBuilder trackTypeCommon() {
        this.trackType = Task.TrackType.COMMON;
        return this;
    }

    public TestTaskBuilder assignedTo(String userId) {
        this.assigneeId = userId;
        return this;
    }

    public TestTaskBuilder dueIn(int days) {
        this.dueDate = LocalDate.now().plusDays(days);
        return this;
    }

    public TestTaskBuilder withTag(String tag) {
        this.tags = tag;
        return this;
    }

    public Task build() {
        return Task.builder()
                .id(id)
                .column(column)
                .phaseId(phaseId)
                .title(title)
                .description(description)
                .assigneeId(assigneeId)
                .priority(priority)
                .status(status)
                .dueDate(dueDate)
                .orderNum(orderNum)
                .tags(tags)
                .trackType(trackType)
                .build();
    }
}
