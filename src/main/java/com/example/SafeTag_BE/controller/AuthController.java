package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.LoginRequestDto;
import com.example.SafeTag_BE.dto.LoginResponseDto;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.security.JwtTokenProvider;
import com.example.SafeTag_BE.service.UserService;
import com.example.SafeTag_BE.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        //일반 사용자 로그인 시도
        User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (user != null) {
            String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(),user.getUsername());
            return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
        }

        //관리자 로그인 시도
        Admin admin = adminService.login(loginRequest.getUsername(), loginRequest.getPassword());
        if (admin != null) {
            String token = jwtTokenProvider.generateToken(admin.getId(), admin.getRole(),user.getUsername());
            return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
        }

        //로그인 실패
        return ResponseEntity.status(401).body(new LoginResponseDto(null, "Invalid credentials"));
    }
}
