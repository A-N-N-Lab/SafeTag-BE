package com.example.SafeTag_BE.entity;

import com.example.SafeTag_BE.domain.VerificationStatus;
import com.example.SafeTag_BE.domain.VerificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "verification",
        indexes = {
                @Index(name = "idx_ver_user", columnList = "user_id"),
                @Index(name = "idx_ver_status", columnList = "status"),
                @Index(name = "idx_ver_type", columnList = "type"),
                @Index(name = "idx_ver_reviewer", columnList = "reviewer_admin_id"),
                @Index(name = "idx_ver_created", columnList = "created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Verification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    // 심사자(관리자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_admin_id")
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus status;

    @Column(nullable = false, length = 200)
    private String fileId;

    @Column(length = 500)
    private String reason;  //거절사유

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    private java.time.LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
