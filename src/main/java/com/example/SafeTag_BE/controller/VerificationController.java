package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.VerifyStartRequestDto;
import com.example.SafeTag_BE.dto.VerifyStartResponseDto;
import com.example.SafeTag_BE.dto.VerifyStatusResponseDto;
import com.example.SafeTag_BE.security.SecurityUtil;
import com.example.SafeTag_BE.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verify")
public class VerificationController {

    private final VerificationService verificationService;

    // 사용자 인증 시작
    @PostMapping("/start")
    @PreAuthorize("hasRole('USER')")
    public VerifyStartResponseDto start(@Valid @RequestBody VerifyStartRequestDto req) {
        Long userId = SecurityUtil.getCurrentUserId(); // JWT에서 userId 추출
        return verificationService.startVerification(userId, req);
    }

    // 인증 상태 조회
    @GetMapping("/{verifyId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public VerifyStatusResponseDto status(@PathVariable Long verifyId) {
        Long userIdOrNullIfAdmin = SecurityUtil.getCurrentUserIdOrNullIfAdmin();
        return verificationService.getVerificationStatus(verifyId, userIdOrNullIfAdmin);
    }

    // 관리자 승인
    @PostMapping("/{verifyId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(@PathVariable Long verifyId,
                        @RequestBody(required = false) String reason) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        verificationService.approveVerification(verifyId, adminId, reason);
    }

    // 관리자 반려
    @PostMapping("/{verifyId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable Long verifyId,
                       @RequestBody(required = false) String reason) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        verificationService.rejectVerification(verifyId, adminId, reason);
    }
}
