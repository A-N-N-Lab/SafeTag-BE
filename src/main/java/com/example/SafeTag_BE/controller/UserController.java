package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.UserCreateDto;
import com.example.SafeTag_BE.dto.UserResponseDto;
import com.example.SafeTag_BE.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.example.SafeTag_BE.security.JwtTokenProvider;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user") // 회원 관리 API
@Tag(name = "User API", description = "회원 관련 API")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입 API
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입합니다.")
    public ResponseEntity<?> signup(@Valid @RequestBody UserCreateDto userCreateDto,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("유효성 검사 실패");
        }

        if (!userCreateDto.getPassword1().equals(userCreateDto.getPassword2())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        try {
            userService.create(
                    userCreateDto.getUsername(),
                    userCreateDto.getEmail(),
                    userCreateDto.getPassword1(),
                    userCreateDto.getGender(),
                    userCreateDto.getPhoneNum()
            );
            return ResponseEntity.ok("회원가입 성공");
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("이미 등록된 사용자입니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }

    //내 정보 조회 API
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 가져옵니다.")
    public ResponseEntity<UserResponseDto> getMyInfo(@RequestHeader("Authorization") String token) {
        String username = jwtTokenProvider.getUsernameFromToken(token.replace("Bearer ", ""));
        UserResponseDto userResponse = userService.login(username, null);

        return ResponseEntity.ok(userResponse);
    }
}
