package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.CreateQrRequestDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.exception.QrGoneException;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.service.QrService;
import com.example.SafeTag_BE.exception.QrGoneException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "QR API", description = "QR 생성, 회전, 메타/이미지 조회, 중계 호출 API")
@RestController
@RequestMapping("/api/qrs")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;
    private final UserRepository userRepository;

    @Operation(summary = "QR 발급/회전", description = "남은 시간 10초 이하면 새 QR 발급, 아니면 기존 유지")
    @PostMapping("/issue-or-rotate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> issueOrRotate(Authentication auth) {
        Long userId = resolveUserId(auth);
        var qr = qrService.issueOrRotate(userId);
        return ResponseEntity.ok(Map.of(
                "qrId", qr.getId(),
                "expiredAt", String.valueOf(qr.getExpiredAt())
        ));
    }

    @Operation(summary = "QR 메타 조회", description = "만료 시 410(GONE) 반환")
    @GetMapping("/{qrId}")
    public ResponseEntity<?> getMeta(@PathVariable Long qrId) {
        return ResponseEntity.ok(qrService.meta(qrId));
    }

    @Operation(summary = "QR 이미지 조회", description = "QR ID를 기반으로 이미지(PNG)를 반환합니다.")
    @GetMapping("/{qrId}/image")
    public ResponseEntity<byte[]> getQrImage(@PathVariable Long qrId) throws Exception {
        DynamicQR qr = qrService.getValidByIdOrGone(qrId);
        byte[] imageBytes = qrService.generateQrImage(qr.getQrValue());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        // 캐시 방지 헤더
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        // 클라이언트 동기화를 위한 만료 시간 헤더
        headers.add("Expires-At", String.valueOf(qr.getExpiredAt()));

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @Operation(summary = "QR 생성(수동)", description = "사용자 ID를 받아 QR 코드를 생성합니다.")
    @PostMapping
    public ResponseEntity<?> createQr(@RequestBody CreateQrRequestDto dto) {
        return ResponseEntity.ok(qrService.createQr(dto));
    }

    @Operation(summary = "QR 유효성 확인", description = "QR code 문자열로 유효성/만료를 확인합니다.")
    @GetMapping("/by-code/{code}")
    public ResponseEntity<?> validateByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(qrService.validate(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("valid", false, "reason", e.getMessage()));
        }
    }

    @Operation(summary = "QR 중계 전화번호 요청", description = "QR 값을 기반으로 중계용 전화번호(마스킹)를 반환합니다.")
    @GetMapping("/proxy-call")
    public ResponseEntity<String> proxyCall(@RequestParam String qrValue) {
        try {
            String phone = qrService.getProxyPhoneNumber(qrValue);
            return ResponseEntity.ok("중계 연결용 전화번호: " + phone);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @ExceptionHandler(QrGoneException.class)
    public ResponseEntity<?> handleGone(QrGoneException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "code", "QR_EXPIRED",
                "message", e.getMessage()
        ));
    }

    // Authentication → userId 해석
    private Long resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            var user = userRepository.findByUsername(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            return user.getId();
        }
    }
}
