package com.insuretech.pms.support;

import com.insuretech.pms.task.entity.Sprint;
import com.insuretech.pms.task.entity.UserStory;
import java.util.UUID;

/**
 * Fluent builder for UserStory entity in tests.
 * Provides default Korean test data.
 */
public class TestUserStoryBuilder {

    private String id = UUID.randomUUID().toString();
    private String projectId = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private Sprint sprint = null;
    private String title = "테스트 사용자 스토리";
    private String description = "테스트 사용자 스토리 설명";
    private String acceptanceCriteria = null;
    private UserStory.Priority priority = UserStory.Priority.MEDIUM;
    private Integer storyPoints = 5;
    private UserStory.StoryStatus status = UserStory.StoryStatus.BACKLOG;
    private String assigneeId = null;
    private String epic = null;
    private Integer priorityOrder = null;

    public TestUserStoryBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestUserStoryBuilder projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public TestUserStoryBuilder sprint(Sprint sprint) {
        this.sprint = sprint;
        return this;
    }

    public TestUserStoryBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TestUserStoryBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestUserStoryBuilder acceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
        return this;
    }

    public TestUserStoryBuilder priority(UserStory.Priority priority) {
        this.priority = priority;
        return this;
    }

    public TestUserStoryBuilder storyPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
        return this;
    }

    public TestUserStoryBuilder status(UserStory.StoryStatus status) {
        this.status = status;
        return this;
    }

    public TestUserStoryBuilder assigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public TestUserStoryBuilder epic(String epic) {
        this.epic = epic;
        return this;
    }

    public TestUserStoryBuilder priorityOrder(Integer priorityOrder) {
        this.priorityOrder = priorityOrder;
        return this;
    }

    public TestUserStoryBuilder asAIModelTraining() {
        this.title = "AI 모델 훈련 파이프라인 구축";
        this.description = "머신러닝 모델을 자동으로 훈련하는 파이프라인 시스템 개발";
        this.acceptanceCriteria = "자동 훈련 스케줄 설정 가능\\nAccuracy 기준 자동 배포\\n실시간 모니터링";
        this.priority = UserStory.Priority.HIGH;
        this.storyPoints = 8;
        this.epic = "AI 엔진";
        return this;
    }

    public TestUserStoryBuilder asDataPreprocessing() {
        this.title = "보험 청구 데이터 전처리";
        this.description = "원본 보험 청구 데이터를 머신러닝 학습에 적합하게 전처리";
        this.acceptanceCriteria = "데이터 정규화 완료\\n결측치 처리\\n이상치 제거";
        this.priority = UserStory.Priority.HIGH;
        this.storyPoints = 5;
        this.epic = "AI 엔진";
        return this;
    }

    public TestUserStoryBuilder asMobileUIBasic() {
        this.title = "모바일 앱 기본 UI 개발";
        this.description = "기본 로그인 및 메인 화면 UI 구현";
        this.acceptanceCriteria = "로그인 화면 완성\\n메인 대시보드 UI\\n네비게이션 구현";
        this.priority = UserStory.Priority.HIGH;
        this.storyPoints = 5;
        this.epic = "모바일 앱";
        return this;
    }

    public TestUserStoryBuilder asClaimStatusInquiry() {
        this.title = "청구 상태 조회 기능";
        this.description = "고객이 자신의 청구 상태를 실시간으로 조회";
        this.acceptanceCriteria = "실시간 상태 업데이트\\n상태 변경 알림\\n조회 이력 저장";
        this.priority = UserStory.Priority.MEDIUM;
        this.storyPoints = 3;
        this.epic = "모바일 앱";
        return this;
    }

    public TestUserStoryBuilder asSecurityAudit() {
        this.title = "보안 감사 및 취약점 분석";
        this.description = "OWASP Top 10 기준에 따른 보안 검증";
        this.acceptanceCriteria = "OWASP Top 10 모두 검증\\n취약점 보고서 작성\\n개선 방안 제시";
        this.priority = UserStory.Priority.CRITICAL;
        this.storyPoints = 8;
        this.epic = "보안";
        return this;
    }

    public TestUserStoryBuilder asAPIIntegration() {
        this.title = "레거시 시스템 API 연동";
        this.description = "기존 보험 관리 시스템과의 REST API 연동";
        this.acceptanceCriteria = "API 문서 작성\\n엔드포인트 테스트\\n오류 처리";
        this.priority = UserStory.Priority.HIGH;
        this.storyPoints = 5;
        this.epic = "통합";
        return this;
    }

    public TestUserStoryBuilder asPerformanceOptimization() {
        this.title = "시스템 성능 최적화";
        this.description = "응답 시간 3초 이하, 99.9% 가용성 달성";
        this.acceptanceCriteria = "데이터베이스 쿼리 최적화\\n캐싱 전략 수립\\n부하 테스트 완료";
        this.priority = UserStory.Priority.MEDIUM;
        this.storyPoints = 5;
        this.epic = "성능";
        return this;
    }

    public TestUserStoryBuilder highPriority() {
        this.priority = UserStory.Priority.HIGH;
        return this;
    }

    public TestUserStoryBuilder criticalPriority() {
        this.priority = UserStory.Priority.CRITICAL;
        return this;
    }

    public TestUserStoryBuilder lowPriority() {
        this.priority = UserStory.Priority.LOW;
        return this;
    }

    public TestUserStoryBuilder backlog() {
        this.status = UserStory.StoryStatus.BACKLOG;
        return this;
    }

    public TestUserStoryBuilder selected() {
        this.status = UserStory.StoryStatus.SELECTED;
        return this;
    }

    public TestUserStoryBuilder inProgress() {
        this.status = UserStory.StoryStatus.IN_PROGRESS;
        return this;
    }

    public TestUserStoryBuilder completed() {
        this.status = UserStory.StoryStatus.COMPLETED;
        return this;
    }

    public TestUserStoryBuilder cancelled() {
        this.status = UserStory.StoryStatus.CANCELLED;
        return this;
    }

    public TestUserStoryBuilder withStoryPoints(Integer points) {
        this.storyPoints = points;
        return this;
    }

    public UserStory build() {
        return UserStory.builder()
                .id(id)
                .projectId(projectId)
                .sprint(sprint)
                .title(title)
                .description(description)
                .acceptanceCriteria(acceptanceCriteria)
                .priority(priority)
                .storyPoints(storyPoints)
                .status(status)
                .assigneeId(assigneeId)
                .epic(epic)
                .priorityOrder(priorityOrder)
                .build();
    }
}
