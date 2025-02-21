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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController  // 🔥 @Controller → @RestController 로 변경
@RequestMapping("/api/user")  // 🔥 /api/user 엔드포인트로 수정
@Tag(name = "User API", description = "회원가입 및 로그인 API")
public class UserController {

    private final UserService userService;

    /**
     * 🔥 회원가입 API
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "사용자 정보를 받아 회원가입합니다.")
    public ResponseEntity<?> signup(@Valid @RequestBody UserCreateDto userCreateForm,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("유효성 검사 실패");
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        try {
            userService.create(
                    userCreateForm.getUsername(),
                    userCreateForm.getEmail(),
                    userCreateForm.getPassword1(),
                    userCreateForm.getGender(),
                    userCreateForm.getPhoneNum()
            );
            return ResponseEntity.ok("회원가입 성공");
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("이미 등록된 사용자입니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }

    /**
     * 🔥 로그인 API
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 정보를 받아 로그인합니다.")
    public ResponseEntity<UserResponseDto> login(@RequestBody UserCreateDto userCreateForm) {
        UserResponseDto responseDto = userService.login(
                userCreateForm.getUsername(),
                userCreateForm.getPassword1()
        );

        if (responseDto != null) {
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.status(401).body(null);  // Unauthorized
        }
    }
}
