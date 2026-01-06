package com.insuretech.pms.auth.service;

import com.insuretech.pms.auth.dto.LoginRequest;
import com.insuretech.pms.auth.dto.LoginResponse;
import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.common.exception.CustomException;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 인증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다"));

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        // Redis에 Refresh Token 저장 (7일)
        String redisKey = "refresh_token:" + user.getEmail();
        redisTemplate.opsForValue().set(redisKey, refreshToken, 7, TimeUnit.DAYS);

        log.info("User logged in: {}", user.getEmail());

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.from(user))
                .build();
    }

    @Transactional
    public void logout(String email) {
        // Redis에서 Refresh Token 삭제
        String redisKey = "refresh_token:" + email;
        redisTemplate.delete(redisKey);

        // 블랙리스트에 추가 (옵션)
        SecurityContextHolder.clearContext();

        log.info("User logged out: {}", email);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw CustomException.unauthorized("인증되지 않은 사용자입니다");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다"));
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw CustomException.unauthorized("유효하지 않은 Refresh Token입니다");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // Redis에서 Refresh Token 확인
        String redisKey = "refresh_token:" + email;
        String storedToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw CustomException.unauthorized("유효하지 않은 Refresh Token입니다");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다"));

        // 새로운 Access Token 생성
        String newToken = jwtTokenProvider.generateToken(email);

        return LoginResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserInfo.from(user))
                .build();
    }
}
