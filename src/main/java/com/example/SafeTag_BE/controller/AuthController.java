package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.LoginRequestDto;
import com.example.SafeTag_BE.dto.LoginResponseDto;
import com.example.SafeTag_BE.dto.UserResponseDto;
import com.example.SafeTag_BE.security.JwtTokenProvider;
import com.example.SafeTag_BE.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        UserResponseDto user = userService.login(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );

        if (user != null) {
            String token = jwtTokenProvider.generateToken(user.getUsername());
            return ResponseEntity.ok(new LoginResponseDto(token, "Bearer"));
        } else {
            return ResponseEntity.status(401).body(new LoginResponseDto(null, "Invalid credentials"));
        }
    }
}
