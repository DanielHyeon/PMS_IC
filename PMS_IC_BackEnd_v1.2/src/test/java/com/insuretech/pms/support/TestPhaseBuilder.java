package com.insuretech.pms.support;

import com.insuretech.pms.project.entity.Phase;
import com.insuretech.pms.project.entity.Project;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fluent builder for Phase entity in tests.
 * Provides default Korean test data.
 */
public class TestPhaseBuilder {

    private String id = "phase-" + UUID.randomUUID().toString().substring(0, 8);
    private Project project;
    private String name = "테스트 페이즈";
    private Integer orderNum = 1;
    private Phase.PhaseStatus status = Phase.PhaseStatus.NOT_STARTED;
    private Phase.GateStatus gateStatus = null;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(30);
    private Integer progress = 0;
    private String description = "테스트 페이즈 설명";
    private Phase.TrackType trackType = Phase.TrackType.COMMON;

    public TestPhaseBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestPhaseBuilder project(Project project) {
        this.project = project;
        return this;
    }

    public TestPhaseBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestPhaseBuilder orderNum(Integer orderNum) {
        this.orderNum = orderNum;
        return this;
    }

    public TestPhaseBuilder status(Phase.PhaseStatus status) {
        this.status = status;
        return this;
    }

    public TestPhaseBuilder gateStatus(Phase.GateStatus gateStatus) {
        this.gateStatus = gateStatus;
        return this;
    }

    public TestPhaseBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public TestPhaseBuilder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public TestPhaseBuilder progress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public TestPhaseBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestPhaseBuilder trackType(Phase.TrackType trackType) {
        this.trackType = trackType;
        return this;
    }

    public TestPhaseBuilder asRequirementAnalysisPhase() {
        this.name = "요구사항 분석";
        this.description = "비즈니스 요구사항 정의 및 분석";
        this.orderNum = 1;
        this.status = Phase.PhaseStatus.NOT_STARTED;
        return this;
    }

    public TestPhaseBuilder asDesignPhase() {
        this.name = "설계 및 계획";
        this.description = "시스템 아키텍처 및 상세 설계";
        this.orderNum = 2;
        this.status = Phase.PhaseStatus.NOT_STARTED;
        return this;
    }

    public TestPhaseBuilder asDevelopmentPhase() {
        this.name = "개발 구현";
        this.description = "코드 개발 및 통합 구현";
        this.orderNum = 3;
        this.status = Phase.PhaseStatus.IN_PROGRESS;
        this.progress = 40;
        return this;
    }

    public TestPhaseBuilder asTestingPhase() {
        this.name = "테스트 및 QA";
        this.description = "단위, 통합, 시스템 테스트 수행";
        this.orderNum = 4;
        this.status = Phase.PhaseStatus.NOT_STARTED;
        return this;
    }

    public TestPhaseBuilder asDeploymentPhase() {
        this.name = "배포 준비";
        this.description = "프로덕션 환경 배포 준비";
        this.orderNum = 5;
        this.status = Phase.PhaseStatus.NOT_STARTED;
        return this;
    }

    public TestPhaseBuilder asMaintenancePhase() {
        this.name = "운영 및 유지보수";
        this.description = "시스템 운영 및 장애 대응";
        this.orderNum = 6;
        this.status = Phase.PhaseStatus.NOT_STARTED;
        return this;
    }

    public TestPhaseBuilder inProgress() {
        this.status = Phase.PhaseStatus.IN_PROGRESS;
        this.progress = 50;
        return this;
    }

    public TestPhaseBuilder completed() {
        this.status = Phase.PhaseStatus.COMPLETED;
        this.progress = 100;
        return this;
    }

    public TestPhaseBuilder onHold() {
        this.status = Phase.PhaseStatus.ON_HOLD;
        return this;
    }

    public TestPhaseBuilder trackTypeAI() {
        this.trackType = Phase.TrackType.AI;
        return this;
    }

    public TestPhaseBuilder trackTypeSI() {
        this.trackType = Phase.TrackType.SI;
        return this;
    }

    public Phase build() {
        return Phase.builder()
                .id(id)
                .project(project)
                .name(name)
                .orderNum(orderNum)
                .status(status)
                .gateStatus(gateStatus)
                .startDate(startDate)
                .endDate(endDate)
                .progress(progress)
                .description(description)
                .trackType(trackType)
                .build();
    }
}
