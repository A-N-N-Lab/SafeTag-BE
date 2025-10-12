package com.example.SafeTag_BE.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RelayTicketService {
    public static class Ticket {
        private final String token;
        private final long expiresAt; // epoch sec
        public Ticket(String token, long expiresAt) { this.token = token; this.expiresAt = expiresAt; }
        public String token() { return token; }
        public int ttlSec() {
            long now = Instant.now().getEpochSecond();
            return (int)Math.max(0, expiresAt - now);
        }
    }

    private final Map<String, Long> store = new ConcurrentHashMap<>();
    private static final int DEFAULT_TTL_SEC = 60;

    public Ticket issue(Long qrId, String kind) {
        String token = kind + "." + qrId + "." + UUID.randomUUID();
        long exp = Instant.now().getEpochSecond() + DEFAULT_TTL_SEC;
        store.put(token, exp);
        return new Ticket(token, exp);
    }

    public Long verify(String token, String expectedKind) {
        Long exp = store.get(token);
        if (exp == null) throw new IllegalArgumentException("Invalid ticket");
        if (Instant.now().getEpochSecond() > exp) {
            store.remove(token);
            throw new IllegalArgumentException("Expired ticket");
        }
        if (!token.startsWith(expectedKind + ".")) {
            throw new IllegalArgumentException("Kind mismatch");
        }
        String[] parts = token.split("\\.");
        return Long.parseLong(parts[1]); // qrId
    }
}
