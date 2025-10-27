//package com.example.SafeTag_BE.controller;
//
//import com.example.SafeTag_BE.dto.LoginRequestDto;
//import com.example.SafeTag_BE.dto.LoginResponseDto;
//import com.example.SafeTag_BE.entity.User;
//import com.example.SafeTag_BE.entity.Admin;
//import com.example.SafeTag_BE.security.JwtTokenProvider;
//import com.example.SafeTag_BE.service.UserService;
//import com.example.SafeTag_BE.service.AdminService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final UserService userService;
//    private final AdminService adminService;
//    private final JwtTokenProvider jwtTokenProvider;
//
//    @PostMapping("/login")
//    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
//        //일반 사용자 로그인 시도
//        User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
//        if (user != null) {
//            String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(),user.getUsername());
//            return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
//        }
//
//        //관리자 로그인 시도
//        Admin admin = adminService.login(loginRequest.getUsername(), loginRequest.getPassword());
//        if (admin != null) {
//            String token = jwtTokenProvider.generateToken(admin.getId(), admin.getRole(),user.getUsername());
//            return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
//        }
//
//        //로그인 실패
//        return ResponseEntity.status(401).body(new LoginResponseDto(null, "Invalid credentials"));
//    }
//}

//
//package com.example.SafeTag_BE.controller;
//
//import com.example.SafeTag_BE.dto.LoginRequestDto;
//import com.example.SafeTag_BE.dto.LoginResponseDto;
//import com.example.SafeTag_BE.entity.User;
//import com.example.SafeTag_BE.entity.Admin;
//import com.example.SafeTag_BE.security.JwtTokenProvider;
//import com.example.SafeTag_BE.service.UserService;
//import com.example.SafeTag_BE.service.AdminService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final UserService userService;
//    private final AdminService adminService;
//    private final JwtTokenProvider jwtTokenProvider;
//
//    @PostMapping("/login")
//    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto req) {
//        try {
//            // 1) USER 로그인 시도
//            User user = userService.login(req.getUsername(), req.getPassword());
//            if (user != null) {
//                String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(), user.getUsername());
//                return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
//            }
//
//            // 2) ADMIN 로그인 시도
//            Admin admin = adminService.login(req.getUsername(), req.getPassword());
//            if (admin != null) {
//                String token = jwtTokenProvider.generateToken(admin.getId(), admin.getRole(), admin.getUsername()); // ✅ FIX
//                return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
//            }
//
//            // 3) 실패 → 401
//            return ResponseEntity.status(401).body(new LoginResponseDto(null, "Bearer"));
//        } catch (Exception e) {
//            // 어떤 예외든 500로 폭발하지 말고 로그 + 401로 통일
//            log.error("Login error", e);
//            return ResponseEntity.status(401).body(new LoginResponseDto(null, "Bearer"));
//        }
//    }
//}


package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.LoginRequestDto;
import com.example.SafeTag_BE.dto.LoginResponseDto;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.security.JwtTokenProvider;
import com.example.SafeTag_BE.service.UserService;
import com.example.SafeTag_BE.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto req) {
        try {
            log.info("[LOGIN] 요청 수신 - username={}, passwordLength={}",
                    req.getUsername(), req.getPassword() != null ? req.getPassword().length() : 0);

            // 1) USER 로그인 시도
            log.info("[LOGIN] 일반 사용자 로그인 시도 - username={}", req.getUsername());
            User user = userService.login(req.getUsername(), req.getPassword());
            if (user != null) {
                String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(), user.getUsername());
                log.info("[LOGIN] 사용자 로그인 성공 - id={}, username={}, role={}",
                        user.getId(), user.getUsername(), user.getRole());
                return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
            } else {
                log.warn("[LOGIN] 일반 사용자 로그인 실패 - username={}", req.getUsername());
            }

            // 2) ADMIN 로그인 시도
            log.info("[LOGIN] 관리자 로그인 시도 - username={}", req.getUsername());
            Admin admin = adminService.login(req.getUsername(), req.getPassword());
            if (admin != null) {
                String token = jwtTokenProvider.generateToken(admin.getId(), admin.getRole(), admin.getUsername());
                log.info("[LOGIN] 관리자 로그인 성공 - id={}, username={}, role={}",
                        admin.getId(), admin.getUsername(), admin.getRole());
                return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
            } else {
                log.warn("[LOGIN] 관리자 로그인 실패 - username={}", req.getUsername());
            }

            // 3) 실패 → 401
            log.warn("[LOGIN] 로그인 실패 - username={}", req.getUsername());
            return ResponseEntity.status(401).body(new LoginResponseDto(null, "Bearer"));

        } catch (Exception e) {
            // 어떤 예외든 500으로 폭발하지 말고 로그 + 401로 통일
            log.error("[LOGIN] 로그인 처리 중 예외 발생 - username=" + req.getUsername(), e);
            return ResponseEntity.status(401).body(new LoginResponseDto(null, "Bearer"));
        }
    }
}
