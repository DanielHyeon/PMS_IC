package com.insuretech.pms.auth.service;

import com.insuretech.pms.auth.dto.LoginRequest;
import com.insuretech.pms.auth.dto.LoginResponse;
import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.common.exception.CustomException;
import com.insuretech.pms.project.entity.ProjectMember;
import com.insuretech.pms.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Retrieve user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> CustomException.notFound("User not found"));

        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT tokens
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Store refresh token in Redis
        storeRefreshToken(user.getEmail(), refreshToken);

        log.info("User logged in: {}", user.getEmail());

        List<LoginResponse.ProjectRoleInfo> projectRoles = buildProjectRoleInfo(user.getId());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.from(user))
                .projectRoles(projectRoles)
                .build();
    }

    @Transactional
    public void logout(String email) {
        // Delete refresh token from Redis
        deleteRefreshToken(email);

        // Clear security context
        SecurityContextHolder.clearContext();

        log.info("User logged out: {}", email);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw CustomException.unauthorized("User is not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> CustomException.notFound("User not found"));
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw CustomException.unauthorized("Invalid refresh token");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // Verify refresh token in Redis
        String storedToken = getStoredRefreshToken(email);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw CustomException.unauthorized("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> CustomException.notFound("User not found"));

        // Generate new access token
        String newToken = jwtTokenProvider.generateToken(email);

        List<LoginResponse.ProjectRoleInfo> projectRoles = buildProjectRoleInfo(user.getId());

        return LoginResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.from(user))
                .projectRoles(projectRoles)
                .build();
    }

    private void storeRefreshToken(String email, String token) {
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, token, REFRESH_TOKEN_EXPIRY_DAYS, TimeUnit.DAYS);
    }

    private void deleteRefreshToken(String email) {
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + email;
        redisTemplate.delete(redisKey);
    }

    private String getStoredRefreshToken(String email) {
        String redisKey = REFRESH_TOKEN_KEY_PREFIX + email;
        return (String) redisTemplate.opsForValue().get(redisKey);
    }

    private List<LoginResponse.ProjectRoleInfo> buildProjectRoleInfo(String userId) {
        List<ProjectMember> memberships = projectMemberRepository.findByUserIdAndActiveTrue(userId);
        return memberships.stream()
                .map(LoginResponse.ProjectRoleInfo::from)
                .toList();
    }
}
