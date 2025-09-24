package com.example.SafeTag_BE.config;

import com.example.SafeTag_BE.webrtc.SignalingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final SignalingHandler signalingHandler;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/signaling")
                // 패턴 기반 허용: 로컬/LAN/터널 전부 포함
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "https://localhost:*",
                        "http://127.0.0.1:*",
                        "https://127.0.0.1:*",
                        "http://192.168.*:*",
                        "http://10.*:*",
                        "http://172.*:*",
                        "https://*.ngrok.io",
                        "https://*.trycloudflare.com",
                        "http://localhost:5173"
                );
        // SockJS를 쓰지 않는 "순수 WebSocket" 설계면 withSockJS()는 붙이면 안 됩니다.
        // .withSockJS();  // ← STOMP/SockJS 쓸 때만
    }

}
