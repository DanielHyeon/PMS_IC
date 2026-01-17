package com.insuretech.pms.support;

import com.insuretech.pms.auth.entity.User;
import java.time.LocalDateTime;

/**
 * Fluent builder for User entity in tests.
 * Provides default Korean test data.
 */
public class TestUserBuilder {

    private String id = "user-" + System.nanoTime();
    private String email = id + "@test.com";
    private String password = "encoded_password";
    private String name = "테스트 사용자";
    private User.UserRole role = User.UserRole.DEVELOPER;
    private String department = "개발팀";
    private Boolean active = true;
    private LocalDateTime lastLoginAt = null;

    public TestUserBuilder id(String id) {
        this.id = id;
        return this;
    }

    public TestUserBuilder email(String email) {
        this.email = email;
        return this;
    }

    public TestUserBuilder password(String password) {
        this.password = password;
        return this;
    }

    public TestUserBuilder name(String name) {
        this.name = name;
        return this;
    }

    public TestUserBuilder role(User.UserRole role) {
        this.role = role;
        return this;
    }

    public TestUserBuilder department(String department) {
        this.department = department;
        return this;
    }

    public TestUserBuilder active(Boolean active) {
        this.active = active;
        return this;
    }

    public TestUserBuilder lastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        return this;
    }

    public TestUserBuilder asAdmin() {
        this.role = User.UserRole.ADMIN;
        this.name = "관리자";
        this.department = "운영팀";
        return this;
    }

    public TestUserBuilder asProjectManager() {
        this.role = User.UserRole.PM;
        this.name = "프로젝트 매니저";
        this.department = "프로젝트관리팀";
        return this;
    }

    public TestUserBuilder asDeveloper() {
        this.role = User.UserRole.DEVELOPER;
        this.name = "개발자";
        this.department = "개발팀";
        return this;
    }

    public TestUserBuilder asQA() {
        this.role = User.UserRole.QA;
        this.name = "품질보증담당자";
        this.department = "QA팀";
        return this;
    }

    public TestUserBuilder asBusinessAnalyst() {
        this.role = User.UserRole.BUSINESS_ANALYST;
        this.name = "비즈니스 분석가";
        this.department = "분석팀";
        return this;
    }

    public User build() {
        return User.builder()
                .id(id)
                .email(email)
                .password(password)
                .name(name)
                .role(role)
                .department(department)
                .active(active)
                .lastLoginAt(lastLoginAt)
                .build();
    }
}
