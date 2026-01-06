package com.insuretech.pms.auth.service;

import com.insuretech.pms.auth.dto.LoginResponse;
import com.insuretech.pms.auth.entity.User;
import com.insuretech.pms.auth.repository.UserRepository;
import com.insuretech.pms.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LoginResponse.UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(LoginResponse.UserInfo::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LoginResponse.UserInfo getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("사용자를 찾을 수 없습니다: " + id));
        return LoginResponse.UserInfo.from(user);
    }
}
