package com.insuretech.pms.support;

import com.insuretech.pms.rfp.entity.Priority;
import com.insuretech.pms.rfp.entity.Requirement;
import com.insuretech.pms.rfp.entity.RequirementCategory;
import com.insuretech.pms.rfp.entity.RequirementStatus;
import com.insuretech.pms.rfp.entity.Rfp;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

/**
 * Fluent builder for Requirement entity in tests.
 * Provides default Korean test data.
 */
public class TestRequirementBuilder {

    private String id = UUID.randomUUID().toString();
    private Rfp rfp = null;
    private String projectId = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private String code = "REQ-" + System.currentTimeMillis();
    private String title = "테스트 요구사항";
    private String description = "테스트용 요구사항 설명";
    private RequirementCategory category = RequirementCategory.FUNCTIONAL;
    private Priority priority = Priority.MEDIUM;
    private RequirementStatus status = RequirementStatus.IDENTIFIED;
    private Integer progress = 0;
    private String sourceText = null;
    private Integer pageNumber = null;
    private String assigneeId = null;
    private LocalDate dueDate = LocalDate.now().plusDays(30);
    private String acceptanceCriteria = null;
    private Integer estimatedEffort = null;
    private Integer actualEffort = null;
    private String tenantId;
    private String neo4jNodeId = null;

    public TestRequirementBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestRequirementBuilder rfp(Rfp rfp) {
        this.rfp = rfp;
        return this;
    }

    public TestRequirementBuilder projectId(String projectId) {
        this.projectId = projectId;
        this.tenantId = projectId;
        return this;
    }

    public TestRequirementBuilder code(String code) {
        this.code = code;
        return this;
    }

    public TestRequirementBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TestRequirementBuilder description(String description) {
        this.description = description;
        return this;
    }

    public TestRequirementBuilder category(RequirementCategory category) {
        this.category = category;
        return this;
    }

    public TestRequirementBuilder priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public TestRequirementBuilder status(RequirementStatus status) {
        this.status = status;
        return this;
    }

    public TestRequirementBuilder progress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public TestRequirementBuilder sourceText(String sourceText) {
        this.sourceText = sourceText;
        return this;
    }

    public TestRequirementBuilder pageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public TestRequirementBuilder assigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public TestRequirementBuilder dueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public TestRequirementBuilder acceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
        return this;
    }

    public TestRequirementBuilder estimatedEffort(Integer estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
        return this;
    }

    public TestRequirementBuilder actualEffort(Integer actualEffort) {
        this.actualEffort = actualEffort;
        return this;
    }

    public TestRequirementBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public TestRequirementBuilder neo4jNodeId(String neo4jNodeId) {
        this.neo4jNodeId = neo4jNodeId;
        return this;
    }

    public TestRequirementBuilder asAIRequirement() {
        this.category = RequirementCategory.AI;
        this.title = "AI 모델 정확도 개선";
        this.description = "머신러닝 모델의 보험심사 정확도를 90% 이상으로 개선";
        return this;
    }

    public TestRequirementBuilder asSecurityRequirement() {
        this.category = RequirementCategory.SECURITY;
        this.title = "데이터 암호화 구현";
        this.description = "모든 고객 개인정보는 AES-256 암호화 적용";
        this.priority = Priority.CRITICAL;
        return this;
    }

    public TestRequirementBuilder asFunctionalRequirement() {
        this.category = RequirementCategory.FUNCTIONAL;
        this.title = "모바일 앱 기본 기능";
        this.description = "iOS 및 안드로이드에서 청구 가능";
        return this;
    }

    public TestRequirementBuilder asIntegrationRequirement() {
        this.category = RequirementCategory.INTEGRATION;
        this.title = "레거시 시스템 연동";
        this.description = "기존 보험 관리 시스템과의 API 연동";
        return this;
    }

    public TestRequirementBuilder asNonFunctionalRequirement() {
        this.category = RequirementCategory.NON_FUNCTIONAL;
        this.title = "시스템 성능 요구사항";
        this.description = "응답 시간 3초 이하, 99.9% 가용성";
        return this;
    }

    public TestRequirementBuilder asUIRequirement() {
        this.category = RequirementCategory.UI;
        this.title = "사용자 인터페이스 설계";
        this.description = "직관적이고 사용하기 쉬운 UI 제공";
        return this;
    }

    public TestRequirementBuilder identified() {
        this.status = RequirementStatus.IDENTIFIED;
        this.progress = 0;
        return this;
    }

    public TestRequirementBuilder analyzed() {
        this.status = RequirementStatus.ANALYZED;
        this.progress = 25;
        return this;
    }

    public TestRequirementBuilder approved() {
        this.status = RequirementStatus.APPROVED;
        this.progress = 50;
        return this;
    }

    public TestRequirementBuilder implemented() {
        this.status = RequirementStatus.IMPLEMENTED;
        this.progress = 75;
        return this;
    }

    public TestRequirementBuilder verified() {
        this.status = RequirementStatus.VERIFIED;
        this.progress = 100;
        return this;
    }

    public TestRequirementBuilder highPriority() {
        this.priority = Priority.HIGH;
        return this;
    }

    public TestRequirementBuilder criticalPriority() {
        this.priority = Priority.CRITICAL;
        return this;
    }

    public TestRequirementBuilder withAcceptanceCriteria(String criteria) {
        this.acceptanceCriteria = criteria;
        return this;
    }

    public TestRequirementBuilder withEstimatedEffort(Integer hours) {
        this.estimatedEffort = hours;
        return this;
    }

    public Requirement build() {
        return Requirement.builder()
                .id(id)
                .rfp(rfp)
                .projectId(projectId)
                .code(code)
                .title(title)
                .description(description)
                .category(category)
                .priority(priority)
                .status(status)
                .progress(progress)
                .sourceText(sourceText)
                .pageNumber(pageNumber)
                .assigneeId(assigneeId)
                .dueDate(dueDate)
                .acceptanceCriteria(acceptanceCriteria)
                .estimatedEffort(estimatedEffort)
                .actualEffort(actualEffort)
                .tenantId(tenantId != null ? tenantId : projectId)
                .neo4jNodeId(neo4jNodeId)
                .linkedTaskIds(new HashSet<>())
                .build();
    }
}
