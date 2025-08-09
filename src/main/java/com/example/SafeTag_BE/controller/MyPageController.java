package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.MyPageUpdateDto;
import com.example.SafeTag_BE.entity.Admin;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.security.JwtTokenProvider;
import com.example.SafeTag_BE.service.AdminService;
import com.example.SafeTag_BE.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
@Tag(name = "MyPage API", description = "사용자/관리자 마이페이지 API")
public class MyPageController {

    private final UserService userService;
    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;


    @GetMapping("")
    @Operation(summary = "마이페이지 조회", description = "현재 로그인한 회원의 마이페이지 정보를 조회합니다.")
    public ResponseEntity<?> myPageView(Principal principal,
                                        @RequestHeader("Authorization") String authHeader) {
        String role = jwtTokenProvider.getRoleFromToken(authHeader.replace("Bearer ", ""));
        String username = principal.getName();

        if ("ROLE_USER".equals(role)) {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);
        } else if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminService.getAdminByUsername(username);
            return ResponseEntity.ok(admin);
        } else {
            return ResponseEntity.badRequest().body("잘못된 권한입니다.");
        }
    }


    @GetMapping("/edit")
    @Operation(summary = "마이페이지 수정 폼", description = "마이페이지 수정 폼에 기존 정보를 불러옵니다.")
    public ResponseEntity<MyPageUpdateDto> myPageForm(Principal principal,
                                                      @RequestHeader("Authorization") String authHeader) {
        String role = jwtTokenProvider.getRoleFromToken(authHeader.replace("Bearer ", ""));
        String username = principal.getName();

        MyPageUpdateDto dto = new MyPageUpdateDto();

        if ("ROLE_USER".equals(role)) {
            User user = userService.getUserByUsername(username);
            dto.setName(user.getName());
            dto.setPhoneNum(user.getPhoneNum());
            dto.setBirthDate(user.getBirthDate());
            dto.setGender(user.getGender());
            dto.setAddress(user.getAddress());
        } else if ("ROLE_ADMIN".equals(role)) {
            Admin admin = adminService.getAdminByUsername(username);
            dto.setName(admin.getName());
            dto.setPhoneNum(admin.getPhoneNum());
            dto.setBirthDate(admin.getBirthDate());
            dto.setGender(admin.getGender());
            dto.setCompany(admin.getCompany());
        }

        return ResponseEntity.ok(dto);
    }


    @PutMapping("/edit")
    @Operation(summary = "마이페이지 수정", description = "현재 로그인한 회원 정보를 수정합니다.")
    public ResponseEntity<String> updateMyPage(@RequestBody MyPageUpdateDto dto,
                                               Principal principal,
                                               @RequestHeader("Authorization") String authHeader) {
        String role = jwtTokenProvider.getRoleFromToken(authHeader.replace("Bearer ", ""));
        String username = principal.getName();

        try {
            if ("ROLE_USER".equals(role)) {
                User user = userService.getUserByUsername(username);
                userService.updateUser(
                        user.getId(),
                        dto.getName(),
                        dto.getPassword(),
                        dto.getPhoneNum(),
                        dto.getBirthDate(),
                        dto.getGender(),
                        dto.getAddress()
                );
            } else if ("ROLE_ADMIN".equals(role)) {
                Admin admin = adminService.getAdminByUsername(username);
                adminService.updateAdmin(
                        admin.getId(),
                        dto.getName(),
                        dto.getPassword(),
                        dto.getPhoneNum(),
                        dto.getBirthDate(),
                        dto.getGender(),
                        dto.getCompany()
                );
            } else {
                return ResponseEntity.badRequest().body("잘못된 권한입니다.");
            }

            return ResponseEntity.ok("수정 성공");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("수정 실패: " + e.getMessage());
        }
    }
}
