package com.example.SafeTag_BE.service;

import com.example.SafeTag_BE.config.WebRtcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;               // java.time 사용
import java.util.*;

@Service
@RequiredArgsConstructor
public class TurnCredentialService {

    private final WebRtcProperties props;

    /** ICE 서버 목록 + (선택) username/credential 반환 */
    public Map<String, Object> issue() {
        return issue(props.getTurn().getTtlSeconds());
    }

    /** ttlSec을 외부에서 주입 가능하도록 오버로드 */
    public Map<String, Object> issue(int ttlSec) {
        List<Map<String, Object>> servers = new ArrayList<>();

        // 1) STUN
        for (String url : props.getStun()) {
            if (url != null && !url.isBlank()) {
                servers.add(Map.of("urls", List.of(url)));
            }
        }

        // 2) TURN(Short-term credentials)
        WebRtcProperties.Turn t = props.getTurn();
        String secret = t.getRestSecret();

        String username = null;
        String credential = null;
        int effectiveTtl = Math.max(60, ttlSec > 0 ? ttlSec : t.getTtlSeconds()); // ✅ 최소 60초
        long exp = Instant.now().getEpochSecond() + effectiveTtl;

        if (secret != null && !secret.isBlank() && t.getUrls() != null && !t.getUrls().isEmpty()) {
            username = exp + ":safetag"; // <expiry>:<label> 형식 (coturn 규격)
            credential = hmacSha1Base64(secret, username);

            // URL 목록 그대로(UDP/TCP/TLS 혼합 가능)
            Map<String, Object> turnEntry = new HashMap<>();
            turnEntry.put("urls", t.getUrls());
            turnEntry.put("username", username);
            turnEntry.put("credential", credential);
            servers.add(turnEntry);
        } else {
            // 방어: secret이나 url 없으면 TURN 미포함(로그/메트릭 권장)
        }

        // (선택) 디버깅/프론트 수동설정 편의 제공
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("iceServers", servers);
        if (username != null) {
            out.put("username", username);
            out.put("credential", credential);
            out.put("ttl", effectiveTtl);
        }
        return out;
    }

    private String hmacSha1Base64(String secret, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1"); // coturn use-auth-secret 규격
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC error", e);
        }
    }
}
