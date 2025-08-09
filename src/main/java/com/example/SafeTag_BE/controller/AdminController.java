package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.AdminCreateDto;
import com.example.SafeTag_BE.dto.AdminResponseDto;
import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.security.JwtTokenProvider;
import com.example.SafeTag_BE.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API", description = "관리자 관련 API")
public class AdminController {

    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

    // 관리자 회원가입
    @PostMapping("/signup")
    @Operation(summary = "관리자 회원가입", description = "관리자 정보를 받아 회원가입합니다.")
    public ResponseEntity<?> signup(@Valid @RequestBody AdminCreateDto adminCreateDto,
                                    BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body("유효성 검사 실패");
        }

        if (!adminCreateDto.getPassword1().equals(adminCreateDto.getPassword2())) {
            return ResponseEntity.badRequest().body("비밀번호가 일치하지 않습니다.");
        }

        try {
            adminService.create(
                    adminCreateDto.getName(),
                    adminCreateDto.getUsername(),
                    adminCreateDto.getPassword1(),
                    adminCreateDto.getPhoneNum(),
                    adminCreateDto.getBirthDate(),
                    adminCreateDto.getGender(),
                    adminCreateDto.getCompany()
            );
            return ResponseEntity.ok("관리자 회원가입 성공");
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body("이미 등록된 관리자입니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }

    // 관리자 정보 조회
    @GetMapping("/me")
    @Operation(summary = "관리자 정보 조회", description = "현재 로그인된 관리자의 정보를 가져옵니다.")
    public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String token) {
        token = token.replace("Bearer ", "");
        String role = jwtTokenProvider.getRoleFromToken(token.replace("Bearer ", ""));
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.badRequest().body("관리자만 조회 가능합니다.");
        }

        Admin admin = adminService.getAdmin(userId);
        AdminResponseDto responseDto = new AdminResponseDto(
                admin.getId(),
                admin.getName(),
                admin.getUsername(),
                admin.getPhoneNum(),
                admin.getBirthDate(),
                admin.getGender(),
                admin.getCompany()
        );

        return ResponseEntity.ok(responseDto);
    }

    // 관리자 탈퇴
    @DeleteMapping("/delete")
    @Operation(summary = "관리자 탈퇴", description = "현재 로그인한 관리자의 계정을 삭제합니다.")
    public ResponseEntity<String> deleteAdmin(Principal principal,
                                              @RequestHeader("Authorization") String token) {
        String role = jwtTokenProvider.getRoleFromToken(token.replace("Bearer ", ""));
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.badRequest().body("관리자만 탈퇴할 수 있습니다.");
        }

        try {
            String username = principal.getName();
            Admin admin = adminService.getAdminByUsername(username);
            adminService.deleteAdmin(admin.getId());
            return ResponseEntity.ok("관리자 탈퇴 성공");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("관리자 탈퇴 실패: " + e.getMessage());
        }
    }
}
