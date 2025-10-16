package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.StickerIssueRequestDto;
import com.example.SafeTag_BE.dto.StickerIssueResponseDto;
import com.example.SafeTag_BE.entity.User;
import com.example.SafeTag_BE.repository.StickerRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import com.example.SafeTag_BE.store.InMemoryStore;
import com.example.SafeTag_BE.entity.Sticker;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/sticker")
@RequiredArgsConstructor
public class StickerController {

    private static final AtomicLong SEQ = new AtomicLong(1); // 데모용 Long ID 시퀀스
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    @Autowired
    private final StickerRepository stickerRepository;     // ✅ 생성자 주입
    private final UserRepository userRepository;

    @PostMapping("/issue")
    public StickerIssueResponseDto issue(@RequestBody StickerIssueRequestDto req) {
        log.info("[StickerController] HIT /api/sticker/issue req={}", req);

        // 유효기간 계산
        int days = (req.getValidDays() == null || req.getValidDays() == 0) ? 730 : req.getValidDays();
        Instant expInstant = InMemoryStore.plusDays(days);
        LocalDateTime expLdt = LocalDateTime.ofInstant(expInstant, ZONE);
        LocalDate expDate = expLdt.toLocalDate();

        String imageUrl = req.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = switch (req.getStickerType()) {
                case PREGNANT -> "/static/templates/pregnant.png";
                case DISABLED -> "/static/templates/disabled.png";
                case RESIDENT -> "/static/templates/resident.png";
                default -> "/static/templates/default.png";
            };
        }

        Long userId = req.getUserId();
        if (userId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
                String username = ud.getUsername();
                userId = userRepository.findByUsername(username)
                        .map(User::getId)
                        .orElse(null);
            }
        }

        // 엔티티 생성
        Sticker sticker = Sticker.builder()
                .carNumber(req.getCarNumber())
                .imageUrl(imageUrl)
                .issuedAt(LocalDate.now(ZONE))
                .expiresAt(expDate)
                .issuer("SAFETAG")
                .userId(userId)
                .type(req.getStickerType())   // 단일 Enum 저장
                .build();

        Sticker saved = stickerRepository.save(sticker);
        Long id = saved.getId();

        // 인메모리 만료 관리
        InMemoryStore.STICKERS.put(id.toString(), expInstant);

        // 응답
        String qr = "QR:STICKER:" + id;
        return new StickerIssueResponseDto(id, qr, expLdt, req.getImageUrl(), req.getStickerType().name());

    }

    @GetMapping("/sticker/{id}")
    public ResponseEntity<?> getSticker(@PathVariable String id) {
        Instant exp = InMemoryStore.STICKERS.get(id);
        if (exp == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", "NOT_FOUND",
                            "message", "해당 스티커가 존재하지 않습니다."
                    ));
        }

        // 만료 검사
        if (Instant.now().isAfter(exp)) {
            InMemoryStore.STICKERS.remove(id); // 만료 즉시 삭제
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of(
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
