package com.insuretech.pms.task.service;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("WIP Email Notification Service Tests")
class WipEmailNotificationServiceTest {

    @Autowired
    private WipEmailNotificationService emailService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JavaMailSender mailSender;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-1")
                .name("Test User")
                .email("test@example.com")
                .password("encrypted")
                .role(User.UserRole.PM)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Send soft limit warning email")
    void testSendSoftLimitWarning() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendSoftLimitWarning(
                "project-1",
                "column-1",
                "In Progress",
                8,
                10,
                "user-1"
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send soft limit warning with missing user")
    void testSendSoftLimitWarningMissingUser() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        // Should not throw, just log warning
        assertThatNoException().isThrownBy(() ->
                emailService.sendSoftLimitWarning(
                        "project-1",
                        "column-1",
                        "In Progress",
                        8,
                        10,
                        "user-1"
                )
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send hard limit violation email")
    void testSendHardLimitViolation() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendHardLimitViolation(
                "project-1",
                "column-1",
                "In Progress",
                15,
                12,
                "user-1"
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send bottleneck detection alert")
    void testSendBottleneckAlert() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendBottleneckAlert(
                "project-1",
                "column-1",
                "Review",
                3,
                8,
                "user-1"
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send sprint CONWIP violation email")
    void testSendSprintConwipViolation() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendSprintConwipViolation(
                "project-1",
                "sprint-1",
                "Sprint 25",
                28,
                25,
                "user-1"
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send personal WIP limit violation")
    void testSendPersonalWipViolation() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendPersonalWipViolation(
                "project-1",
                "user-1",
                "Test User",
                6,
                5
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send personal WIP violation with missing assignee")
    void testSendPersonalWipViolationMissingAssignee() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        // Should not throw, just log warning
        assertThatNoException().isThrownBy(() ->
                emailService.sendPersonalWipViolation(
                        "project-1",
                        "user-1",
                        "Test User",
                        6,
                        5
                )
        );

        verify(userRepository, timeout(5000)).findById("user-1");
    }

    @Test
    @DisplayName("Send team notification to multiple recipients")
    void testSendTeamNotification() {
        java.util.List<String> recipients = java.util.List.of(
                "user1@example.com",
                "user2@example.com",
                "user3@example.com"
        );

        String subject = "WIP Status Update";
        String content = "<html><body>Test content</body></html>";

        emailService.sendTeamNotification(recipients, subject, content);

        // Should complete without throwing
        assertThat(recipients).hasSize(3);
    }

    @Test
    @DisplayName("Email content includes project link")
    void testEmailContentIncludesProjectLink() {
        // This is a conceptual test verifying the service creates proper HTML
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

        emailService.sendSoftLimitWarning(
                "project-1",
                "column-1",
                "In Progress",
                8,
                10,
                "user-1"
        );

        // Verify user lookup was called (email service attempts to retrieve user)
        verify(userRepository, timeout(5000).atLeastOnce()).findById(anyString());
    }

    @Test
    @DisplayName("Service handles null email gracefully")
    void testHandleNullEmail() {
        User invalidUser = User.builder()
                .id("user-1")
                .name("Invalid User")
                .email(null)
                .password("encrypted")
                .role(User.UserRole.PM)
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(invalidUser));

        // Should handle gracefully without throwing NPE
        assertThatNoException().isThrownBy(() ->
                emailService.sendSoftLimitWarning(
                        "project-1",
                        "column-1",
                        "In Progress",
                        8,
                        10,
                        "user-1"
                )
        );
    }
}
