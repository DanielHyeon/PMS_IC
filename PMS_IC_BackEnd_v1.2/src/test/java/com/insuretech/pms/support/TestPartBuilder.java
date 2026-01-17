package com.insuretech.pms.support;

import com.insuretech.pms.project.entity.Part;
import com.insuretech.pms.project.entity.Project;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fluent builder for Part entity in tests.
 * Provides default Korean test data.
 */
public class TestPartBuilder {

    private String id = "part-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "테스트 파트";
    private String description = "테스트 파트 설명";
    private Project project;
    private String leaderId = "user-" + UUID.randomUUID().toString().substring(0, 8);
    private String leaderName = "파트 리더";
    private Part.PartStatus status = Part.PartStatus.ACTIVE;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusDays(60);
    private Integer progress = 0;
    private Set<String> memberIds = new HashSet<>();

    public TestPartBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestPartBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestPartBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestPartBuilder project(Project project) {
        this.project = project;
        return this;
    }

    public TestPartBuilder leaderId(String leaderId) {
        this.leaderId = leaderId;
        return this;
    }

    public TestPartBuilder leaderName(String leaderName) {
        this.leaderName = leaderName;
        return this;
    }

    public TestPartBuilder status(Part.PartStatus status) {
        this.status = status;
        return this;
    }

    public TestPartBuilder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public TestPartBuilder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public TestPartBuilder progress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public TestPartBuilder addMember(String userId) {
        this.memberIds.add(userId);
        return this;
    }

    public TestPartBuilder memberIds(Set<String> memberIds) {
        this.memberIds = memberIds;
        return this;
    }

    public TestPartBuilder asAIEnginePart() {
        this.name = "AI 엔진 개발팀";
        this.description = "머신러닝 모델 개발 및 최적화 담당";
        this.leaderName = "AI 팀 리더";
        return this;
    }

    public TestPartBuilder asMobileAppPart() {
        this.name = "모바일 앱 개발팀";
        this.description = "iOS 및 안드로이드 앱 개발 담당";
        this.leaderName = "모바일 팀 리더";
        return this;
    }

    public TestPartBuilder asBackendPart() {
        this.name = "백엔드 개발팀";
        this.description = "서버 및 API 개발 담당";
        this.leaderName = "백엔드 팀 리더";
        return this;
    }

    public TestPartBuilder asQAPart() {
        this.name = "QA 및 테스트팀";
        this.description = "품질보증 및 시스템 테스트 담당";
        this.leaderName = "QA 팀 리더";
        return this;
    }

    public TestPartBuilder asInfrastructurePart() {
        this.name = "인프라 및 운영팀";
        this.description = "클라우드 인프라 및 DevOps 담당";
        this.leaderName = "인프라 팀 리더";
        return this;
    }

    public TestPartBuilder asSecurityPart() {
        this.name = "보안 팀";
        this.description = "정보보안 및 취약점 분석 담당";
        this.leaderName = "보안 팀 리더";
        return this;
    }

    public TestPartBuilder active() {
        this.status = Part.PartStatus.ACTIVE;
        return this;
    }

    public TestPartBuilder inactive() {
        this.status = Part.PartStatus.INACTIVE;
        return this;
    }

    public TestPartBuilder completed() {
        this.status = Part.PartStatus.COMPLETED;
        this.progress = 100;
        return this;
    }

    public TestPartBuilder withProgress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public Part build() {
        return Part.builder()
                .id(id)
                .name(name)
                .description(description)
                .project(project)
                .leaderId(leaderId)
                .leaderName(leaderName)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .progress(progress)
                .memberIds(memberIds)
                .build();
    }
}
