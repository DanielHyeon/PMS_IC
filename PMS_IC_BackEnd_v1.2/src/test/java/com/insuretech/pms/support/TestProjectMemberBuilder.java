package com.insuretech.pms.support;

import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.entity.ProjectMember;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fluent builder for ProjectMember entity in tests.
 * Provides default Korean test data.
 */
public class TestProjectMemberBuilder {

    private String id = "pm-" + UUID.randomUUID().toString().substring(0, 8);
    private Project project;
    private String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
    private String userName = "팀원";
    private String userEmail = userId + "@test.com";
    private ProjectMember.ProjectRole role = ProjectMember.ProjectRole.MEMBER;
    private String department = "개발팀";
    private Boolean active = true;
    private LocalDateTime joinedAt = LocalDateTime.now();

    public TestProjectMemberBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestProjectMemberBuilder project(Project project) {
        this.project = project;
        return this;
    }

    public TestProjectMemberBuilder userId(String userId) {
        this.userId = userId;
        return this;
    }

    public TestProjectMemberBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    public TestProjectMemberBuilder userEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public TestProjectMemberBuilder role(ProjectMember.ProjectRole role) {
        this.role = role;
        return this;
    }

    public TestProjectMemberBuilder department(String department) {
        this.department = department;
        return this;
    }

    public TestProjectMemberBuilder active(Boolean active) {
        this.active = active;
        return this;
    }

    public TestProjectMemberBuilder joinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
        return this;
    }

    public TestProjectMemberBuilder asSponsor() {
        this.role = ProjectMember.ProjectRole.SPONSOR;
        this.userName = "스폰서";
        this.department = "경영진";
        return this;
    }

    public TestProjectMemberBuilder asProjectManager() {
        this.role = ProjectMember.ProjectRole.PM;
        this.userName = "프로젝트 매니저";
        this.department = "프로젝트관리팀";
        return this;
    }

    public TestProjectMemberBuilder asPMOHead() {
        this.role = ProjectMember.ProjectRole.PMO_HEAD;
        this.userName = "PMO 담당자";
        this.department = "PMO팀";
        return this;
    }

    public TestProjectMemberBuilder asDeveloper() {
        this.role = ProjectMember.ProjectRole.DEVELOPER;
        this.userName = "개발자";
        this.department = "개발팀";
        return this;
    }

    public TestProjectMemberBuilder asQA() {
        this.role = ProjectMember.ProjectRole.QA;
        this.userName = "QA 담당자";
        this.department = "QA팀";
        return this;
    }

    public TestProjectMemberBuilder asBusinessAnalyst() {
        this.role = ProjectMember.ProjectRole.BUSINESS_ANALYST;
        this.userName = "비즈니스 분석가";
        this.department = "분석팀";
        return this;
    }

    public TestProjectMemberBuilder asAuditor() {
        this.role = ProjectMember.ProjectRole.AUDITOR;
        this.userName = "감시자";
        this.department = "감시팀";
        return this;
    }

    public TestProjectMemberBuilder inactive() {
        this.active = false;
        return this;
    }

    public ProjectMember build() {
        return ProjectMember.builder()
                .id(id)
                .project(project)
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .role(role)
                .department(department)
                .active(active)
                .joinedAt(joinedAt)
                .build();
    }
}
