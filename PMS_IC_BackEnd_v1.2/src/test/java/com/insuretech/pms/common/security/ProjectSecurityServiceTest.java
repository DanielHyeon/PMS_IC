package com.insuretech.pms.common.security;

import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.project.entity.Project;
import com.insuretech.pms.project.entity.ProjectMember;
import com.insuretech.pms.project.entity.ProjectMember.ProjectRole;
import com.insuretech.pms.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectSecurityService Tests")
class ProjectSecurityServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectSecurityService projectSecurityService;

    private static final String PROJECT_ID = "proj-001";
    private static final String USER_ID = "user-001";
    private static final String USER_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuthentication(String email, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private User createUser(String id, String email, User.UserRole role) {
        return User.builder()
                .id(id)
                .email(email)
                .role(role)
                .name("Test User")
                .build();
    }

    private ProjectMember createProjectMember(String projectId, String userId, ProjectRole role) {
        Project project = Project.builder()
                .id(projectId)
                .name("Test Project")
                .build();

        return ProjectMember.builder()
                .id("pm-001")
                .project(project)
                .userId(userId)
                .role(role)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("hasRole")
    class HasRole {

        @Test
        @DisplayName("Should return true when user has the required role on the project")
        void shouldReturnTrueWhenUserHasRole() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PM);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.PM);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.hasRole(PROJECT_ID, "PM");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user has a different role on the project")
        void shouldReturnFalseWhenUserHasDifferentRole() {
            // Given
            setupAuthentication(USER_EMAIL, "DEVELOPER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.hasRole(PROJECT_ID, "PM");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when user is not a member of the project")
        void shouldReturnFalseWhenUserNotMember() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PM);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // When
            boolean result = projectSecurityService.hasRole(PROJECT_ID, "PM");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when user is system ADMIN regardless of project membership")
        void shouldReturnTrueForSystemAdmin() {
            // Given
            setupAuthentication(USER_EMAIL, "ADMIN");

            // When
            boolean result = projectSecurityService.hasRole(PROJECT_ID, "PM");

            // Then
            assertThat(result).isTrue();
            verify(projectMemberRepository, never()).findByProjectIdAndUserIdAndActiveTrue(any(), any());
        }
    }

    @Nested
    @DisplayName("hasAnyRole")
    class HasAnyRole {

        @Test
        @DisplayName("Should return true when user has one of the required roles")
        void shouldReturnTrueWhenUserHasOneOfRoles() {
            // Given
            setupAuthentication(USER_EMAIL, "DEVELOPER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.hasAnyRole(PROJECT_ID, "PM", "DEVELOPER", "QA");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user has none of the required roles")
        void shouldReturnFalseWhenUserHasNoneOfRoles() {
            // Given
            setupAuthentication(USER_EMAIL, "MEMBER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.MEMBER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.hasAnyRole(PROJECT_ID, "PM", "PMO_HEAD");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isProjectMember")
    class IsProjectMember {

        @Test
        @DisplayName("Should return true when user is an active member")
        void shouldReturnTrueWhenUserIsActiveMember() {
            // Given
            setupAuthentication(USER_EMAIL, "DEVELOPER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.existsByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(true);

            // When
            boolean result = projectSecurityService.isProjectMember(PROJECT_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not a member")
        void shouldReturnFalseWhenUserIsNotMember() {
            // Given
            setupAuthentication(USER_EMAIL, "DEVELOPER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.existsByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(false);

            // When
            boolean result = projectSecurityService.isProjectMember(PROJECT_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true for ADMIN regardless of membership")
        void shouldReturnTrueForAdmin() {
            // Given
            setupAuthentication(USER_EMAIL, "ADMIN");

            // When
            boolean result = projectSecurityService.isProjectMember(PROJECT_ID);

            // Then
            assertThat(result).isTrue();
            verify(projectMemberRepository, never()).existsByProjectIdAndUserIdAndActiveTrue(any(), any());
        }

        @Test
        @DisplayName("Should return true for AUDITOR regardless of membership")
        void shouldReturnTrueForAuditor() {
            // Given
            setupAuthentication(USER_EMAIL, "AUDITOR");

            // When
            boolean result = projectSecurityService.isProjectMember(PROJECT_ID);

            // Then
            assertThat(result).isTrue();
            verify(projectMemberRepository, never()).existsByProjectIdAndUserIdAndActiveTrue(any(), any());
        }
    }

    @Nested
    @DisplayName("hasSystemRole")
    class HasSystemRole {

        @Test
        @DisplayName("Should return true when user has the system role")
        void shouldReturnTrueWhenUserHasSystemRole() {
            // Given
            setupAuthentication(USER_EMAIL, "ADMIN");

            // When
            boolean result = projectSecurityService.hasSystemRole("ADMIN");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user does not have the system role")
        void shouldReturnFalseWhenUserDoesNotHaveSystemRole() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");

            // When
            boolean result = projectSecurityService.hasSystemRole("ADMIN");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when no authentication exists")
        void shouldReturnFalseWhenNoAuth() {
            // Given - no authentication set

            // When
            boolean result = projectSecurityService.hasSystemRole("ADMIN");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getProjectRole")
    class GetProjectRole {

        @Test
        @DisplayName("Should return the project role when user is a member")
        void shouldReturnProjectRoleWhenMember() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PM);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.PM);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            Optional<ProjectRole> result = projectSecurityService.getProjectRole(PROJECT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(ProjectRole.PM);
        }

        @Test
        @DisplayName("Should return empty when user is not a member")
        void shouldReturnEmptyWhenNotMember() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PM);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ProjectRole> result = projectSecurityService.getProjectRole(PROJECT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("canManageMembers")
    class CanManageMembers {

        @Test
        @DisplayName("Should return true for PM on the project")
        void shouldReturnTrueForPM() {
            // Given
            setupAuthentication(USER_EMAIL, "PM");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PM);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.PM);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.canManageMembers(PROJECT_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for PMO_HEAD on the project")
        void shouldReturnTrueForPMOHead() {
            // Given
            setupAuthentication(USER_EMAIL, "PMO_HEAD");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.PMO_HEAD);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.PMO_HEAD);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.canManageMembers(PROJECT_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for DEVELOPER on the project")
        void shouldReturnFalseForDeveloper() {
            // Given
            setupAuthentication(USER_EMAIL, "DEVELOPER");
            User user = createUser(USER_ID, USER_EMAIL, User.UserRole.DEVELOPER);
            ProjectMember member = createProjectMember(PROJECT_ID, USER_ID, ProjectRole.DEVELOPER);

            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
            when(projectMemberRepository.findByProjectIdAndUserIdAndActiveTrue(PROJECT_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            // When
            boolean result = projectSecurityService.canManageMembers(PROJECT_ID);

            // Then
            assertThat(result).isFalse();
        }
    }
}
