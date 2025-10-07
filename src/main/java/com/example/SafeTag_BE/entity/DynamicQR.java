package com.example.SafeTag_BE.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.example.SafeTag_BE.entity.User;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicQR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique= true, length = 64)
    private String qrValue;

    private LocalDateTime generatedAt;
    private LocalDateTime expiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 동시 회전 방지
    @jakarta.persistence.Version
    private Long version;

    public boolean isExpired(LocalDateTime now){
        return expiredAt != null && expiredAt.isBefore(now);
    }

    public long secondsLeft(LocalDateTime now){
        return Math.max(0, java.time.Duration.between(now, expiredAt).toSeconds());
    }

}
