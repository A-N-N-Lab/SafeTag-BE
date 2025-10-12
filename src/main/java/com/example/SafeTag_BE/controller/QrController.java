//package com.example.SafeTag_BE.controller;
//
//import com.example.SafeTag_BE.dto.CreateQrRequestDto;
//import com.example.SafeTag_BE.entity.DynamicQR;
//import com.example.SafeTag_BE.exception.QrGoneException;
//import com.example.SafeTag_BE.repository.UserRepository;
//import com.example.SafeTag_BE.service.QrService;
//import com.example.SafeTag_BE.exception.QrGoneException;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.*;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@Tag(name = "QR API", description = "QR 생성, 회전, 메타/이미지 조회, 중계 호출 API")
//@RestController
//@RequestMapping("/api/qrs")
//@RequiredArgsConstructor
//public class QrController {
//
//    private final QrService qrService;
//    private final UserRepository userRepository;
//
//    @Operation(summary = "QR 발급/회전", description = "force=true 이면 무조건 새 발급, 아니면 남은시간 10초 이하일 때 회전")
//    @PostMapping("/issue-or-rotate")
//    public ResponseEntity<?> issueOrRotate(
//            Authentication auth,
//            @RequestParam(name = "force", defaultValue = "false") boolean force,
//            @RequestParam(name = "userId", required = false) Long userIdParam
//    ) {
//        Long userId;
//        if (auth != null && auth.getName() != null) {
//            userId = resolveUserId(auth);
//        } else if (userIdParam != null) {
//            userId = userIdParam;
//        } else {
//            // 공개 호출인데 userId가 없으면 에러
//            return ResponseEntity.badRequest().body(Map.of("error", "userId required for public call"));
//        }
//        var qr = qrService.issueOrRotate(userId, force);
//        return ResponseEntity.ok(Map.of(
//                "qrId", qr.getId(),
//                "expiredAt", String.valueOf(qr.getExpiredAt())
//        ));
//    }
//
//
//    @Operation(summary = "QR 메타 조회", description = "만료 시 410(GONE) 반환")
//    @GetMapping("/{qrId}")
//    public ResponseEntity<?> getMeta(@PathVariable Long qrId) {
//        return ResponseEntity.ok(qrService.meta(qrId));
//    }
//
//    @Operation(summary = "QR 이미지 조회", description = "QR ID를 기반으로 이미지(PNG)를 반환합니다.")
//    @GetMapping("/{qrId}/image")
//    public ResponseEntity<byte[]> getQrImage(@PathVariable Long qrId) throws Exception {
//        DynamicQR qr = qrService.getValidByIdOrGone(qrId);
//        byte[] imageBytes = qrService.generateQrImage(qr.getQrValue());
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.IMAGE_PNG);
//
//        // 캐시 방지 헤더
//        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
//        headers.add("Pragma", "no-cache");
//        headers.add("Expires", "0");
//
//        // 클라이언트 동기화를 위한 만료 시간 헤더
//        headers.add("Expires-At", String.valueOf(qr.getExpiredAt()));
//
//        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
//    }
//
//    @Operation(summary = "QR 생성(수동)", description = "사용자 ID를 받아 QR 코드를 생성합니다.")
//    @PostMapping
//    public ResponseEntity<?> createQr(@RequestBody CreateQrRequestDto dto) {
//        return ResponseEntity.ok(qrService.createQr(dto));
//    }
//
//    @Operation(summary = "QR 유효성 확인", description = "QR code 문자열로 유효성/만료를 확인합니다.")
//    @GetMapping("/by-code/{code}")
//    public ResponseEntity<?> validateByCode(@PathVariable String code) {
//        try {
//            return ResponseEntity.ok(qrService.validate(code));
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("valid", false, "reason", e.getMessage()));
//        }
//    }
//
//    @Operation(summary = "QR 중계 전화번호 요청", description = "QR 값을 기반으로 중계용 전화번호(마스킹)를 반환합니다.")
//    @GetMapping("/proxy-call")
//    public ResponseEntity<String> proxyCall(@RequestParam String qrValue) {
//        try {
//            String phone = qrService.getProxyPhoneNumber(qrValue);
//            return ResponseEntity.ok("중계 연결용 전화번호: " + phone);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @ExceptionHandler(QrGoneException.class)
//    public ResponseEntity<?> handleGone(QrGoneException e) {
//        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
//                "code", "QR_EXPIRED",
//                "message", e.getMessage()
//        ));
//    }
//
//    // Authentication → userId 해석
//    private Long resolveUserId(Authentication auth) {
//        if (auth == null || auth.getName() == null) {
//            throw new IllegalArgumentException("인증 정보가 없습니다.");
//        }
//        try {
//            return Long.valueOf(auth.getName());
//        } catch (NumberFormatException e) {
//            var user = userRepository.findByUsername(auth.getName())
//                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//            return user.getId();
//        }
//    }
//
//}
package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.CreateQrRequestDto;
import com.example.SafeTag_BE.entity.DynamicQR;
import com.example.SafeTag_BE.exception.QrGoneException;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Tag(name = "QR API", description = "QR 생성, 회전, 메타/이미지 조회, 중계 호출 API")
@RestController
@RequestMapping("/api/qrs")
@RequiredArgsConstructor
public class QrController {

    private final QrService qrService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter RFC1123_GMT =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));

    // ===== Utils =====
    private Instant toInstantUTC(Object expiredAt) {
        if (expiredAt == null) return null;
        if (expiredAt instanceof Instant i) return i;
        if (expiredAt instanceof OffsetDateTime odt) return odt.toInstant();
        if (expiredAt instanceof ZonedDateTime zdt) return zdt.toInstant();
        if (expiredAt instanceof LocalDateTime ldt) return ldt.toInstant(ZoneOffset.UTC);
        if (expiredAt instanceof LocalDate ld) return ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        // 문자열로 저장했다면 파싱 시도
        if (expiredAt instanceof CharSequence cs) {
            String s = cs.toString().trim();
            try { return Instant.parse(s); } catch (Exception ignored) {}
            // 2025-10-08T10:20:30 형태(오프셋 없는 LocalDateTime)도 고려
            try { return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC); } catch (Exception ignored) {}
        }
        throw new IllegalArgumentException("Unsupported expiredAt type: " + expiredAt.getClass());
    }

    private long ttlSecondsFrom(Instant exp) {
        if (exp == null) return 0L;
        long ttl = Duration.between(Instant.now(), exp).getSeconds();
        return Math.max(0L, ttl);
    }

    // ===== Endpoints =====

    @Operation(summary = "QR 발급/회전", description = "force=true 이면 무조건 새 발급, 아니면 남은시간 10초 이하일 때 회전")
    @PostMapping("/issue-or-rotate")
    public ResponseEntity<?> issueOrRotate(
            Authentication auth,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            @RequestParam(name = "userId", required = false) Long userIdParam
    ) {
        Long userId;
        if (auth != null && auth.getName() != null) {
            userId = resolveUserId(auth);
        } else if (userIdParam != null) {
            userId = userIdParam;
        } else {
            // 공개 호출인데 userId가 없으면 에러
            return ResponseEntity.badRequest().body(Map.of("error", "userId required for public call"));
        }

        DynamicQR qr = qrService.issueOrRotate(userId, force);
        Instant exp = toInstantUTC(qr.getExpiredAt());
        long ttl = ttlSecondsFrom(exp);

        return ResponseEntity.ok(Map.of(
                "qrId", qr.getId(),
                "imageUrl", "/api/qrs/" + qr.getId() + "/image",
                "expiresAt", exp != null ? ISO.format(exp) : null,
                "ttlSeconds", ttl
        ));
    }

    @Operation(summary = "QR 메타 조회", description = "만료 시 410(GONE) 반환")
    @GetMapping("/{qrId}")
    public ResponseEntity<?> getMeta(@PathVariable Long qrId) {
        Map<String, Object> meta = qrService.meta(qrId); // 불변일 수 있음

        // ⚠ 불변일 수 있으니 복사해서 작업
        java.util.Map<String, Object> safe = new java.util.HashMap<>(meta);

        Object expiredAtObj = safe.get("expiredAt");
        try {
            java.time.Instant exp = null;
            if (expiredAtObj != null) {
                String s = String.valueOf(expiredAtObj).trim();
                try {
                    // "2025-10-08T12:34:56" 같은 LocalDateTime 문자열
                    exp = java.time.LocalDateTime.parse(s)
                            .atZone(java.time.ZoneId.systemDefault()).toInstant();
                } catch (Exception ignore) {
                    try { exp = java.time.Instant.parse(s); } catch (Exception ignored) {}
                }
            }
            if (exp != null) {
                long ttl = Math.max(0L, java.time.Duration.between(java.time.Instant.now(), exp).getSeconds());
                safe.putIfAbsent("expiredAtIso", java.time.format.DateTimeFormatter.ISO_INSTANT.format(exp));
                safe.putIfAbsent("ttlSeconds", ttl);
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok(safe);
    }


    @Operation(summary = "QR 이미지 조회", description = "QR ID를 기반으로 이미지(PNG)를 반환합니다.")
    @GetMapping("/{qrId}/image")
    public ResponseEntity<byte[]> getQrImage(@PathVariable Long qrId) throws Exception {
        DynamicQR qr = qrService.getValidByIdOrGone(qrId);
        byte[] imageBytes = qrService.generateQrImage(qr.getQrValue());

        Instant exp = toInstantUTC(qr.getExpiredAt());
        long ttl = ttlSecondsFrom(exp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        // 캐시 방지 + 실제 만료시각 전달
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        if (exp != null) {
            headers.add(HttpHeaders.EXPIRES, RFC1123_GMT.format(ZonedDateTime.ofInstant(exp, ZoneId.of("GMT"))));
            headers.add("Expires-At", ISO.format(exp));   // 라즈파이 스크립트가 읽는 핵심 헤더
            headers.add("X-QR-Expires-At", ISO.format(exp)); // 보조
            headers.add("X-QR-TTL", Long.toString(ttl));  // 라즈파이가 바로 TTL을 쓸 수 있도록
        } else {
            headers.add(HttpHeaders.EXPIRES, "0");
            headers.add("Expires-At", "");
            headers.add("X-QR-TTL", "0");
        }

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
