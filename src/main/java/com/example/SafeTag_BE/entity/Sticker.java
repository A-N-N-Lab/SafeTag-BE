package com.example.SafeTag_BE.entity;

import com.example.SafeTag_BE.enums.StickerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class Sticker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자(차주)의 스티커인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 스티커 종류 (거주민 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StickerType type;

    // 스티커 번호 (발급번호)
    @Column(nullable = false, unique = true)
    private String stickerNo;

    // 거주지 (동/호)
    @Column(nullable = true)
    private String buildingUnit; // 예: "101동 202호"

    // 발급일
    @Column(nullable = false)
    private LocalDate issuedAt;

    // 유효기간
    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    // 상태 (예: ACTIVE, EXPIRED)
    @Column(nullable = false)
    private String status = "ACTIVE";
}
