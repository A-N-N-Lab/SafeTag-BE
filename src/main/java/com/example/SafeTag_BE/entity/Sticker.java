package com.example.SafeTag_BE.entity;

import com.example.SafeTag_BE.domain.VerificationType;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Sticker {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private VerificationType type;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "car_number", nullable = false)
    private String carNumber;

    @Column(name = "image_url")
    private String imageUrl; // 템플릿 URL

    private LocalDate issuedAt;   // 발급일
    private LocalDate expiresAt;  // 유효기간
    private String issuer;        // 발급기관

}


