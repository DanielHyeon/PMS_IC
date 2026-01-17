package com.insuretech.pms.support;

import com.insuretech.pms.task.entity.Sprint;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fluent builder for Sprint entity in tests.
 * Provides default Korean test data.
 */
public class TestSprintBuilder {

    private String id = UUID.randomUUID().toString();
    private String projectId = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Sprint";
    private String goal = "스프린트 목표";
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(14);
    private Sprint.SprintStatus status = Sprint.SprintStatus.PLANNED;

    public TestSprintBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestSprintBuilder projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public TestSprintBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestSprintBuilder goal(String goal) {
        this.goal = goal;
        return this;
    }

    public TestSprintBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public TestSprintBuilder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public TestSprintBuilder status(Sprint.SprintStatus status) {
        this.status = status;
        return this;
    }

    public TestSprintBuilder asSprint1() {
        this.name = "Sprint 1";
        this.goal = "기본 기능 구축 및 검증";
        this.status = Sprint.SprintStatus.PLANNED;
        this.startDate = LocalDate.now().plusDays(7);
        this.endDate = LocalDate.now().plusDays(21);
        return this;
    }

    public TestSprintBuilder asSprint2() {
        this.name = "Sprint 2";
        this.goal = "기능 개선 및 성능 최적화";
        this.status = Sprint.SprintStatus.PLANNED;
        this.startDate = LocalDate.now().plusDays(21);
        this.endDate = LocalDate.now().plusDays(35);
        return this;
    }

    public TestSprintBuilder asSprint3() {
        this.name = "Sprint 3";
        this.goal = "통합 테스트 및 배포 준비";
        this.status = Sprint.SprintStatus.PLANNED;
        this.startDate = LocalDate.now().plusDays(35);
        this.endDate = LocalDate.now().plusDays(49);
        return this;
    }

    public TestSprintBuilder asAIModelSprint() {
        this.name = "AI 모델 개발 Sprint";
        this.goal = "머신러닝 모델 훈련 및 정확도 90% 달성";
        return this;
    }

    public TestSprintBuilder asMobileAppSprint() {
        this.name = "모바일 앱 개발 Sprint";
        this.goal = "기본 UI 및 청구 기능 구현";
        return this;
    }

    public TestSprintBuilder asSecuritySpint() {
        this.name = "보안 강화 Sprint";
        this.goal = "데이터 암호화 및 보안 감시 구현";
        return this;
    }

    public TestSprintBuilder planned() {
        this.status = Sprint.SprintStatus.PLANNED;
        return this;
    }

    public TestSprintBuilder active() {
        this.status = Sprint.SprintStatus.ACTIVE;
        this.startDate = LocalDate.now().minusDays(3);
        return this;
    }

    public TestSprintBuilder completed() {
        this.status = Sprint.SprintStatus.COMPLETED;
        this.startDate = LocalDate.now().minusDays(14);
        this.endDate = LocalDate.now().minusDays(1);
        return this;
    }

    public TestSprintBuilder cancelled() {
        this.status = Sprint.SprintStatus.CANCELLED;
        return this;
    }

    public TestSprintBuilder twoWeekDuration() {
        this.endDate = this.startDate.plusDays(14);
        return this;
    }

    public TestSprintBuilder threeWeekDuration() {
        this.endDate = this.startDate.plusDays(21);
        return this;
    }

    public Sprint build() {
        return Sprint.builder()
                .id(id)
                .projectId(projectId)
                .name(name)
                .goal(goal)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .build();
    }
}
