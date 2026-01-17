package com.insuretech.pms.support;

import com.insuretech.pms.project.entity.Project;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fluent builder for Project entity in tests.
 * Provides default Korean test data.
 */
public class TestProjectBuilder {

    private String id = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "테스트 프로젝트";
    private String description = "테스트용 프로젝트 설명";
    private Project.ProjectStatus status = Project.ProjectStatus.PLANNING;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(90);
    private BigDecimal budget = BigDecimal.valueOf(50_000_000);
    private Integer progress = 0;

    public TestProjectBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestProjectBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestProjectBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestProjectBuilder status(Project.ProjectStatus status) {
        this.status = status;
        return this;
    }

    public TestProjectBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public TestProjectBuilder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public TestProjectBuilder budget(BigDecimal budget) {
        this.budget = budget;
        return this;
    }

    public TestProjectBuilder progress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public TestProjectBuilder asAIProject() {
        this.name = "AI 보험심사 자동화 시스템";
        this.description = "머신러닝을 활용한 보험심사 프로세스 자동화";
        this.status = Project.ProjectStatus.IN_PROGRESS;
        this.progress = 35;
        return this;
    }

    public TestProjectBuilder asMobileProject() {
        this.name = "모바일 보험청구 플랫폼";
        this.description = "iOS 및 안드로이드 기반 보험청구 모바일 앱";
        this.status = Project.ProjectStatus.PLANNING;
        this.progress = 5;
        return this;
    }

    public TestProjectBuilder asDataAnalyticsProject() {
        this.name = "데이터 분석 대시보드";
        this.description = "실시간 보험 데이터 분석 및 시각화";
        this.status = Project.ProjectStatus.PLANNING;
        this.progress = 0;
        return this;
    }

    public TestProjectBuilder inProgress() {
        this.status = Project.ProjectStatus.IN_PROGRESS;
        this.progress = 40;
        return this;
    }

    public TestProjectBuilder planning() {
        this.status = Project.ProjectStatus.PLANNING;
        this.progress = 10;
        return this;
    }

    public TestProjectBuilder completed() {
        this.status = Project.ProjectStatus.COMPLETED;
        this.progress = 100;
        return this;
    }

    public TestProjectBuilder withBudget(long amount) {
        this.budget = BigDecimal.valueOf(amount);
        return this;
    }

    public Project build() {
        return Project.builder()
                .id(id)
                .name(name)
                .description(description)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .budget(budget)
                .progress(progress)
                .phases(new ArrayList<>())
                .build();
    }
}
