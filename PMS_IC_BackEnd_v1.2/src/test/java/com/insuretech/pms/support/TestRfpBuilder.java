package com.insuretech.pms.support;

import com.insuretech.pms.rfp.entity.ProcessingStatus;
import com.insuretech.pms.rfp.entity.Rfp;
import com.insuretech.pms.rfp.entity.RfpStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Fluent builder for RFP entity in tests.
 * Provides default Korean test data.
 */
public class TestRfpBuilder {

    private String id = UUID.randomUUID().toString();
    private String projectId = "proj-" + UUID.randomUUID().toString().substring(0, 8);
    private String title = "요구사항 정의서";
    private String content = "테스트용 RFP 내용";
    private String filePath = null;
    private String fileName = null;
    private String fileType = null;
    private Long fileSize = null;
    private RfpStatus status = RfpStatus.DRAFT;
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    private String processingMessage = null;
    private LocalDateTime submittedAt = null;
    private String tenantId;

    public TestRfpBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestRfpBuilder projectId(String projectId) {
        this.projectId = projectId;
        this.tenantId = projectId;
        return this;
    }

    public TestRfpBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TestRfpBuilder content(String content) {
        this.content = content;
        return this;
    }

    public TestRfpBuilder filePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public TestRfpBuilder fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public TestRfpBuilder fileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public TestRfpBuilder fileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public TestRfpBuilder status(RfpStatus status) {
        this.status = status;
        return this;
    }

    public TestRfpBuilder processingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
        return this;
    }

    public TestRfpBuilder processingMessage(String processingMessage) {
        this.processingMessage = processingMessage;
        return this;
    }

    public TestRfpBuilder submittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public TestRfpBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public TestRfpBuilder asDraft() {
        this.status = RfpStatus.DRAFT;
        this.processingStatus = ProcessingStatus.PENDING;
        return this;
    }

    public TestRfpBuilder asSubmitted() {
        this.status = RfpStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.PENDING;
        return this;
    }

    public TestRfpBuilder asUnderReview() {
        this.status = RfpStatus.UNDER_REVIEW;
        this.submittedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.EXTRACTING;
        return this;
    }

    public TestRfpBuilder asApproved() {
        this.status = RfpStatus.APPROVED;
        this.submittedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.COMPLETED;
        return this;
    }

    public TestRfpBuilder asRejected() {
        this.status = RfpStatus.REJECTED;
        this.submittedAt = LocalDateTime.now();
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingMessage = "요구사항이 불명확함";
        return this;
    }

    public TestRfpBuilder asProcessing() {
        this.status = RfpStatus.UNDER_REVIEW;
        this.processingStatus = ProcessingStatus.EXTRACTING;
        this.submittedAt = LocalDateTime.now();
        return this;
    }

    public TestRfpBuilder withFile(String fileName, String fileType, Long fileSize) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = "/uploads/" + UUID.randomUUID() + "/" + fileName;
        return this;
    }

    public TestRfpBuilder asAIRequirements() {
        this.title = "AI 보험심사 요구사항서";
        this.content = "머신러닝 기반 보험심사 자동화 시스템 요구사항";
        return this;
    }

    public TestRfpBuilder asMobileRequirements() {
        this.title = "모바일 앱 요구사항서";
        this.content = "iOS 및 안드로이드 보험청구 앱 요구사항";
        return this;
    }

    public TestRfpBuilder asSecurityRequirements() {
        this.title = "보안 요구사항서";
        this.content = "정보보안 및 개인정보보호 요구사항";
        return this;
    }

    public Rfp build() {
        Rfp rfp = Rfp.builder()
                .id(id)
                .projectId(projectId)
                .title(title)
                .content(content)
                .filePath(filePath)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(fileSize)
                .status(status)
                .processingStatus(processingStatus)
                .processingMessage(processingMessage)
                .submittedAt(submittedAt)
                .tenantId(tenantId != null ? tenantId : projectId)
                .requirements(new ArrayList<>())
                .build();
        return rfp;
    }
}
