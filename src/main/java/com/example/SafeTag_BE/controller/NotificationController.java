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


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notify")
public class NotificationController {

    private final UserService userService;

    // 로그인한 사용자의 fcm 토큰 등록
    // body : { "token" : "FCM_DEVICE_TOKEN" }
    @PostMapping("/token")
    public ResponseEntity<?> registerToken(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestBody Map<String, String> body
    ) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Missing FCM token");
        }

        userService.updateFcmToken(user.id(), token);
        return ResponseEntity.ok("FCM token registered");
    }

}
