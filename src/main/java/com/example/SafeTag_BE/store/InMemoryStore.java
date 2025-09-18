package com.example.SafeTag_BE.store;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore {
    // verifyId -> status
    public static final Map<String, String> VERIFY = new ConcurrentHashMap<>();
    // stickerId -> expiresAt
    public static final Map<String, Instant> STICKERS = new ConcurrentHashMap<>();
    // otp -> expiresAt
    public static final Map<String, Instant> OTP = new ConcurrentHashMap<>();

    public static String newId() { return UUID.randomUUID().toString(); }
    public static Instant plusDays(int d) { return Instant.now().plus(d, ChronoUnit.DAYS); }
    public static Instant plusMinutes(int m) { return Instant.now().plus(m, ChronoUnit.MINUTES); }
}
