package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.entity.FcmToken;
import com.example.SafeTag_BE.repository.FcmTokenRepository;
import com.example.SafeTag_BE.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {
    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerFcmToken(@RequestBody Map<String, String> body, Authentication auth) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        }

        var user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 기존 토큰 조회
        var existing = fcmTokenRepository.findByToken(token).orElse(null);

        if (existing != null) {
            //  소유자 재바인딩 (토큰이 다른 유저에게 묶여 있었다면 현재 로그인 유저로 변경)
            existing.setUser(user);
            existing.setActive(true);
            existing.setUpdatedAt(LocalDateTime.now());
            fcmTokenRepository.save(existing);
        } else {
            // 신규 등록
            fcmTokenRepository.save(
                    FcmToken.builder()
                            .user(user)
                            .token(token)
                            .active(true)
                            .build()
            );
        }

        return ResponseEntity.ok(Map.of("status", "FCM token registered"));
    }
}
