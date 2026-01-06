package com.insuretech.pms.common.init;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing demo users...");
            createDemoUsers();
            log.info("Demo users created successfully");
        }
    }

    private void createDemoUsers() {
        createUser("U001", "sponsor@insure.com", "sponsor123", "이사장", User.UserRole.SPONSOR, "경영진");
        createUser("U002", "pmo@insure.com", "pmo123", "PMO 총괄", User.UserRole.PMO_HEAD, "PMO");
        createUser("U003", "pm@insure.com", "pm123", "김철수", User.UserRole.PM, "IT혁신팀");
        createUser("U004", "dev@insure.com", "dev123", "박민수", User.UserRole.DEVELOPER, "AI개발팀");
        createUser("U005", "qa@insure.com", "qa123", "최지훈", User.UserRole.QA, "품질보증팀");
        createUser("U006", "ba@insure.com", "ba123", "이영희", User.UserRole.BUSINESS_ANALYST, "보험심사팀");
        createUser("U007", "auditor@insure.com", "auditor123", "감리인", User.UserRole.AUDITOR, "외부감리법인");
        createUser("U008", "admin@insure.com", "admin123", "시스템관리자", User.UserRole.ADMIN, "IT운영팀");
    }

    private void createUser(String id, String email, String password, String name, User.UserRole role, String department) {
        User user = User.builder()
                .id(id)
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(role)
                .department(department)
                .active(true)
                .build();
        userRepository.save(user);
    }
}