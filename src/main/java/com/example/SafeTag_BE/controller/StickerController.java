package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.StickerIssueRequestDto;
import com.example.SafeTag_BE.dto.StickerIssueResponseDto;
import com.example.SafeTag_BE.store.InMemoryStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/sticker")
@Tag(name ="sticker-controller",description = "스티커 관련 API (거주민·임산부·장애인 인증 스티커 발급 )")
public class StickerController {

    private static final AtomicLong SEQ = new AtomicLong(1); // 데모용 Long ID 시퀀스
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @PostMapping("/issue")
    public StickerIssueResponseDto issue(@RequestBody StickerIssueRequestDto req) {
        long stickerId = SEQ.getAndIncrement();

        int days = (req.getValidDays() == null) ? 30 : req.getValidDays();
        Instant expInstant = InMemoryStore.plusDays(days);
        LocalDateTime expLdt = LocalDateTime.ofInstant(expInstant, ZONE);
        InMemoryStore.STICKERS.put(Long.toString(stickerId), expInstant);
        String qr = "QR:STICKER:" + stickerId;
        return new StickerIssueResponseDto(stickerId, qr, expLdt);
    }
}
