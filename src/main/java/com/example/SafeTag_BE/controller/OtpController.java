package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.dto.OtpIssueRequestDto;
import com.example.SafeTag_BE.dto.OtpIssueResponseDto;
import com.example.SafeTag_BE.store.InMemoryStore;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private static final SecureRandom RND = new SecureRandom();
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @PostMapping("/issue")
    public OtpIssueResponseDto issue(@RequestBody OtpIssueRequestDto req) {
        String otp = String.format("%06d", RND.nextInt(1_000_000));
        Instant expInstant = InMemoryStore.plusMinutes(5);
        LocalDateTime expLdt = LocalDateTime.ofInstant(expInstant, ZONE);
        InMemoryStore.OTP.put(otp, expInstant);
        return new OtpIssueResponseDto(otp, expLdt);
    }
}
