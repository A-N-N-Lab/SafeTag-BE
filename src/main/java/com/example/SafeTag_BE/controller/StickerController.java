//package com.example.SafeTag_BE.controller;
//
//import com.example.SafeTag_BE.dto.StickerIssueRequestDto;
//import com.example.SafeTag_BE.dto.StickerIssueResponseDto;
//import com.example.SafeTag_BE.store.InMemoryStore;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.concurrent.atomic.AtomicLong;
//
//@RestController
//@RequestMapping("/api/sticker")
//@Tag(name ="sticker-controller",description = "스티커 관련 API (거주민·임산부·장애인 인증 스티커 발급 )")
//public class StickerController {
//
//    private static final AtomicLong SEQ = new AtomicLong(1); // 데모용 Long ID 시퀀스
//    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
//
//    @PostMapping("/issue")
//    public StickerIssueResponseDto issue(@RequestBody StickerIssueRequestDto req) {
//        long stickerId = SEQ.getAndIncrement();
//
//        int days = (req.getValidDays() == null) ? 30 : req.getValidDays();
//        Instant expInstant = InMemoryStore.plusDays(days);
//        LocalDateTime expLdt = LocalDateTime.ofInstant(expInstant, ZONE);
//        InMemoryStore.STICKERS.put(Long.toString(stickerId), expInstant);
//        String qr = "QR:STICKER:" + stickerId;
//        return new StickerIssueResponseDto(stickerId, qr, expLdt);
//    }
//}

package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.enums.VerificationStatus;
import com.example.SafeTag_BE.enums.VerificationType;
import com.example.SafeTag_BE.dto.StickerIssueRequestDto;
import com.example.SafeTag_BE.dto.StickerIssueResponseDto;
import com.example.SafeTag_BE.entity.Sticker;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.repository.StickerRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.store.InMemoryStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/sticker")
@RequiredArgsConstructor
@Tag(name ="sticker-controller",description = "스티커 관련 API (거주민·임산부·장애인 인증 스티커 발급)")
public class StickerController {

    private static final AtomicLong SEQ = new AtomicLong(1); // 데모용 시퀀스(미사용 가능)
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final StickerRepository stickerRepository;
    private final UserRepository userRepository;

    private String defaultImage(VerificationType t) {
        return switch (t) {
            case PREGNANT -> "/static/templates/pregnant.png";
            case DISABLED -> "/static/templates/disabled.png";
            case RESIDENT -> "/static/templates/resident.png";
        };
    }

    @PostMapping("/issue")
    public StickerIssueResponseDto issue(@RequestBody StickerIssueRequestDto req) {
        log.info("[StickerController] HIT /api/sticker/issue req={}", req);

        // 1) 유효기간 계산
        int days = (req.getValidDays() == null || req.getValidDays() == 0) ? 730 : req.getValidDays();
        ZoneId ZONE = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(ZONE);
        LocalDate expDate = today.plusDays(days);

        // 2) imageUrl 설정
        String imageUrl = (req.getImageUrl() == null || req.getImageUrl().isBlank())
                ? defaultImage(req.getStickerType())
                : req.getImageUrl();

        // 3) userId 추출
        Long userId = req.getUserId();
        if (userId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
                userId = userRepository.findByUsername(ud.getUsername()).map(User::getId).orElse(null);
            }
        }

        // 4) Sticker 엔티티 생성
        Sticker sticker = Sticker.builder()
                .carNumber(req.getCarNumber())
                .imageUrl(imageUrl)
                .issuedAt(today)
                .expiresAt(expDate)
                .validFrom(today)
                .validTo(expDate)
                .issuer("SAFETAG")
                .status(VerificationStatus.APPROVED)
                .type(req.getStickerType())
                .userId(userId)
                .build();

        //  5) sticker_no 생성 추가 (여기가 중요!)
        String stickerNo = "ST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        sticker.setStickerNo(stickerNo);

        // 6) DB 저장
        Sticker saved = stickerRepository.save(sticker);
        Long id = saved.getId();

        // 7) 인메모리 만료 관리
        Instant expInstant = expDate.atStartOfDay(ZONE).toInstant();
        InMemoryStore.STICKERS.put(id.toString(), expInstant);

        // 8) 응답 반환
        String qr = "QR:STICKER:" + id;
        return new StickerIssueResponseDto(id, qr, expDate.atStartOfDay(), imageUrl, req.getStickerType().name());
    }


    @GetMapping("/sticker/{id}")
    public ResponseEntity<?> getSticker(@PathVariable String id) {
        Instant exp = InMemoryStore.STICKERS.get(id);
        if (exp == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", "NOT_FOUND",
                    "message", "해당 스티커가 존재하지 않습니다."
            ));
        }
        if (Instant.now().isAfter(exp)) {
            InMemoryStore.STICKERS.remove(id);
            return ResponseEntity.status(410).body(Map.of(
                    "status", "EXPIRED",
                    "message", "스티커가 만료되었습니다."
            ));
        }
        long remainingSec = exp.getEpochSecond() - Instant.now().getEpochSecond();
        return ResponseEntity.ok(Map.of(
                "status", "ACTIVE",
                "expiresAt", exp.toString(),
                "remainingSeconds", Math.max(0, remainingSec)
        ));
    }
}
