package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.entity.CallMode;
import com.example.SafeTag_BE.entity.CallSession;
import com.example.SafeTag_BE.entity.CallState;
import com.example.SafeTag_BE.repository.CallSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;

    @Value("${relay.sessionTtlSeconds:300}") // 기본 5분
    private int sessionTtl;

    /** WebRTC 요청 세션 생성 */
    @Transactional
    public CallSession createWebRtcRequested(Long ownerUserId, Long callerUserId, String qrUuid) {
        int ttl = Math.max(60, sessionTtl); // 최소 60초 보장
        String sid = UUID.randomUUID().toString();
        var now = Instant.now();
        var session = CallSession.builder()
                .ownerUserId(ownerUserId)
                .callerUserId(callerUserId)
                .sessionUuid(sid)
                .mode(CallMode.WEBRTC)
                .state(CallState.REQUESTED)
                .startedAt(now)
                .expiresAt(now.plusSeconds(ttl))
                .qrUuid(qrUuid)
                .build();
        return callSessionRepository.saveAndFlush(session);
    }

    /** 세션 존재/만료 확인 */
    @Transactional
    public CallSession get(String sessionUuid) {
        var opt = callSessionRepository.findBySessionUuid(sessionUuid);
        if (opt.isEmpty()) return null;

        var s = opt.get();
        if (s.getExpiresAt() != null && Instant.now().isAfter(s.getExpiresAt())) {
            s.setState(CallState.FAILED);
            s.setEndedAt(Instant.now());
            callSessionRepository.saveAndFlush(s);
            return null;
        }
        return s;
    }

    @Transactional
    public void markWaitingPeers(String sessionUuid){
        callSessionRepository.findBySessionUuid(sessionUuid)
                .ifPresent(s -> { s.setState(CallState.WAITING_PEERS); callSessionRepository.save(s); });
    }

    @Transactional
    public void markConnected(String sessionUuid){
        callSessionRepository.findBySessionUuid(sessionUuid)
                .ifPresent(s -> { s.setState(CallState.CONNECTED); callSessionRepository.save(s); });
    }

    @Transactional
    public void endByUuid(String sessionUuid, boolean ok){
        callSessionRepository.findBySessionUuid(sessionUuid)
                .ifPresent(s -> {
                    s.setState(ok ? CallState.ENDED : CallState.FAILED);
                    s.setEndedAt(Instant.now());
                    callSessionRepository.save(s);
                });
    }
}
