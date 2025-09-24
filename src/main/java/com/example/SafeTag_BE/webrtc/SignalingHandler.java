package com.example.SafeTag_BE.webrtc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper om = new ObjectMapper();

    // room(sessionId) -> 참가자 세션들
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // 세션 -> 속한 room
    private final Map<WebSocketSession, String> sessionRoom = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WS connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = om.readTree(message.getPayload());
            String type = getText(root, "type");
            String room = getText(root, "sessionId"); // 프론트에서 sessionId로 보냄
            if (type == null || room == null || room.isBlank()) return;

            switch (type.toLowerCase()) {
                case "join" -> {
                    rooms.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(session);
                    sessionRoom.put(session, room);
                    // 방에 들어온 본인에게 ack
                    send(session, Map.of("type", "ack", "sessionId", room));
                    // 기존 참가자들에게 새로운 참가자 알림
                    broadcastExcept(room, session, Map.of("type", "joined", "sessionId", room));
                    log.info("JOIN room={} sid={}", room, session.getId());
                }
                case "offer", "answer" -> {
                    // SDP 그대로 릴레이
                    JsonNode sdp = root.get("sdp"); // 전체 SDP 객체
                    broadcastExcept(room, session, Map.of("type", type.toLowerCase(), "sdp", sdp, "sessionId", room));
                }
                case "ice" -> {
                    // ICE candidate 그대로 릴레이
                    JsonNode cand = root.get("candidate");
                    broadcastExcept(room, session, Map.of("type", "ice", "candidate", cand, "sessionId", room));
                }
                default -> log.debug("Unknown type: {}", type);
            }
        } catch (Exception e) {
            log.warn("WS handle error: {}", e.toString());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String room = sessionRoom.remove(session);
        if (room != null) {
            Set<WebSocketSession> set = rooms.getOrDefault(room, Collections.emptySet());
            set.remove(session);
            broadcast(room, Map.of("type", "left", "sessionId", room));
            if (set.isEmpty()) rooms.remove(room);
        }
        log.info("WS closed: {} ({})", session.getId(), status);
    }

    private void send(WebSocketSession s, Object payload) {
        try {
            s.sendMessage(new TextMessage(om.writeValueAsString(payload)));
        } catch (Exception ignored) {}
    }

    private void broadcast(String room, Object payload) {
        Set<WebSocketSession> set = rooms.get(room);
        if (set == null) return;
        String text;
        try { text = om.writeValueAsString(payload); } catch (Exception e) { return; }
        for (WebSocketSession s : set) {
            if (s.isOpen()) {
                try { s.sendMessage(new TextMessage(text)); } catch (Exception ignored) {}
            }
        }
    }

    private void broadcastExcept(String room, WebSocketSession except, Object payload) {
        Set<WebSocketSession> set = rooms.get(room);
        if (set == null) return;
        String text;
        try { text = om.writeValueAsString(payload); } catch (Exception e) { return; }
        for (WebSocketSession s : set) {
            if (s == except) continue;
            if (s.isOpen()) {
                try { s.sendMessage(new TextMessage(text)); } catch (Exception ignored) {}
            }
        }
    }

    private String getText(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }
}
