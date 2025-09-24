package com.example.SafeTag_BE.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "call_session",
        indexes = {
                @Index(name = "idx_session_uuid", columnList = "session_uuid", unique = true),
                @Index(name = "idx_expires_at",  columnList = "expires_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CallSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 방문자(회원 아니면 null)
    @Column(name = "caller_user_id")
    private Long callerUserId;

    // 차주(필수)
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    // WebRTC 세션 UUID (브라우저들이 공유)
    @Column(name = "session_uuid", nullable = false, length = 36)
    private String sessionUuid;

    // WebRTC / 050 / 콜백 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private CallMode mode;

    // 표시 번호(050 확장용으로 남김)
    @Column(name = "proxy_number", length = 20)
    private String proxyNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private CallState state;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    // QR 연동 정보
    @Column(name = "qr_uuid")
    private String qrUuid;

    // 세션 만료 시각(TTL)
    @Column(name = "expires_at")
    private Instant expiresAt;
}
