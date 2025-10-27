package com.example.SafeTag_BE.controller;

import com.example.SafeTag_BE.security.CustomUserPrincipal;
import com.example.SafeTag_BE.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.SafeTag_BE.service.FcmService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify")
public class NotificationController {

    private final UserService userService;
    private final FcmService fcmService;

    // body: { "token": "FCM_DEVICE_TOKEN" }
    @PostMapping("/token")
    public ResponseEntity<?> registerToken(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestBody Map<String, String> body
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Missing FCM token");
        }

        userService.updateFcmToken(user.id(), token);
        return ResponseEntity.ok("FCM token registered");
    }

    @PostMapping("/test")
    public ResponseEntity<?> sendTest(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // 1 로그인한 사용자의 fcmToken 가져오기
        String token = userService.getUser(user.id()).getFcmToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("No FCM token registered");
        }

        try {
            // 2 실제 푸시 전송
            String response = fcmService.sendToToken(token, "테스트 알림", "SafeTag에서 보낸 테스트 메시지", null);
            return ResponseEntity.ok("FCM sent successfully: " + response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("FCM send failed: " + e.getMessage());
        }
    }

}
