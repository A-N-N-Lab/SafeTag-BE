package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.repository.DynamicQRRepository;
import com.example.SafeTag_BE.service.CallSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.example.SafeTag_BE.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calls")
@Slf4j
public class CallApiController {

    private final DynamicQRRepository dynamicQRRepository;
    private final CallSessionService callSessionService;
    private final FcmService fcmService;

    public record StartReq(String qrUuid, Long callerUserId) {}
    public record StartRes(String sessionId, long ttlSeconds) {}

    @PostMapping("/start")
    public ResponseEntity<StartRes> start(@RequestBody StartReq req){
        if (req == null || req.qrUuid() == null || req.qrUuid().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qrUuid required");

        var qr = dynamicQRRepository.findByQrValue(req.qrUuid())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "invalid qr"));

        var user = qr.getUser();
        if (user == null)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "qr.user is null");

        var cs = callSessionService.createWebRtcRequested(user.getId(), req.callerUserId(), req.qrUuid());
        long ttl = 0;
        if (cs.getExpiresAt() != null) {
            ttl = Math.max(0, cs.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        }

        // 여기서 푸시
        try {
            var token = user.getFcmToken(); // 차주 토큰
            fcmService.sendCallRequest(token, user.getName(), cs.getSessionUuid());
            log.info("[CALL] push sent: user={}, sessionId={}", user.getUsername(), cs.getSessionUuid());
        } catch (FirebaseMessagingException e) {
            log.warn("[CALL] push failed: {}", e.getMessage());

        }

        return ResponseEntity.ok(new StartRes(cs.getSessionUuid(), ttl));
    }
}