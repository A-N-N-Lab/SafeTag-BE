package com.example.SafeTag_BE.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fcm_token", indexes = {
        @Index(name = "idx_fcm_token_user", columnList = "user_id"),
        @Index(name = "idx_fcm_token_token", columnList = "token", unique = true)
})
public class FcmToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 토큰 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private com.example.SafeTag_BE.entity.User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

//    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // 생성/수정 시각
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
