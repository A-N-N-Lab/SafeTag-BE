package com.example.SafeTag_BE.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    public String sendToToken(String token, String title, String body, String sessionId)
            throws FirebaseMessagingException {

        Message msg = Message.builder()
                .setToken(token)
                .putData("type", "CALL_REQUEST")
                .putData("sessionId", sessionId == null ? "" : sessionId)
                .putData("title", title == null ? "" : title)   // 필요 시 SW/onMessage에서 사용
                .putData("body",  body  == null ? "" : body)
                .build();

        log.info("[FCM] send start to={}..., title={}, sessionId={}",
                token.substring(0, Math.min(8, token.length())), title, sessionId);

        String id = FirebaseMessaging.getInstance().send(msg);
        log.info("[FCM] send done id={}", id);
        return id;
    }

    // 익명 통화 요청
    public String sendCallRequest(String token, String ownerName, String sessionId)
            throws FirebaseMessagingException {
        String title = "SafeTag · 익명 통화 요청";
        String body  = "상대방이 " + (ownerName == null ? "차주" : ownerName) + "님과 통화를 요청했습니다.";
        return sendToToken(token, title, body, sessionId);
    }
}
