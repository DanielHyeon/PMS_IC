package com.insuretech.pms.auth.controller;

import com.insuretech.pms.auth.dto.LoginResponse;
import com.insuretech.pms.auth.service.UserService;
import com.insuretech.pms.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Users", description = "사용자 조회 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoginResponse.UserInfo>>> getAllUsers() {
        List<LoginResponse.UserInfo> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @Operation(summary = "사용자 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoginResponse.UserInfo>> getUserById(@PathVariable String id) {
        LoginResponse.UserInfo user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
